# Full configuration boundary

`config.yml` controls operational behavior. `messages.yml` controls every player-facing message and PlaceholderAPI display value. `/claimfly reload` validates a complete candidate, updates command routes, providers, PvPManager, PlaceholderAPI, permissions, and messages, and restores the previous runtime state if application fails.

## Configurable systems

- `commands.routes`: main roots and aliases for the flight tree and direct reload tree. Labels must be unique lowercase command-safe values.
- `permissions`: owner, trusted list, auto-flight, warmup, bypasses, PlotSquared road, RedProtect owner bypass, admin toggle, and reload nodes.
- `notifications`: independent manual, automatic, and admin flight-toggle messages.
- `flight`: manual/automatic availability, state restoration, speed, game modes, world/game-mode/death/join/combat cleanup, and fall-damage protection.
- `warmup`: duration, target mode, and movement/damage/claim-exit cancellation.
- `cooldown`: duration.
- `slow-falling`: enabled state, duration, amplifier, ambient/particles/icon presentation, and disable reasons in `apply-on`.
- `regions`: provider warning throttle, individual provider toggles, and ProtectionStones delegated-owner policy.
- `integrations.placeholderapi`: enabled state, expansion identifier, and status parameter.
- `integrations.pvpmanager`: enabled state and failure-warning throttle.
- `debug`: transition/provider diagnostics.
- `messages.yml`: prefix, every command/event response, and placeholder values. A blank message suppresses only that response.

Valid `slow-falling.apply-on` values are `MANUAL`, `ADMIN`, `CLAIM_EXIT`, `WORLD_CHANGE`, `GAME_MODE`, `COMBAT`, `JOIN`, `QUIT`, and `DEATH`.

## Intentionally fixed technical contracts

Plugin identity, Java/Paper API contracts, external provider class and method names, tick/millisecond conversion constants, Folia ownership handoff delays, and validation limits imposed by Minecraft remain in code. They are implementation contracts rather than server policy and changing them in YAML would make an invalid or unsafe runtime possible.

`plugin.yml` still declares the bundled default permission nodes so Bukkit can publish descriptions and operator defaults. Runtime permission checks use `permissions.*`; custom nodes do not require Java changes.
