package dev.visherryz.viclaimfly;

import dev.visherryz.viclaimfly.command.ClaimFlyCommands;
import dev.visherryz.viclaimfly.command.ClaimFlyCommandManager;
import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.flight.ClaimFlightController;
import dev.visherryz.viclaimfly.flight.CooldownService;
import dev.visherryz.viclaimfly.flight.FlightService;
import dev.visherryz.viclaimfly.flight.FlightSessionRegistry;
import dev.visherryz.viclaimfly.flight.WarmupService;
import dev.visherryz.viclaimfly.integration.ClaimFlyPlaceholderExpansion;
import dev.visherryz.viclaimfly.integration.PvpManagerHook;
import dev.visherryz.viclaimfly.listener.PlayerDamageListener;
import dev.visherryz.viclaimfly.listener.PlayerLifecycleListener;
import dev.visherryz.viclaimfly.listener.PlayerMovementListener;
import dev.visherryz.viclaimfly.logging.DebugLogger;
import dev.visherryz.viclaimfly.message.MessageService;
import dev.visherryz.viclaimfly.region.ClaimAccessService;
import dev.visherryz.viclaimfly.region.ClaimProviderRegistry;
import dev.visherryz.viclaimfly.region.OwnerPermissionLookup;
import dev.visherryz.viclaimfly.region.PermissionPolicy;
import dev.visherryz.viclaimfly.scheduler.FoliaScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class ViClaimFlyPlugin extends JavaPlugin {
    private ConfigurationService configuration;
    private MessageService messages;
    private FoliaScheduler scheduler;
    private ClaimAccessService claims;
    private ClaimProviderRegistry providerRegistry;
    private OwnerPermissionLookup ownerPermissions;
    private PvpManagerHook pvpManager;
    private FlightService flights;
    private WarmupService warmups;
    private CooldownService cooldowns;
    private ClaimFlyCommandManager commandManager;
    private ClaimFlyPlaceholderExpansion placeholderExpansion;
    private DebugLogger debug;
    private final AtomicBoolean reloadInProgress = new AtomicBoolean();

    @Override
    public void onEnable() {
        try {
            configuration = new ConfigurationService(this);
            configuration.load();
            messages = new MessageService(configuration);
            debug = new DebugLogger(this, configuration);
            scheduler = new FoliaScheduler(this);

            PermissionPolicy permissions = new PermissionPolicy(configuration);
            ownerPermissions = new OwnerPermissionLookup(this);
            claims = new ClaimAccessService();
            providerRegistry = new ClaimProviderRegistry(this, configuration, permissions, ownerPermissions);
            claims.replaceProviders(providerRegistry.discover());

            FlightSessionRegistry sessions = new FlightSessionRegistry();
            cooldowns = new CooldownService();
            warmups = new WarmupService(this, configuration, permissions);
            flights = new FlightService(configuration, messages, sessions, debug);
            pvpManager = new PvpManagerHook(this, configuration);
            ClaimFlightController controller = new ClaimFlightController(
                    configuration, messages, claims, flights, cooldowns, warmups, pvpManager, debug, permissions
            );

            getServer().getPluginManager().registerEvents(new PlayerMovementListener(controller), this);
            getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(controller, scheduler), this);
            getServer().getPluginManager().registerEvents(new PlayerDamageListener(controller), this);

            commandManager = new ClaimFlyCommandManager(this, configuration, scheduler);
            commandManager.register(new ClaimFlyCommands(
                    this, controller, flights, messages, scheduler, permissions
            ));

            refreshPlaceholderIntegration();
            if (claims.providerCount() == 0) {
                getLogger().warning("No supported claim provider is active; claim flight will remain unavailable.");
            }
            getLogger().info("viClaimFly " + getPluginMeta().getVersion() + " enabled with providers: " + claims.providerIds());
            debug.log("startup providerCount=" + claims.providerCount() + " providers=" + claims.providerIds());
        } catch (Throwable failure) {
            getLogger().log(Level.SEVERE, "Unable to enable viClaimFly", failure);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) placeholderExpansion.unregister();
        if (commandManager != null) commandManager.close();
        if (warmups != null) warmups.close();
        if (cooldowns != null) cooldowns.clear();
        if (ownerPermissions != null) ownerPermissions.close();
        if (flights != null) flights.close();
    }

    public void reloadFromCommand(CommandSender sender) {
        if (!reloadInProgress.compareAndSet(false, true)) {
            scheduler.forSender(sender, () -> messages.send(sender, "reload-failed"));
            return;
        }
        getServer().getAsyncScheduler().runNow(this, ignored -> {
            try {
                ConfigurationService.Snapshot candidate = configuration.readSnapshot();
                getServer().getGlobalRegionScheduler().execute(this, () -> {
                    ConfigurationService.Snapshot previous = configuration.snapshot();
                    try {
                        configuration.apply(candidate);
                        claims.replaceProviders(providerRegistry.discover());
                        pvpManager.refresh();
                        commandManager.reload();
                        refreshPlaceholderIntegration();
                        debug.log("reload providerCount=" + claims.providerCount() + " providers=" + claims.providerIds());
                        scheduler.forSender(sender, () -> messages.send(sender, "reload-success",
                                MessageService.text("providers", claims.providerCount())));
                    } catch (Throwable failure) {
                        configuration.restore(previous);
                        try {
                            claims.replaceProviders(providerRegistry.discover());
                            pvpManager.refresh();
                            commandManager.reload();
                            refreshPlaceholderIntegration();
                        } catch (Throwable rollbackFailure) {
                            failure.addSuppressed(rollbackFailure);
                        }
                        getLogger().log(Level.SEVERE, "Configuration reload failed; previous runtime state was restored", failure);
                        scheduler.forSender(sender, () -> messages.send(sender, "reload-failed"));
                    } finally {
                        reloadInProgress.set(false);
                    }
                });
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "Configuration reload failed; previous values remain active", failure);
                reloadInProgress.set(false);
                scheduler.forSender(sender, () -> messages.send(sender, "reload-failed"));
            }
        });
    }

    private void refreshPlaceholderIntegration() {
        boolean wanted = configuration.config().integrations().placeholderApi();
        boolean available = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (wanted && available) {
            ClaimFlyPlaceholderExpansion expansion = new ClaimFlyPlaceholderExpansion(this, configuration, flights, warmups);
            if (expansion.register()) placeholderExpansion = expansion;
            else getLogger().warning("PlaceholderAPI refused to register the configured expansion identifier");
        }
    }
}
