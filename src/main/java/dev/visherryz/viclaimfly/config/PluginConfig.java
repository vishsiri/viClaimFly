package dev.visherryz.viclaimfly.config;

import org.bukkit.GameMode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PluginConfig(
        boolean debug,
        Commands commands,
        Permissions permissions,
        Flight flight,
        Notifications notifications,
        Warmup warmup,
        Cooldown cooldown,
        SlowFalling slowFalling,
        Regions regions,
        Integrations integrations
) {
    public record Commands(Map<String, List<String>> routes) { }

    public record Permissions(
            String owner,
            List<String> trusted,
            String autoFlight,
            String warmup,
            String warmupBypass,
            String cooldownBypass,
            String joinBypass,
            String plotRoad,
            String redProtectOwnerBypass,
            String adminToggle,
            String reload
    ) { }

    public record Flight(
            boolean manualEnabled,
            boolean autoEnabled,
            boolean restorePreviousFlightState,
            float flySpeed,
            Set<GameMode> allowedGameModes,
            boolean disableOnWorldChange,
            boolean disableOnGameModeChange,
            boolean disableOnDeath,
            boolean cleanupOnJoin,
            boolean disableInPvpCombat,
            boolean cancelFallDamageWhileManaged
    ) { }

    public record Notifications(boolean manual, boolean auto, boolean admin) { }

    public record Warmup(int seconds, WarmupMode mode, boolean cancelOnBlockMove,
                         boolean cancelOnDamage, boolean cancelOnClaimExit) { }

    public record Cooldown(int seconds) { }

    public record SlowFalling(boolean enabled, int durationSeconds, int amplifier,
                              boolean ambient, boolean particles, boolean icon,
                              Set<FlightDisableReason> applyOn) {
        public boolean appliesTo(FlightDisableReason reason) {
            return enabled && applyOn.contains(reason);
        }
    }

    public record Regions(Map<String, Boolean> providers, int providerWarningIntervalSeconds,
                          boolean protectionStonesDelegatedOwnerPermission) {
        public boolean providerEnabled(String id) {
            return providers.getOrDefault(id.toLowerCase(java.util.Locale.ROOT), false);
        }
    }

    public record Integrations(boolean placeholderApi, String placeholderIdentifier,
                               String placeholderStatusParameter, boolean pvpManager,
                               int pvpWarningIntervalSeconds) { }
}
