package dev.visherryz.viclaimfly.config;

import org.bukkit.GameMode;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ConfigurationService {
    private static final Pattern COMMAND_LABEL = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private static final Pattern PLACEHOLDER_TOKEN = Pattern.compile("[a-z0-9_]+", Pattern.CASE_INSENSITIVE);
    private static final Map<String, List<String>> DEFAULT_COMMAND_ROUTES = Map.of(
            "claimfly", List.of("claimfly", "regionfly", "chocofly", "pfly", "cfly", "rfly"),
            "claimflyreload", List.of("claimflyreload", "cfreload")
    );
    private static final List<String> MESSAGE_KEYS = List.of(
            "no-permission", "player-only", "outside-claim", "restricted-game-mode",
            "flight-on", "flight-off", "auto-flight-on", "auto-flight-off", "cooldown",
            "warmup-started", "warmup-pending", "warmup-cancelled", "warmup-failed",
            "combat-disabled", "admin-toggled", "player-not-found", "status-on", "status-off",
            "reload-success", "reload-failed"
    );

    private final JavaPlugin plugin;
    private volatile PluginConfig config;
    private volatile MessagesConfig messages;

    public ConfigurationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void load() throws ConfigurateException {
        apply(readSnapshot());
    }

    public Snapshot readSnapshot() throws ConfigurateException {
        installDefault("config.yml");
        installDefault("messages.yml");
        PluginConfig nextConfig = parseConfig(loader(path("config.yml")).load());
        MessagesConfig nextMessages = parseMessages(loader(path("messages.yml")).load());
        validate(nextConfig);
        return new Snapshot(nextConfig, nextMessages);
    }

    public PluginConfig config() {
        return config;
    }

    public MessagesConfig messages() {
        return messages;
    }

    public Snapshot snapshot() {
        return new Snapshot(config, messages);
    }

    public synchronized void apply(Snapshot snapshot) {
        config = snapshot.config();
        messages = snapshot.messages();
    }

    public void restore(Snapshot snapshot) {
        apply(snapshot);
    }

    public record Snapshot(PluginConfig config, MessagesConfig messages) { }

    private Path path(String name) {
        return plugin.getDataFolder().toPath().resolve(name);
    }

    private void installDefault(String name) {
        if (!Files.exists(path(name))) plugin.saveResource(name, false);
    }

    private YamlConfigurationLoader loader(Path path) {
        return YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
    }

    private PluginConfig parseConfig(ConfigurationNode root) throws ConfigurateException {
        int configVersion = root.node("config-version").getInt(1);
        if (configVersion < 1 || configVersion > 2) {
            throw new ConfigurateException("Unsupported config-version: " + configVersion);
        }
        ConfigurationNode flight = root.node("flight");
        Set<GameMode> gameModes = new LinkedHashSet<>();
        for (String value : strings(flight.node("allowed-game-modes"), List.of("SURVIVAL", "ADVENTURE"))) {
            try {
                gameModes.add(GameMode.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new ConfigurateException("Unknown flight.allowed-game-modes value: " + value, exception);
            }
        }

        ConfigurationNode warmup = root.node("warmup");
        WarmupMode warmupMode;
        try {
            warmupMode = WarmupMode.valueOf(warmup.node("mode").getString("PERMISSION").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigurateException("warmup.mode must be DISABLED, ALL, or PERMISSION", exception);
        }

        ConfigurationNode regions = root.node("regions");
        Map<String, Boolean> providers = new LinkedHashMap<>();
        ConfigurationNode providerNode = regions.node("providers");
        for (String id : List.of("protectionstones", "lands", "huskclaims", "griefprevention",
                "redprotect", "griefdefender", "plotsquared", "worldguard")) {
            providers.put(id, providerNode.node(id).getBoolean(true));
        }

        ConfigurationNode integrations = root.node("integrations");
        ConfigurationNode permissions = root.node("permissions");
        Map<String, List<String>> commandRoutes = commandRoutes(root.node("commands", "routes"));
        Set<FlightDisableReason> slowFallingTriggers = enumSet(
                root.node("slow-falling", "apply-on"),
                FlightDisableReason.class,
                Set.of(FlightDisableReason.CLAIM_EXIT, FlightDisableReason.COMBAT),
                "slow-falling.apply-on"
        );
        ConfigurationNode placeholderApi = integrations.node("placeholderapi");
        ConfigurationNode pvpManager = integrations.node("pvpmanager");
        return new PluginConfig(
                root.node("debug").getBoolean(false),
                new PluginConfig.Commands(commandRoutes),
                new PluginConfig.Permissions(
                        permissions.node("owner").getString(regions.node("owner-permission").getString("claim.fly")),
                        List.copyOf(strings(permissions.node("trusted"),
                                strings(regions.node("trusted-permissions"), List.of("claim.fly.trusted")))),
                        permissions.node("auto-flight").getString("claim.fly.autofly"),
                        permissions.node("warmup").getString("claim.fly.warmup"),
                        permissions.node("warmup-bypass").getString("claim.fly.warmup.bypass"),
                        permissions.node("cooldown-bypass").getString("claim.fly.cooldown.bypass"),
                        permissions.node("join-bypass").getString("claim.fly.join.bypass"),
                        permissions.node("plot-road").getString("claim.fly.plotroad"),
                        permissions.node("redprotect-owner-bypass").getString("redprotect.flag.bypass.build"),
                        permissions.node("admin-toggle").getString("claim.fly.admin"),
                        permissions.node("reload").getString("claim.reload")
                ),
                new PluginConfig.Flight(
                        flight.node("manual-enabled").getBoolean(true),
                        flight.node("auto-enabled").getBoolean(true),
                        flight.node("restore-previous-flight-state").getBoolean(true),
                        flight.node("fly-speed").getFloat(0.1f),
                        Set.copyOf(gameModes),
                        flight.node("disable-on-world-change").getBoolean(true),
                        flight.node("disable-on-game-mode-change").getBoolean(true),
                        flight.node("disable-on-death").getBoolean(true),
                        flight.node("cleanup-on-join").getBoolean(true),
                        flight.node("disable-in-pvp-combat").getBoolean(true),
                        flight.node("cancel-fall-damage-while-managed").getBoolean(true)
                ),
                new PluginConfig.Notifications(
                        root.node("notifications", "manual").getBoolean(true),
                        root.node("notifications", "auto").getBoolean(true),
                        root.node("notifications", "admin").getBoolean(true)
                ),
                new PluginConfig.Warmup(
                        warmup.node("seconds").getInt(2),
                        warmupMode,
                        warmup.node("cancel-on-block-move").getBoolean(true),
                        warmup.node("cancel-on-damage").getBoolean(true),
                        warmup.node("cancel-on-claim-exit").getBoolean(true)
                ),
                new PluginConfig.Cooldown(root.node("cooldown", "seconds").getInt(5)),
                new PluginConfig.SlowFalling(
                        root.node("slow-falling", "enabled").getBoolean(true),
                        root.node("slow-falling", "duration-seconds").getInt(5),
                        root.node("slow-falling", "amplifier").getInt(0),
                        root.node("slow-falling", "ambient").getBoolean(false),
                        root.node("slow-falling", "particles").getBoolean(false),
                        root.node("slow-falling", "icon").getBoolean(true),
                        Set.copyOf(slowFallingTriggers)
                ),
                new PluginConfig.Regions(
                        Map.copyOf(providers),
                        regions.node("provider-warning-interval-seconds").getInt(60),
                        regions.node("protectionstones", "delegated-owner-permission").getBoolean(true)
                ),
                new PluginConfig.Integrations(
                        mapBoolean(placeholderApi, "enabled", true),
                        mapString(placeholderApi, "identifier", "claimfly"),
                        mapString(placeholderApi, "status-parameter", "status"),
                        mapBoolean(pvpManager, "enabled", true),
                        mapInt(pvpManager, "warning-interval-seconds", 60)
                )
        );
    }

    private MessagesConfig parseMessages(ConfigurationNode root) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : MESSAGE_KEYS) values.put(key, root.node(key).getString("<red>Missing message: " + key + "</red>"));
        return new MessagesConfig(
                root.node("prefix").getString("<dark_gray>[<aqua>viClaimFly</aqua>]</dark_gray> "),
                Map.copyOf(values),
                root.node("placeholders", "on").getString("On"),
                root.node("placeholders", "off").getString("Off"),
                root.node("placeholders", "warming-up").getString("Warmup")
        );
    }

    private List<String> strings(ConfigurationNode node, List<String> defaults) throws ConfigurateException {
        List<String> values = node.getList(String.class);
        return values == null || values.isEmpty() ? defaults : values;
    }

    private Map<String, List<String>> commandRoutes(ConfigurationNode node) throws ConfigurateException {
        if (node.virtual()) return DEFAULT_COMMAND_ROUTES;
        if (!node.isMap()) throw new ConfigurateException("commands.routes must be a map");
        Map<String, List<String>> routes = new LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String key = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT);
            if (!DEFAULT_COMMAND_ROUTES.containsKey(key)) {
                throw new ConfigurateException("Unknown commands.routes key: " + key);
            }
            List<String> labels = strings(entry.getValue(), List.of());
            if (labels.isEmpty()) throw new ConfigurateException("commands.routes." + key + " cannot be empty");
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String label : labels) {
                String value = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
                if (!COMMAND_LABEL.matcher(value).matches()) {
                    throw new ConfigurateException("Invalid command label in commands.routes." + key + ": " + label);
                }
                normalized.add(value);
            }
            routes.put(key, List.copyOf(normalized));
        }
        DEFAULT_COMMAND_ROUTES.forEach((key, value) -> routes.putIfAbsent(key, value));
        long uniqueLabels = routes.values().stream().flatMap(List::stream).distinct().count();
        long allLabels = routes.values().stream().mapToLong(List::size).sum();
        if (uniqueLabels != allLabels) throw new ConfigurateException("commands.routes contains duplicate labels");
        return Map.copyOf(routes);
    }

    private <E extends Enum<E>> Set<E> enumSet(ConfigurationNode node, Class<E> type,
                                                Set<E> defaults, String path) throws ConfigurateException {
        List<String> values = strings(node, defaults.stream().map(Enum::name).toList());
        LinkedHashSet<E> result = new LinkedHashSet<>();
        for (String value : values) {
            try {
                result.add(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new ConfigurateException("Unknown " + path + " value: " + value, exception);
            }
        }
        return Set.copyOf(result);
    }

    private boolean mapBoolean(ConfigurationNode node, String child, boolean fallback) {
        return node.isMap() ? node.node(child).getBoolean(fallback) : node.getBoolean(fallback);
    }

    private String mapString(ConfigurationNode node, String child, String fallback) {
        return node.isMap() ? node.node(child).getString(fallback) : fallback;
    }

    private int mapInt(ConfigurationNode node, String child, int fallback) {
        return node.isMap() ? node.node(child).getInt(fallback) : fallback;
    }

    private void validate(PluginConfig value) throws ConfigurateException {
        if (value.flight().flySpeed() < -1.0f || value.flight().flySpeed() > 1.0f || value.flight().flySpeed() == 0.0f) {
            throw new ConfigurateException("flight.fly-speed must be between -1.0 and 1.0 and cannot be zero");
        }
        if (value.flight().allowedGameModes().isEmpty()) throw new ConfigurateException("flight.allowed-game-modes cannot be empty");
        if (value.warmup().seconds() < 0 || value.cooldown().seconds() < 0) throw new ConfigurateException("warmup/cooldown seconds cannot be negative");
        if (value.slowFalling().durationSeconds() < 0 || value.slowFalling().amplifier() < 0 || value.slowFalling().amplifier() > 255) {
            throw new ConfigurateException("slow-falling duration/amplifier is outside the supported range");
        }
        List<String> permissionNodes = List.of(
                value.permissions().owner(), value.permissions().autoFlight(), value.permissions().warmup(),
                value.permissions().warmupBypass(), value.permissions().cooldownBypass(),
                value.permissions().joinBypass(), value.permissions().plotRoad(),
                value.permissions().redProtectOwnerBypass(), value.permissions().adminToggle(),
                value.permissions().reload()
        );
        if (permissionNodes.stream().anyMatch(valueNode -> valueNode == null || valueNode.isBlank())
                || value.permissions().trusted().isEmpty()
                || value.permissions().trusted().stream().anyMatch(node -> node == null || node.isBlank())) {
            throw new ConfigurateException("permissions nodes cannot be blank and permissions.trusted cannot be empty");
        }
        if (!PLACEHOLDER_TOKEN.matcher(value.integrations().placeholderIdentifier()).matches()) {
            throw new ConfigurateException("integrations.placeholderapi.identifier must contain letters, numbers, or underscores only");
        }
        if (!PLACEHOLDER_TOKEN.matcher(value.integrations().placeholderStatusParameter()).matches()) {
            throw new ConfigurateException("integrations.placeholderapi.status-parameter must contain letters, numbers, or underscores only");
        }
        if (value.integrations().pvpWarningIntervalSeconds() < 0) {
            throw new ConfigurateException("integrations.pvpmanager.warning-interval-seconds cannot be negative");
        }
        if (value.regions().providerWarningIntervalSeconds() < 0) {
            throw new ConfigurateException("regions.provider-warning-interval-seconds cannot be negative");
        }
    }
}
