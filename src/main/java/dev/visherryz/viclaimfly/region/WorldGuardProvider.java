package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

final class WorldGuardProvider extends AbstractReflectiveProvider {
    private final Class<?> worldGuardType;
    private final Class<?> adapterType;

    WorldGuardProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                       ConfigurationService configuration) throws ReflectiveOperationException {
        super(owner, dependency, permissions, configuration);
        this.worldGuardType = ReflectionAccess.type(dependency, "com.sk89q.worldguard.WorldGuard");
        this.adapterType = ReflectionAccess.type(dependency, "com.sk89q.worldedit.bukkit.BukkitAdapter");
    }

    @Override public String id() { return "worldguard"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        if (player.getWorld() == null) return ClaimDecision.NOT_APPLICABLE;
        Object worldGuard = ReflectionAccess.callStatic(worldGuardType, "getInstance");
        Object platform = ReflectionAccess.call(worldGuard, "getPlatform");
        Object container = ReflectionAccess.call(platform, "getRegionContainer");
        Object world = ReflectionAccess.callStatic(adapterType, "adapt", player.getWorld());
        Object manager = ReflectionAccess.call(container, "get", world);
        if (manager == null) return ClaimDecision.NOT_APPLICABLE;
        Object vector = ReflectionAccess.callStatic(adapterType, "asBlockVector", player.getLocation());
        Object applicable = ReflectionAccess.call(manager, "getApplicableRegions", vector);
        Object regions = ReflectionAccess.call(applicable, "getRegions");
        if (!(regions instanceof Collection<?> collection) || collection.isEmpty()) return ClaimDecision.NOT_APPLICABLE;
        boolean sawRegion = false;
        for (Object region : collection) {
            String id = String.valueOf(ReflectionAccess.call(region, "getId"));
            if (id.isBlank() || id.equalsIgnoreCase("__global__")) continue;
            sawRegion = true;
            Object owners = ReflectionAccess.call(region, "getOwners");
            if (ReflectionAccess.bool(owners, "contains", player.getUniqueId()) && permissions.owner(player)) return ClaimDecision.ALLOW;
            Object members = ReflectionAccess.call(region, "getMembers");
            if (ReflectionAccess.bool(members, "contains", player.getUniqueId()) && permissions.trusted(player)) return ClaimDecision.ALLOW;
        }
        return sawRegion ? ClaimDecision.DENY : ClaimDecision.NOT_APPLICABLE;
    }
}
