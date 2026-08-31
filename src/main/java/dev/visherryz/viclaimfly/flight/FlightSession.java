package dev.visherryz.viclaimfly.flight;

import java.util.UUID;

public record FlightSession(UUID playerId, ActivationSource source, boolean previousAllowFlight,
                            boolean previousFlying, float previousFlySpeed) { }
