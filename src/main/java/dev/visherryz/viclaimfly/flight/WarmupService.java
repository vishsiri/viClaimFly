package dev.visherryz.viclaimfly.flight;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.config.WarmupMode;
import dev.visherryz.viclaimfly.region.PermissionPolicy;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WarmupService {
    private final Plugin plugin;
    private final ConfigurationService configuration;
    private final PermissionPolicy permissions;
    private final Clock clock;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public WarmupService(Plugin plugin, ConfigurationService configuration, PermissionPolicy permissions) {
        this(plugin, configuration, permissions, Clock.systemUTC());
    }

    WarmupService(Plugin plugin, ConfigurationService configuration, PermissionPolicy permissions, Clock clock) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.permissions = permissions;
        this.clock = clock;
    }

    public boolean required(Player player) {
        var warmup = configuration.config().warmup();
        if (warmup.seconds() <= 0 || warmup.mode() == WarmupMode.DISABLED || permissions.warmupBypass(player)) return false;
        return warmup.mode() == WarmupMode.ALL || permissions.warmup(player);
    }

    public boolean start(Player player, Runnable completion) {
        cancel(player.getUniqueId());
        int seconds = configuration.config().warmup().seconds();
        UUID playerId = player.getUniqueId();
        UUID token = UUID.randomUUID();
        Instant end = clock.instant().plusSeconds(seconds);
        ScheduledTask task = player.getScheduler().runDelayed(plugin, ignored -> {
            Entry current = entries.get(playerId);
            if (current == null || !current.token().equals(token)) return;
            entries.remove(playerId, current);
            completion.run();
        }, () -> entries.computeIfPresent(playerId,
                (ignored, current) -> current.token().equals(token) ? null : current),
                Math.max(1L, seconds * 20L));
        if (task == null) return false;
        entries.put(playerId, new Entry(token, end, task));
        return true;
    }

    public boolean pending(UUID playerId) {
        return secondsLeft(playerId) > 0L;
    }

    public long secondsLeft(UUID playerId) {
        Entry entry = entries.get(playerId);
        if (entry == null) return 0L;
        long millis = entry.end().toEpochMilli() - clock.millis();
        return millis <= 0L ? 0L : Math.max(1L, (millis + 999L) / 1000L);
    }

    public boolean cancel(UUID playerId) {
        Entry entry = entries.remove(playerId);
        if (entry == null) return false;
        entry.task().cancel();
        return true;
    }

    public void close() {
        entries.values().forEach(entry -> entry.task().cancel());
        entries.clear();
    }

    private record Entry(UUID token, Instant end, ScheduledTask task) { }
}
