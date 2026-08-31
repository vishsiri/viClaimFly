package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;

final class HuskClaimsProvider extends AbstractReflectiveProvider {
    HuskClaimsProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                       ConfigurationService configuration) {
        super(owner, dependency, permissions, configuration);
    }

    @Override public String id() { return "huskclaims"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Class<?> type = ReflectionAccess.type(dependency, "net.william278.huskclaims.api.HuskClaimsAPI");
        Object api = ReflectionAccess.callStatic(type, "getInstance");
        Object user = ReflectionAccess.call(api, "getOnlineUser", player.getUniqueId());
        if (user == null) return ClaimDecision.DENY;
        Location location = player.getLocation();
        Object world = ReflectionAccess.call(user, "getWorld");
        Object position = ReflectionAccess.call(api, "getPosition", location.getX(), location.getY(), location.getZ(), world);
        Object claimOptional = ReflectionAccess.call(api, "getClaimAt", position);
        if (!(claimOptional instanceof Optional<?> claimResult) || claimResult.isEmpty()) return ClaimDecision.NOT_APPLICABLE;
        Object ownerOptional = ReflectionAccess.call(api, "getClaimOwnerAt", position);
        if (!(ownerOptional instanceof Optional<?> ownerResult) || ownerResult.isEmpty()) return ClaimDecision.DENY;
        Object userId = ReflectionAccess.call(user, "getUuid");
        if (userId.equals(ReflectionAccess.call(ownerResult.get(), "getUuid"))) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        if (!permissions.trusted(player)) return ClaimDecision.DENY;
        Object trusted = ReflectionAccess.call(claimResult.get(), "getTrustedUsers");
        return trusted instanceof Map<?, ?> map && map.containsKey(userId) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
    }
}
