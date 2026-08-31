# Deployment

1. Stop Paper/Folia completely.
2. Remove the old ChocoFly JAR and place `viClaimFly-1.1.0.jar` in `plugins`.
3. Start once to generate `plugins/viClaimFly/config.yml` and `messages.yml`.
4. Assign `claim.fly` to claim owners. Add `claim.fly.trusted` for members, and `claim.fly.autofly` if desired. Grant `claim.fly.join.bypass` only to players whose external flight state must survive join cleanup.
5. Review `regions.providers` and disable hooks not used by the server.
6. Edit MiniMessage text in `plugins/viClaimFly/messages.yml`, then use `/claimfly reload`.

ChocoFly's flat YAML is not read automatically because several old keys had ambiguous or broken semantics. Equivalent values are:

| ChocoFly | viClaimFly |
|---|---|
| `slow_falling` | `slow-falling.duration-seconds` |
| `enable_slowfalling` | `slow-falling.enabled` |
| `claimfly_cooldown` | `cooldown.seconds` |
| `warmup` | `warmup.seconds` |
| `auto_claimfly` | `flight.auto-enabled` |
| `claimfly_notif` | `notifications.manual` |
| `autoclaimfly_notif` | `notifications.auto` |
| `debug` | `debug` |
| permission nodes | `permissions.*` |
| command aliases | `commands.routes.*` |
| `claimfly_*`, `autofly_*`, `ondamaged` | keys in `messages.yml` |
| `claimfly_*_placeholder` | `messages.yml` under `placeholders` |

Legacy `&` color strings must be converted to MiniMessage, for example `&aON` becomes `<green>ON</green>`.

Version 1.1 uses `config-version: 2`. Existing 1.0 files still receive backward-compatible defaults, including legacy `regions.owner-permission`, `regions.trusted-permissions`, and scalar integration toggles. To expose every setting for editing, merge the complete bundled 1.1 `config.yml` into the deployed file before reload.
