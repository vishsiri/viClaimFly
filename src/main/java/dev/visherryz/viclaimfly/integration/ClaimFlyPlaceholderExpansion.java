package dev.visherryz.viclaimfly.integration;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.flight.FlightService;
import dev.visherryz.viclaimfly.flight.WarmupService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClaimFlyPlaceholderExpansion extends PlaceholderExpansion {
    private final Plugin plugin;
    private final ConfigurationService configuration;
    private final FlightService flights;
    private final WarmupService warmups;
    private final String identifier;
    private final String statusParameter;

    public ClaimFlyPlaceholderExpansion(Plugin plugin, ConfigurationService configuration,
                                        FlightService flights, WarmupService warmups) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.flights = flights;
        this.warmups = warmups;
        this.identifier = configuration.config().integrations().placeholderIdentifier();
        this.statusParameter = configuration.config().integrations().placeholderStatusParameter();
    }

    @Override public @NotNull String getIdentifier() { return identifier; }
    @Override public @NotNull String getAuthor() { return plugin.getPluginMeta().getAuthors().getFirst(); }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String parameters) {
        if (!parameters.equalsIgnoreCase(statusParameter) || player == null) return null;
        if (warmups.pending(player.getUniqueId())) return configuration.messages().placeholderWarmup();
        return flights.isManaged(player.getUniqueId())
                ? configuration.messages().placeholderOn()
                : configuration.messages().placeholderOff();
    }
}
