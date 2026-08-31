package dev.visherryz.viclaimfly.flight;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownService {
    private final Clock clock;
    private final Map<UUID, Instant> endsAt = new ConcurrentHashMap<>();

    public CooldownService() {
        this(Clock.systemUTC());
    }

    CooldownService(Clock clock) {
        this.clock = clock;
    }

    public void start(UUID playerId, int seconds) {
        if (seconds <= 0) {
            endsAt.remove(playerId);
            return;
        }
        endsAt.put(playerId, clock.instant().plusSeconds(seconds));
    }

    public long secondsLeft(UUID playerId) {
        Instant end = endsAt.get(playerId);
        if (end == null) return 0L;
        Duration remaining = Duration.between(clock.instant(), end);
        if (remaining.isNegative() || remaining.isZero()) {
            endsAt.remove(playerId, end);
            return 0L;
        }
        return Math.max(1L, (remaining.toMillis() + 999L) / 1000L);
    }

    public void remove(UUID playerId) {
        endsAt.remove(playerId);
    }

    public void clear() {
        endsAt.clear();
    }
}
