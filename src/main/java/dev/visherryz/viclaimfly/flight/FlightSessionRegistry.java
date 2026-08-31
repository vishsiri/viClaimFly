package dev.visherryz.viclaimfly.flight;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlightSessionRegistry {
    private final Map<UUID, FlightSession> sessions = new ConcurrentHashMap<>();

    public Optional<FlightSession> get(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public boolean contains(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void put(FlightSession session) {
        sessions.put(session.playerId(), session);
    }

    public Optional<FlightSession> remove(UUID playerId) {
        return Optional.ofNullable(sessions.remove(playerId));
    }

    public Collection<FlightSession> snapshot() {
        return List.copyOf(sessions.values());
    }

    public void clear() {
        sessions.clear();
    }
}
