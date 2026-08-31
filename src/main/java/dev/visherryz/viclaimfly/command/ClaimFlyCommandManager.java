package dev.visherryz.viclaimfly.command;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import dev.visherryz.viclaimfly.scheduler.FoliaScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.dynamic.Annotations;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.BukkitLampConfig;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.command.ExecutableCommand;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ClaimFlyCommandManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final FoliaScheduler scheduler;
    private final Lamp<BukkitCommandActor> lamp;
    private final ThreadLocal<ConfiguredCommandRoutes> registeringRoutes = new ThreadLocal<>();
    private List<ExecutableCommand<BukkitCommandActor>> registered = List.of();
    private ConfiguredCommandRoutes routes;
    private Object handler;

    public ClaimFlyCommandManager(JavaPlugin plugin, ConfigurationService configuration,
                                  FoliaScheduler scheduler) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.scheduler = scheduler;
        BukkitLampConfig<BukkitCommandActor> lampConfig = BukkitLampConfig
                .<BukkitCommandActor>builder(plugin)
                .disableBrigadier()
                .build();
        lamp = BukkitLamp.builder(lampConfig)
                .annotationReplacer(Command.class, this::replaceRoutes)
                .build();
    }

    public synchronized void register(Object commandHandler) {
        if (handler != null) throw new IllegalStateException("Commands are already registered");
        handler = commandHandler;
        routes = configuredRoutes();
        registered = register(routes);
        refreshClientCommands();
    }

    public synchronized void reload() {
        if (handler == null) throw new IllegalStateException("Commands are not registered");
        ConfiguredCommandRoutes next = configuredRoutes();
        if (routes.sameRoutes(next)) return;

        ConfiguredCommandRoutes previousRoutes = routes;
        unregister(registered);
        try {
            registered = register(next);
            routes = next;
            refreshClientCommands();
        } catch (RuntimeException failure) {
            try {
                registered = register(previousRoutes);
                routes = previousRoutes;
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
                registered = List.of();
            }
            throw failure;
        }
    }

    @Override
    public synchronized void close() {
        lamp.unregisterAllCommands();
        registered = List.of();
        handler = null;
    }

    private Collection<Annotation> replaceRoutes(java.lang.reflect.AnnotatedElement element, Command command) {
        ConfiguredCommandRoutes active = registeringRoutes.get();
        if (active == null) return List.of(command);
        return List.of(Annotations.create(Command.class, "value", active.rewrite(command.value())));
    }

    private ConfiguredCommandRoutes configuredRoutes() {
        return ConfiguredCommandRoutes.create(configuration.config().commands().routes());
    }

    private List<ExecutableCommand<BukkitCommandActor>> register(ConfiguredCommandRoutes commandRoutes) {
        registeringRoutes.set(commandRoutes);
        try {
            return List.copyOf(lamp.register(handler));
        } finally {
            registeringRoutes.remove();
        }
    }

    private void unregister(List<ExecutableCommand<BukkitCommandActor>> commands) {
        new ArrayList<>(commands).forEach(lamp::unregister);
    }

    private void refreshClientCommands() {
        if (!plugin.isEnabled()) return;
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, ignored ->
                plugin.getServer().getOnlinePlayers().forEach(player ->
                        scheduler.forEntity(player, player::updateCommands, () -> { })), 1L);
    }
}
