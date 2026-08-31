package dev.visherryz.viclaimfly.region;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class OwnerPermissionLookup implements AutoCloseable {
    public enum Result { ALLOWED, DENIED, PENDING }

    private final Plugin owner;
    private final Map<UUID, CompletableFuture<?>> inFlight = new ConcurrentHashMap<>();

    public OwnerPermissionLookup(Plugin owner) {
        this.owner = owner;
    }

    public Result check(UUID ownerId, String permission) {
        Player online = Bukkit.getPlayer(ownerId);
        if (online != null) return online.hasPermission(permission) ? Result.ALLOWED : Result.DENIED;
        try {
            Object luckPerms = luckPerms();
            if (luckPerms == null) return Result.DENIED;
            Object userManager = ReflectionAccess.call(luckPerms, "getUserManager");
            Object loaded = ReflectionAccess.call(userManager, "getUser", ownerId);
            if (loaded != null) return cachedPermission(luckPerms, loaded, permission) ? Result.ALLOWED : Result.DENIED;
            return startLoad(userManager, ownerId) ? Result.PENDING : Result.DENIED;
        } catch (Throwable failure) {
            if (owner.getLogger().isLoggable(java.util.logging.Level.FINE)) owner.getLogger().log(java.util.logging.Level.FINE, "LuckPerms lookup failed", failure);
            return Result.DENIED;
        }
    }

    private boolean startLoad(Object userManager, UUID ownerId) {
        if (inFlight.containsKey(ownerId)) return true;
        try {
            Object result = ReflectionAccess.call(userManager, "loadUser", ownerId);
            if (!(result instanceof CompletionStage<?> stage)) return false;
            CompletableFuture<?> future = stage.toCompletableFuture();
            CompletableFuture<?> existing = inFlight.putIfAbsent(ownerId, future);
            if (existing == null) future.whenComplete((ignored, failure) -> inFlight.remove(ownerId, future));
            return true;
        } catch (Throwable failure) {
            return false;
        }
    }

    private Object luckPerms() throws ReflectiveOperationException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (plugin == null || !plugin.isEnabled()) return null;
        Class<?> api = ReflectionAccess.type(plugin, "net.luckperms.api.LuckPerms");
        Object registration = Bukkit.getServicesManager().getRegistration(api);
        return registration == null ? null : ReflectionAccess.call(registration, "getProvider");
    }

    private boolean cachedPermission(Object luckPerms, Object user, String permission) throws ReflectiveOperationException {
        Object contextManager = ReflectionAccess.call(luckPerms, "getContextManager");
        Object options = ReflectionAccess.call(contextManager, "getStaticQueryOptions");
        Object cachedData = ReflectionAccess.call(user, "getCachedData");
        Object permissionData = ReflectionAccess.call(cachedData, "getPermissionData", options);
        Object result = ReflectionAccess.call(permissionData, "checkPermission", permission);
        Object bool = ReflectionAccess.call(result, "asBoolean");
        return Boolean.TRUE.equals(bool);
    }

    @Override
    public void close() {
        inFlight.values().forEach(future -> future.cancel(false));
        inFlight.clear();
    }
}
