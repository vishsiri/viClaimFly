# viClaimFly

`viClaimFly` is an original Paper/Folia claim-flight plugin rebuilt from a behavioral review of ChocoFly 0.56.12. The reference JAR is not included and its decompiled source is not copied into this repository.

## Platform

- Java 21
- Paper or Folia 1.21.11
- Lamp commands
- Configurate YAML
- Adventure MiniMessage
- Optional PlaceholderAPI and PvPManager integrations

Supported claim providers are ProtectionStones, Lands, HuskClaims, GriefPrevention, RedProtect, GriefDefender, PlotSquared, and WorldGuard. Every hook is isolated and fail-closed: one incompatible dependency cannot grant flight or stop the other providers.

## Commands

- `/claimfly` toggles managed flight in an eligible claim.
- `/claimfly status` shows the managed-flight state.
- `/claimfly toggle <player>` toggles an online target (`permissions.admin-toggle`).
- `/claimfly <player>` preserves the legacy root admin syntax.
- `/claimfly reload` atomically reloads both YAML files (`permissions.reload`).
- Default legacy roots are `/regionfly`, `/chocofly`, `/pfly`, `/cfly`, `/rfly`, `/claimflyreload`, and `/cfreload`. Every root and alias is configurable under `commands.routes` and updates safely on reload.

## Default permissions

- `claim.fly`: owner access.
- `claim.fly.trusted`: trusted/member access, in addition to `claim.fly`.
- `claim.fly.autofly`: automatic flight on claim entry.
- `claim.fly.warmup`: receives warmup when `warmup.mode: PERMISSION`.
- `claim.fly.warmup.bypass` and `claim.fly.cooldown.bypass`: bypass timing controls.
- `claim.fly.join.bypass`: preserves the player's flight state during join cleanup.
- `claim.fly.plotroad`: enables PlotSquared road flight.
- Legacy trusted permissions (`claim.fly.other`, `.others`, `.member`, `.members`) are supported.

These are the bundled defaults. Runtime permission nodes are read from `permissions.*` in `config.yml`.

ProtectionStones can preserve the patched ChocoFly delegation contract: a member may fly when an owner has `claim.fly`. Online owners use Bukkit permissions; offline owners use an optional LuckPerms cached/async lookup. While an offline owner is loading, access is denied until a later movement or command re-check.

## Configuration

Configuration is split between [`config.yml`](src/main/resources/config.yml) and [`messages.yml`](src/main/resources/messages.yml). Messages use MiniMessage. Reload parses and validates complete candidate snapshots before publishing them, so invalid YAML leaves the previous runtime configuration active.

Command roots, permissions, flight lifecycle rules, notification channels, timing, potion presentation, provider policies, PlaceholderAPI naming, PvP warning throttling, and every player-facing message are configurable. Set an individual entry in `messages.yml` to an empty string to suppress it. See [`CONFIGURATION.md`](docs/CONFIGURATION.md) for the complete boundary.

`notifications.manual`, `notifications.auto`, and `notifications.admin` independently control flight toggle messages. `debug: true` writes managed flight transitions, join cleanup, startup provider discovery, and reload provider discovery to the server log.

Warmup now waits before enabling flight. `PERMISSION` mode keeps the old permission shape, `ALL` applies warmup to everyone except the bypass permission, and `DISABLED` turns it off. Movement, damage, claim exit, death, quit, world changes, and combat all have explicit cleanup paths.

## Build

```bat
gradlew.bat clean test build
```

The deployable artifact is `build/libs/viClaimFly-1.1.0.jar`. The build relocates Lamp, Configurate, and SnakeYAML and checks that Bukkit, Adventure, Folia, and PlaceholderAPI classes are not bundled.

Do not use PlugMan or `/reload`. Stop the server fully, replace the JAR, and start it normally so entity-owned flight state and optional dependency lifecycles remain consistent.

## Runtime acceptance checklist

Test on the exact production provider versions:

1. Owner and trusted/member access, plus denial in wilderness.
2. Manual toggle, actual warmup delay/cancellation, cooldown, and auto-flight claim entry/exit.
3. Survival/adventure versus creative/spectator state restoration.
4. World change, death, quit/rejoin, fall protection, and PvPManager combat revocation.
5. `%claimfly_status%`, `/claimfly reload`, and an invalid-YAML rollback attempt.
6. Cross-region movement on Folia with no thread-ownership warnings.
