package dev.visherryz.viclaimfly.integration;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.region.ReflectionBridge;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicLong;

public final class PvpManagerHook {
    private final Plugin owner;
    private final ConfigurationService configuration;
    private volatile Plugin dependency;
    private final AtomicLong lastWarning = new AtomicLong();

    public PvpManagerHook(Plugin owner, ConfigurationService configuration) {
        this.owner = owner;
        this.configuration = configuration;
        refresh();
    }

    public void refresh() {
        Plugin candidate = owner.getServer().getPluginManager().getPlugin("PvPManager");
        dependency = candidate != null && candidate.isEnabled() ? candidate : null;
    }

    public boolean active() {
        return dependency != null;
    }

    public boolean inCombat(Player player) {
        Plugin plugin = dependency;
        if (plugin == null) return false;
        try {
            Class<?> type = ReflectionBridge.type(plugin, "me.NoChance.PvPManager.PvPManager");
            Object instance = ReflectionBridge.callStatic(type, "getInstance");
            Object handler = ReflectionBridge.call(instance, "getPlayerHandler");
            Object data = ReflectionBridge.call(handler, "get", player);
            return data != null && ReflectionBridge.bool(data, "isInCombat");
        } catch (Throwable failure) {
            long now = System.currentTimeMillis();
            long previous = lastWarning.get();
            long intervalMillis = configuration.config().integrations().pvpWarningIntervalSeconds() * 1_000L;
            if ((intervalMillis == 0L || now - previous >= intervalMillis)
                    && lastWarning.compareAndSet(previous, now)) {
                owner.getLogger().warning("PvPManager integration failed safely: " + failure.getMessage());
            }
            return false;
        }
    }
}
