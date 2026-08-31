package dev.visherryz.viclaimfly.command;

import dev.visherryz.viclaimfly.ViClaimFlyPlugin;
import dev.visherryz.viclaimfly.flight.ClaimFlightController;
import dev.visherryz.viclaimfly.flight.FlightService;
import dev.visherryz.viclaimfly.message.MessageService;
import dev.visherryz.viclaimfly.region.PermissionPolicy;
import dev.visherryz.viclaimfly.scheduler.FoliaScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

public final class ClaimFlyCommands {
    private final ViClaimFlyPlugin plugin;
    private final ClaimFlightController controller;
    private final FlightService flights;
    private final MessageService messages;
    private final FoliaScheduler scheduler;
    private final PermissionPolicy permissions;

    public ClaimFlyCommands(ViClaimFlyPlugin plugin, ClaimFlightController controller,
                            FlightService flights, MessageService messages, FoliaScheduler scheduler,
                            PermissionPolicy permissions) {
        this.plugin = plugin;
        this.controller = controller;
        this.flights = flights;
        this.messages = messages;
        this.scheduler = scheduler;
        this.permissions = permissions;
    }

    @Command("claimfly")
    public void toggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return;
        }
        scheduler.forEntity(player, () -> controller.toggle(player), () -> { });
    }

    @Command("claimfly status")
    public void status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return;
        }
        scheduler.forEntity(player, () -> messages.send(player,
                flights.isManaged(player.getUniqueId()) ? "status-on" : "status-off"), () -> { });
    }

    @Command("claimfly toggle")
    public void toggleOther(CommandSender sender, Player target) {
        scheduleAdminToggle(sender, target);
    }

    @Command("claimfly")
    public void legacyToggleOther(CommandSender sender, Player target) {
        scheduleAdminToggle(sender, target);
    }

    private void scheduleAdminToggle(CommandSender sender, Player target) {
        if (!permissions.adminToggle(sender)) {
            messages.send(sender, "no-permission");
            return;
        }
        scheduler.forEntity(target, () -> {
            controller.adminToggle(target);
            scheduler.forSender(sender, () -> messages.send(sender, "admin-toggled", MessageService.text("player", target.getName())));
        }, () -> scheduler.forSender(sender, () -> messages.send(sender, "player-not-found")));
    }

    @Command("claimfly reload")
    public void reload(CommandSender sender) {
        if (!permissions.reload(sender)) {
            messages.send(sender, "no-permission");
            return;
        }
        plugin.reloadFromCommand(sender);
    }

    @Command("claimflyreload")
    public void directReload(CommandSender sender) {
        reload(sender);
    }
}
