package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class PlotSquaredProvider extends AbstractReflectiveProvider {
    PlotSquaredProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                        ConfigurationService configuration) {
        super(owner, dependency, permissions, configuration);
    }

    @Override public String id() { return "plotsquared"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Class<?> type = ReflectionAccess.type(dependency, "com.plotsquared.core.player.PlotPlayer");
        Object plotPlayer = ReflectionAccess.callStatic(type, "from", player);
        Object location = ReflectionAccess.call(plotPlayer, "getLocation");
        if (ReflectionAccess.bool(location, "isPlotRoad")) {
            return permissions.owner(player) && permissions.plotRoad(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        }
        Object plot = ReflectionAccess.call(plotPlayer, "getCurrentPlot");
        if (plot == null) return ClaimDecision.NOT_APPLICABLE;
        if (ReflectionAccess.bool(plot, "isOwner", player.getUniqueId())) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        return permissions.trusted(player) && ReflectionAccess.bool(plot, "isAdded", player.getUniqueId())
                ? ClaimDecision.ALLOW : ClaimDecision.DENY;
    }
}
