package dev.visherryz.viclaimfly.listener;

import dev.visherryz.viclaimfly.flight.ClaimFlightController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class PlayerDamageListener implements Listener {
    private final ClaimFlightController controller;

    public PlayerDamageListener(ClaimFlightController controller) {
        this.controller = controller;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void cancelManagedFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && controller.cancelFallDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) controller.onDamage(player);
    }
}
