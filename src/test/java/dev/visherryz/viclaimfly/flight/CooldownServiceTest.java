package dev.visherryz.viclaimfly.flight;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CooldownServiceTest {
    @Test
    void roundsUpPartialSecondsAndExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        CooldownService service = new CooldownService(clock);
        UUID player = UUID.randomUUID();

        service.start(player, 5);
        assertThat(service.secondsLeft(player)).isEqualTo(5);
        clock.advanceMillis(4_001);
        assertThat(service.secondsLeft(player)).isEqualTo(1);
        clock.advanceMillis(999);
        assertThat(service.secondsLeft(player)).isZero();
    }

    @Test
    void zeroDurationClearsAnExistingCooldown() {
        CooldownService service = new CooldownService();
        UUID player = UUID.randomUUID();
        service.start(player, 20);
        service.start(player, 0);
        assertThat(service.secondsLeft(player)).isZero();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) { this.instant = instant; }
        void advanceMillis(long millis) { instant = instant.plusMillis(millis); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
