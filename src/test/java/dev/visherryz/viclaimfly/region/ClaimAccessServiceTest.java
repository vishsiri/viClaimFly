package dev.visherryz.viclaimfly.region;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimAccessServiceTest {
    @Test
    void anyAllowWinsAcrossOverlappingProviders() {
        ClaimAccessService service = new ClaimAccessService();
        service.replaceProviders(List.of(provider("first", ClaimDecision.DENY), provider("second", ClaimDecision.ALLOW)));
        assertThat(service.canFly(null)).isTrue();
    }

    @Test
    void failuresAndPendingResultsDenySafely() {
        ClaimAccessService service = new ClaimAccessService();
        service.replaceProviders(List.of(provider("pending", ClaimDecision.PENDING), provider("broken", ClaimDecision.ERROR)));
        assertThat(service.canFly(null)).isFalse();
    }

    @Test
    void providerReplacementIsAtomicFromTheReaderPerspective() {
        ClaimAccessService service = new ClaimAccessService();
        service.replaceProviders(List.of(provider("old", ClaimDecision.DENY)));
        service.replaceProviders(List.of(provider("new", ClaimDecision.ALLOW)));
        assertThat(service.providerIds()).containsExactly("new");
    }

    private ClaimProvider provider(String id, ClaimDecision result) {
        return new ClaimProvider() {
            @Override public String id() { return id; }
            @Override public ClaimDecision evaluate(Player player) { return result; }
        };
    }
}
