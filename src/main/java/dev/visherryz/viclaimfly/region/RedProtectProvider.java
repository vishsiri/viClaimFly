package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class RedProtectProvider extends AbstractReflectiveProvider {
    RedProtectProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                       ConfigurationService configuration) {
        super(owner, dependency, permissions, configuration);
    }

    @Override public String id() { return "redprotect"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Class<?> type = ReflectionAccess.type(dependency, "br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect");
        Object api = ReflectionAccess.call(ReflectionAccess.callStatic(type, "get"), "getAPI");
        Location location = player.getLocation();
        Object region = ReflectionAccess.call(api, "getLowPriorityRegion", location.getWorld(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (region == null) return ClaimDecision.NOT_APPLICABLE;
        String uuid = player.getUniqueId().toString();
        boolean owner = ReflectionAccess.bool(region, "isLeaderByUUID", uuid)
                || ReflectionAccess.bool(region, "isAdminByUUID", uuid)
                || permissions.redProtectOwnerBypass(player);
        if (owner) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        return permissions.trusted(player) && ReflectionAccess.bool(region, "isMember", uuid)
                ? ClaimDecision.ALLOW : ClaimDecision.DENY;
    }
}
