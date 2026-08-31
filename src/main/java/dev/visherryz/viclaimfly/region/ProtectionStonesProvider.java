package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.UUID;

final class ProtectionStonesProvider extends AbstractReflectiveProvider {
    private final ConfigurationService configuration;
    private final OwnerPermissionLookup ownerPermissions;
    private final Class<?> regionType;

    ProtectionStonesProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                             ConfigurationService configuration, OwnerPermissionLookup ownerPermissions)
            throws ReflectiveOperationException {
        super(owner, dependency, permissions, configuration);
        this.configuration = configuration;
        this.ownerPermissions = ownerPermissions;
        this.regionType = ReflectionAccess.type(dependency, "dev.espi.protectionstones.PSRegion");
    }

    @Override public String id() { return "protectionstones"; }

    @Override
    protected ClaimDecision evaluateSafely(Player player) throws Exception {
        Object region = ReflectionAccess.callStatic(regionType, "fromLocationGroup", player.getLocation());
        if (region == null) return ClaimDecision.NOT_APPLICABLE;
        UUID playerId = player.getUniqueId();
        if (ReflectionAccess.bool(region, "isOwner", playerId)) return permissions.owner(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        if (!ReflectionAccess.bool(region, "isMember", playerId)) return ClaimDecision.DENY;
        if (!configuration.config().regions().protectionStonesDelegatedOwnerPermission()) {
            return permissions.trusted(player) ? ClaimDecision.ALLOW : ClaimDecision.DENY;
        }
        Object owners = ReflectionAccess.call(region, "getOwners");
        if (!(owners instanceof Collection<?> collection)) return ClaimDecision.DENY;
        boolean pending = false;
        for (Object ownerId : collection) {
            if (!(ownerId instanceof UUID uuid)) continue;
            OwnerPermissionLookup.Result result = ownerPermissions.check(uuid, permissions.ownerPermission());
            if (result == OwnerPermissionLookup.Result.ALLOWED) return ClaimDecision.ALLOW;
            pending |= result == OwnerPermissionLookup.Result.PENDING;
        }
        return pending ? ClaimDecision.PENDING : ClaimDecision.DENY;
    }
}
