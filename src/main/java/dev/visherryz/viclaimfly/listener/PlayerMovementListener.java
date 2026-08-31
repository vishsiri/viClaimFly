package dev.visherryz.viclaimfly.listener;

import dev.visherryz.viclaimfly.flight.ClaimFlightController;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PlayerMovementListener implements Listener {
    private final ClaimFlightController controller;

    public PlayerMovementListener(ClaimFlightController controller) {
        this.controller = controller;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!changedBlock(event.getFrom(), event.getTo())) return;
        controller.onBlockMove(event.getPlayer());
    }

    private boolean changedBlock(Location from, Location to) {
        return from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
