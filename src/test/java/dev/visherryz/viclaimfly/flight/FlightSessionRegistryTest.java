package dev.visherryz.viclaimfly.flight;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlightSessionRegistryTest {
    @Test
    void replacesSessionWithoutKeepingPlayerObjects() {
        FlightSessionRegistry registry = new FlightSessionRegistry();
        UUID player = UUID.randomUUID();
        registry.put(new FlightSession(player, ActivationSource.AUTO, false, false, 0.1f));
        registry.put(new FlightSession(player, ActivationSource.MANUAL, false, false, 0.1f));

        assertThat(registry.snapshot()).hasSize(1);
        assertThat(registry.get(player)).get().extracting(FlightSession::source).isEqualTo(ActivationSource.MANUAL);
    }

    @Test
    void removeReturnsTheOwnedSnapshot() {
        FlightSessionRegistry registry = new FlightSessionRegistry();
        UUID player = UUID.randomUUID();
        FlightSession session = new FlightSession(player, ActivationSource.ADMIN, true, true, 0.2f);
        registry.put(session);

        assertThat(registry.remove(player)).contains(session);
        assertThat(registry.contains(player)).isFalse();
    }
}
