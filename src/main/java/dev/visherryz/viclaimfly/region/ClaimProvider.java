package dev.visherryz.viclaimfly.region;

import org.bukkit.entity.Player;

public interface ClaimProvider {
    String id();

    ClaimDecision evaluate(Player player);
}
