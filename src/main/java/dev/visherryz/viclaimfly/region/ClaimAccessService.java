package dev.visherryz.viclaimfly.region;

import org.bukkit.entity.Player;

import java.util.List;

public final class ClaimAccessService {
    private volatile List<ClaimProvider> providers = List.of();

    public void replaceProviders(List<ClaimProvider> next) {
        providers = List.copyOf(next);
    }

    public boolean canFly(Player player) {
        for (ClaimProvider provider : providers) {
            if (provider.evaluate(player) == ClaimDecision.ALLOW) return true;
        }
        return false;
    }

    public int providerCount() {
        return providers.size();
    }

    public List<String> providerIds() {
        return providers.stream().map(ClaimProvider::id).toList();
    }
}
