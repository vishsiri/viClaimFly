package dev.visherryz.viclaimfly.flight;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.config.FlightDisableReason;
import dev.visherryz.viclaimfly.integration.PvpManagerHook;
import dev.visherryz.viclaimfly.logging.DebugLogger;
import dev.visherryz.viclaimfly.message.MessageService;
import dev.visherryz.viclaimfly.region.ClaimAccessService;
import dev.visherryz.viclaimfly.region.PermissionPolicy;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;

public final class ClaimFlightController {
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final ClaimAccessService claims;
    private final FlightService flights;
    private final CooldownService cooldowns;
    private final WarmupService warmups;
    private final PvpManagerHook pvpManager;
    private final DebugLogger debug;
    private final PermissionPolicy permissions;

    public ClaimFlightController(ConfigurationService configuration, MessageService messages,
                                 ClaimAccessService claims, FlightService flights,
                                 CooldownService cooldowns, WarmupService warmups,
                                 PvpManagerHook pvpManager, DebugLogger debug,
                                 PermissionPolicy permissions) {
        this.configuration = configuration;
        this.messages = messages;
        this.claims = claims;
        this.flights = flights;
        this.cooldowns = cooldowns;
        this.warmups = warmups;
        this.pvpManager = pvpManager;
        this.debug = debug;
        this.permissions = permissions;
    }

    public void toggle(Player player) {
        if (flights.isManaged(player.getUniqueId())) {
            warmups.cancel(player.getUniqueId());
            flights.disable(player, true, FlightDisableReason.MANUAL);
            return;
        }
        if (!configuration.config().flight().manualEnabled()) {
            messages.send(player, "no-permission");
            return;
        }
        if (flights.restricted(player)) {
            messages.send(player, "restricted-game-mode");
            return;
        }
        if (!claims.canFly(player)) {
            messages.send(player, permissions.owner(player)
                    ? "outside-claim" : "no-permission");
            return;
        }
        if (warmups.pending(player.getUniqueId())) {
            messages.send(player, "warmup-pending", MessageService.text("seconds", warmups.secondsLeft(player.getUniqueId())));
            return;
        }
        long cooldown = permissions.cooldownBypass(player) ? 0L : cooldowns.secondsLeft(player.getUniqueId());
        if (cooldown > 0L) {
            messages.send(player, "cooldown", MessageService.text("seconds", cooldown));
            return;
        }
        if (warmups.required(player)) {
            int seconds = configuration.config().warmup().seconds();
            if (warmups.start(player, () -> completeWarmup(player))) {
                messages.send(player, "warmup-started", MessageService.text("seconds", seconds));
            } else {
                messages.send(player, "warmup-failed");
            }
            return;
        }
        enableManual(player);
    }

    public void adminToggle(Player target) {
        warmups.cancel(target.getUniqueId());
        if (!flights.disable(target, true, FlightDisableReason.ADMIN)) flights.enable(target, ActivationSource.ADMIN, true);
    }

    public void onBlockMove(Player player) {
        if (warmups.pending(player.getUniqueId()) && configuration.config().warmup().cancelOnBlockMove()) {
            cancelWarmup(player, true);
            return;
        }
        boolean eligible = !flights.restricted(player) && claims.canFly(player);
        if (!eligible) {
            if (configuration.config().warmup().cancelOnClaimExit()) cancelWarmup(player, true);
            flights.disable(player, true, FlightDisableReason.CLAIM_EXIT);
            return;
        }
        if (!configuration.config().flight().autoEnabled() || !permissions.autoFlight(player)) return;
        if (flights.isManaged(player.getUniqueId()) || player.isFlying() || player.getAllowFlight()) return;
        flights.enable(player, ActivationSource.AUTO, true);
    }

    public void onWorldChange(Player player) {
        cancelWarmup(player, false);
        if (configuration.config().flight().disableOnWorldChange()) {
            flights.disable(player, true, FlightDisableReason.WORLD_CHANGE);
        }
    }

    public void onGameModeChange(Player player) {
        if (configuration.config().flight().disableOnGameModeChange() && flights.restricted(player)) {
            cancelWarmup(player, false);
            flights.disable(player, false, FlightDisableReason.GAME_MODE);
        }
    }

    public void onDamage(Player player) {
        if (configuration.config().warmup().cancelOnDamage()) cancelWarmup(player, true);
        if (configuration.config().flight().disableInPvpCombat()
                && configuration.config().integrations().pvpManager()
                && flights.isManaged(player.getUniqueId()) && pvpManager.inCombat(player)) {
            if (flights.disable(player, false, FlightDisableReason.COMBAT)) messages.send(player, "combat-disabled");
        }
    }

    public boolean cancelFallDamage(Player player) {
        return configuration.config().flight().cancelFallDamageWhileManaged() && flights.isManaged(player.getUniqueId());
    }

    public void onQuit(Player player) {
        warmups.cancel(player.getUniqueId());
        cooldowns.remove(player.getUniqueId());
        flights.disable(player, false, FlightDisableReason.QUIT);
        flights.forget(player.getUniqueId());
    }

    public void onJoin(Player player) {
        if (!configuration.config().flight().cleanupOnJoin()) return;
        if (permissions.joinBypass(player)) {
            debug.log("join cleanup bypassed player=" + player.getName() + " uuid=" + player.getUniqueId());
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        warmups.cancel(player.getUniqueId());
        cooldowns.remove(player.getUniqueId());
        boolean disabled = flights.disable(player, false, FlightDisableReason.JOIN);
        debug.log("join cleanup player=" + player.getName() + " uuid=" + player.getUniqueId()
                + " managedSessionRemoved=" + disabled);
    }

    public void onDeath(Player player) {
        warmups.cancel(player.getUniqueId());
        if (configuration.config().flight().disableOnDeath()) {
            flights.disable(player, false, FlightDisableReason.DEATH);
        }
    }

    private void completeWarmup(Player player) {
        if (!player.isOnline() || flights.restricted(player) || !claims.canFly(player)) {
            messages.send(player, "warmup-failed");
            return;
        }
        enableManual(player);
    }

    private void enableManual(Player player) {
        flights.enable(player, ActivationSource.MANUAL, true);
        if (!permissions.cooldownBypass(player)) {
            cooldowns.start(player.getUniqueId(), configuration.config().cooldown().seconds());
        }
    }

    private void cancelWarmup(Player player, boolean notify) {
        if (warmups.cancel(player.getUniqueId()) && notify) messages.send(player, "warmup-cancelled");
    }
}
