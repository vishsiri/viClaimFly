package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

final class GriefDefenderProvider extends AbstractReflectiveProvider {
    GriefDefenderProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                          ConfigurationService configuration) {
        super(owner, dependency, permissions, configuration);
    }

    @Override public String id() { return "griefdefender"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Class<?> type = ReflectionAccess.type(dependency, "com.griefdefender.api.GriefDefender");
        Object core = ReflectionAccess.callStatic(type, "getCore");
        Object claim = ReflectionAccess.call(core, "getClaimAt", player.getLocation());
        if (claim == null) return ClaimDecision.NOT_APPLICABLE;
        Object ownerId = ReflectionAccess.call(claim, "getOwnerUniqueId");
        if (player.getUniqueId().equals(ownerId)) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        if (!permissions.trusted(player)) return ClaimDecision.DENY;
        Object trusts = ReflectionAccess.call(claim, "getUserTrusts");
        return trusts instanceof Collection<?> collection && collection.contains(player.getUniqueId())
                ? ClaimDecision.ALLOW : ClaimDecision.DENY;
    }
}
