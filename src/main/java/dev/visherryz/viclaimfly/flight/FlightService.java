package dev.visherryz.viclaimfly.flight;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.config.FlightDisableReason;
import dev.visherryz.viclaimfly.logging.DebugLogger;
import dev.visherryz.viclaimfly.message.MessageService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.UUID;

public final class FlightService {
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final FlightSessionRegistry sessions;
    private final DebugLogger debug;

    public FlightService(ConfigurationService configuration, MessageService messages,
                         FlightSessionRegistry sessions, DebugLogger debug) {
        this.configuration = configuration;
        this.messages = messages;
        this.sessions = sessions;
        this.debug = debug;
    }

    public boolean isManaged(UUID playerId) {
        return sessions.contains(playerId);
    }

    public Optional<FlightSession> session(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean restricted(Player player) {
        return !configuration.config().flight().allowedGameModes().contains(player.getGameMode());
    }

    public void enable(Player player, ActivationSource source, boolean notify) {
        if (!player.isOnline() || restricted(player)) return;
        UUID playerId = player.getUniqueId();
        FlightSession current = sessions.get(playerId).orElse(null);
        if (current == null) {
            sessions.put(new FlightSession(playerId, source, player.getAllowFlight(), player.isFlying(), player.getFlySpeed()));
        } else if (current.source() != source) {
            sessions.put(new FlightSession(playerId, source, current.previousAllowFlight(), current.previousFlying(), current.previousFlySpeed()));
        }
        player.setFlySpeed(configuration.config().flight().flySpeed());
        player.setAllowFlight(true);
        player.setFlying(true);
        debug.log("enabled player=" + player.getName() + " uuid=" + playerId + " source=" + source);
        if (notify && notificationsEnabled(source)) {
            messages.send(player, source == ActivationSource.AUTO ? "auto-flight-on" : "flight-on");
        }
    }

    public boolean disable(Player player, boolean notify, FlightDisableReason reason) {
        FlightSession session = sessions.remove(player.getUniqueId()).orElse(null);
        if (session == null) return false;
        float managedSpeed = configuration.config().flight().flySpeed();
        boolean stateStillManaged = player.getAllowFlight() && Math.abs(player.getFlySpeed() - managedSpeed) < 0.0001f;
        boolean restore = configuration.config().flight().restorePreviousFlightState();
        if (stateStillManaged && isIntrinsicFlightMode(player.getGameMode())) {
            player.setFlySpeed(session.previousFlySpeed());
        } else if (stateStillManaged && restore) {
            player.setFlySpeed(session.previousFlySpeed());
            player.setAllowFlight(session.previousAllowFlight());
            player.setFlying(session.previousAllowFlight() && session.previousFlying());
        } else if (stateStillManaged) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFlySpeed(session.previousFlySpeed());
        }
        if (configuration.config().slowFalling().appliesTo(reason)) giveSlowFalling(player);
        debug.log("disabled player=" + player.getName() + " uuid=" + player.getUniqueId()
                + " source=" + session.source() + " stateStillManaged=" + stateStillManaged + " restore=" + restore);
        if (notify && notificationsEnabled(session.source(), reason)) {
            messages.send(player, session.source() == ActivationSource.AUTO ? "auto-flight-off" : "flight-off");
        }
        return true;
    }

    public void forget(UUID playerId) {
        sessions.remove(playerId);
    }

    public void close() {
        sessions.clear();
    }

    private void giveSlowFalling(Player player) {
        var settings = configuration.config().slowFalling();
        if (!settings.enabled() || settings.durationSeconds() <= 0) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                settings.durationSeconds() * 20, settings.amplifier(),
                settings.ambient(), settings.particles(), settings.icon()));
    }

    private boolean isIntrinsicFlightMode(GameMode gameMode) {
        return gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
    }

    private boolean notificationsEnabled(ActivationSource source) {
        return switch (source) {
            case AUTO -> configuration.config().notifications().auto();
            case ADMIN -> configuration.config().notifications().admin();
            case MANUAL -> configuration.config().notifications().manual();
        };
    }

    private boolean notificationsEnabled(ActivationSource source, FlightDisableReason reason) {
        return reason == FlightDisableReason.ADMIN
                ? configuration.config().notifications().admin()
                : notificationsEnabled(source);
    }
}
