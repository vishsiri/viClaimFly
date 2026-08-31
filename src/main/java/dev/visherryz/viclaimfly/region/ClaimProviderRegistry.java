package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class ClaimProviderRegistry {
    private final Plugin plugin;
    private final ConfigurationService configuration;
    private final PermissionPolicy permissions;
    private final OwnerPermissionLookup ownerPermissions;

    public ClaimProviderRegistry(Plugin plugin, ConfigurationService configuration,
                                 PermissionPolicy permissions, OwnerPermissionLookup ownerPermissions) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.permissions = permissions;
        this.ownerPermissions = ownerPermissions;
    }

    public List<ClaimProvider> discover() {
        List<ClaimProvider> providers = new ArrayList<>();
        add(providers, "protectionstones", "ProtectionStones", dependency ->
                new ProtectionStonesProvider(plugin, dependency, permissions, configuration, ownerPermissions));
        add(providers, "lands", "Lands", dependency -> new LandsProvider(plugin, dependency, permissions, configuration));
        add(providers, "huskclaims", "HuskClaims", dependency -> new HuskClaimsProvider(plugin, dependency, permissions, configuration));
        add(providers, "griefprevention", "GriefPrevention", dependency -> new GriefPreventionProvider(plugin, dependency, permissions, configuration));
        add(providers, "redprotect", "RedProtect", dependency -> new RedProtectProvider(plugin, dependency, permissions, configuration));
        add(providers, "griefdefender", "GriefDefender", dependency -> new GriefDefenderProvider(plugin, dependency, permissions, configuration));
        add(providers, "plotsquared", "PlotSquared", dependency -> new PlotSquaredProvider(plugin, dependency, permissions, configuration));
        add(providers, "worldguard", "WorldGuard", dependency -> new WorldGuardProvider(plugin, dependency, permissions, configuration));
        return List.copyOf(providers);
    }

    private void add(List<ClaimProvider> providers, String id, String pluginName, Factory factory) {
        if (!configuration.config().regions().providerEnabled(id)) return;
        Plugin dependency = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (dependency == null || !dependency.isEnabled()) return;
        try {
            ClaimProvider provider = factory.create(dependency);
            providers.add(provider);
            plugin.getLogger().info("Enabled claim provider: " + provider.id());
        } catch (Throwable failure) {
            plugin.getLogger().log(Level.WARNING, "Could not enable claim provider '" + id + "'", failure);
        }
    }

    @FunctionalInterface
    private interface Factory {
        ClaimProvider create(Plugin dependency) throws Exception;
    }
}
