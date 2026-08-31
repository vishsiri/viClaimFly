package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class LandsProvider extends AbstractReflectiveProvider {
    private final Object api;

    LandsProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                  ConfigurationService configuration) throws ReflectiveOperationException {
        super(owner, dependency, permissions, configuration);
        Class<?> type = ReflectionAccess.type(dependency, "me.angeschossen.lands.api.LandsIntegration");
        this.api = ReflectionAccess.callStatic(type, "of", owner);
    }

    @Override public String id() { return "lands"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Object area = ReflectionAccess.call(api, "getArea", player.getLocation());
        if (area == null) return ClaimDecision.NOT_APPLICABLE;
        if (player.getUniqueId().equals(ReflectionAccess.call(area, "getOwnerUID"))) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        return permissions.trusted(player) && ReflectionAccess.bool(area, "isTrusted", player.getUniqueId())
                ? ClaimDecision.ALLOW : ClaimDecision.DENY;
    }
}
