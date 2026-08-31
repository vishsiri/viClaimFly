# ChocoFly 0.56.12 behavioral reference

Reference artifact SHA-256: `3FC3A6D7F48EC3C215BEAE2A7C4147ADD206F3FCB3BB9E41D04E1E61C328594D`.

The JAR was treated as read-only behavioral evidence. It exposes manual and automatic claim flight, a five-second default cooldown, a two-second warmup setting, slow falling on claim exit, fall-damage cancellation, optional PvPManager revocation, `%claimfly_status%`, and eight claim-provider hooks.

The rewrite intentionally corrects these observed defects:

- Warmup state existed, but the first command enabled flight immediately instead of waiting.
- The admin target branch rejected the normal one-argument shape and could toggle twice.
- Player objects were retained as map keys.
- Reload updated only part of runtime state; command timing and placeholders stayed stale.
- The WorldGuard detection name used incorrect casing.
- PlotSquared required the road permission even for normal owned plots.
- Claim-exit cleanup only ran while actively flying, which could leave `allowFlight` outside a claim.
- Shutdown cleanup depended on reflection into private state.

The new implementation preserves user-facing aliases, the root admin-target command shape, legacy trusted permissions, separate manual/automatic notification switches, functional debug logging, and `claim.fly.join.bypass` cleanup behavior. It uses immutable configuration snapshots, UUID state, explicit scheduler ownership, isolated adapters, and previous-flight-state restoration.
