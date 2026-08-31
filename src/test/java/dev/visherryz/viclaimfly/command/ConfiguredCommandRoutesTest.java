package dev.visherryz.viclaimfly.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredCommandRoutesTest {
    @Test
    void rewritesEveryConfiguredRootAndPreservesSubcommands() {
        ConfiguredCommandRoutes routes = ConfiguredCommandRoutes.create(Map.of(
                "claimfly", List.of("flyclaim", "fc"),
                "claimflyreload", List.of("flyreload")
        ));

        assertThat(routes.rewrite(new String[]{"claimfly", "claimfly status"}))
                .containsExactly("flyclaim", "fc", "flyclaim status", "fc status");
        assertThat(routes.rewrite(new String[]{"claimflyreload"})).containsExactly("flyreload");
    }

    @Test
    void routeEqualityDetectsReloadChanges() {
        ConfiguredCommandRoutes first = ConfiguredCommandRoutes.create(Map.of("claimfly", List.of("claimfly")));
        ConfiguredCommandRoutes same = ConfiguredCommandRoutes.create(Map.of("claimfly", List.of("claimfly")));
        ConfiguredCommandRoutes changed = ConfiguredCommandRoutes.create(Map.of("claimfly", List.of("cf")));

        assertThat(first.sameRoutes(same)).isTrue();
        assertThat(first.sameRoutes(changed)).isFalse();
    }
}
