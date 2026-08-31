package dev.visherryz.viclaimfly.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FoliaScheduler {
    private final Plugin plugin;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void forEntity(Entity entity, Runnable action, Runnable retired) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            action.run();
            return;
        }
        entity.getScheduler().execute(plugin, action, retired, 1L);
    }

    public void forSender(CommandSender sender, Runnable action) {
        if (sender instanceof Player player) forEntity(player, action, () -> { });
        else plugin.getServer().getGlobalRegionScheduler().execute(plugin, action);
    }

    public void forEntityLater(Entity entity, Runnable action, Runnable retired, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, ignored -> action.run(), retired, delayTicks);
    }
}
