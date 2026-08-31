package dev.visherryz.viclaimfly.listener;

import dev.visherryz.viclaimfly.flight.ClaimFlightController;
import dev.visherryz.viclaimfly.scheduler.FoliaScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {
    private final ClaimFlightController controller;
    private final FoliaScheduler scheduler;

    public PlayerLifecycleListener(ClaimFlightController controller, FoliaScheduler scheduler) {
        this.controller = controller;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        controller.onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        controller.onWorldChange(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        scheduler.forEntityLater(event.getPlayer(), () -> controller.onGameModeChange(event.getPlayer()), () -> { }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        controller.onDeath(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        controller.onQuit(event.getPlayer());
    }
}
