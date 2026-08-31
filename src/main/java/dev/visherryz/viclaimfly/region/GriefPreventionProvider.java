package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

final class GriefPreventionProvider extends AbstractReflectiveProvider {
    GriefPreventionProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                            ConfigurationService configuration) {
        super(owner, dependency, permissions, configuration);
    }

    @Override public String id() { return "griefprevention"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Class<?> type = ReflectionAccess.type(dependency, "me.ryanhamshire.GriefPrevention.GriefPrevention");
        Object instance = ReflectionAccess.staticField(type, "instance");
        Object dataStore = ReflectionAccess.field(instance, "dataStore");
        Object playerData = ReflectionAccess.call(dataStore, "getPlayerData", player.getUniqueId());
        Object lastClaim = ReflectionAccess.field(playerData, "lastClaim");
        Object claim = ReflectionAccess.call(dataStore, "getClaimAt", player.getLocation(), true, lastClaim);
        if (claim == null) return ClaimDecision.NOT_APPLICABLE;
        Object ownerId = ReflectionAccess.call(claim, "getOwnerID");
        if (!(ownerId instanceof java.util.UUID uuid)) return ClaimDecision.DENY;
        try {
            if (ReflectionAccess.field(claim, "siegeData") != null) return ClaimDecision.DENY;
        } catch (NoSuchFieldException ignored) {
            // Newer GriefPrevention versions removed legacy siege state.
        }
        if (Objects.equals(uuid, player.getUniqueId())) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        if (!permissions.trusted(player)) return ClaimDecision.DENY;
        return ReflectionAccess.call(claim, "getPermission", player.getUniqueId().toString()) != null
                ? ClaimDecision.ALLOW : ClaimDecision.DENY;
    }
}
