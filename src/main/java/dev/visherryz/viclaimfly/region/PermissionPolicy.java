package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;

public final class PermissionPolicy {
    private final ConfigurationService configuration;

    public PermissionPolicy(ConfigurationService configuration) {
        this.configuration = configuration;
    }

    public boolean owner(Player player) {
        return player.hasPermission(configuration.config().permissions().owner());
    }

    public boolean trusted(Player player) {
        return owner(player) && configuration.config().permissions().trusted().stream().anyMatch(player::hasPermission);
    }

    public String ownerPermission() {
        return configuration.config().permissions().owner();
    }

    public boolean autoFlight(Player player) { return has(player, configuration.config().permissions().autoFlight()); }
    public boolean warmup(Player player) { return has(player, configuration.config().permissions().warmup()); }
    public boolean warmupBypass(Player player) { return has(player, configuration.config().permissions().warmupBypass()); }
    public boolean cooldownBypass(Player player) { return has(player, configuration.config().permissions().cooldownBypass()); }
    public boolean joinBypass(Player player) { return has(player, configuration.config().permissions().joinBypass()); }
    public boolean plotRoad(Player player) { return has(player, configuration.config().permissions().plotRoad()); }
    public boolean redProtectOwnerBypass(Player player) { return has(player, configuration.config().permissions().redProtectOwnerBypass()); }
    public boolean adminToggle(Player player) { return has(player, configuration.config().permissions().adminToggle()); }

    public boolean adminToggle(org.bukkit.command.CommandSender sender) {
        return sender.hasPermission(configuration.config().permissions().adminToggle());
    }

    public boolean reload(org.bukkit.command.CommandSender sender) {
        return sender.hasPermission(configuration.config().permissions().reload());
    }

    private boolean has(Player player, String permission) {
        return player.hasPermission(permission);
    }
}
