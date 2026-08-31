# Claim access and permission policy

viClaimFly always evaluates the player's real relationship to the claim. A permission does not turn wilderness into a claim and does not allow a non-member into somebody else's claim.

## Individual permission mode

Use this mode when every player must possess their own `claim.fly` permission. The player must also be either the claim owner or an actual member/trusted player.

```yaml
permissions:
  owner: claim.fly
  trusted:
    - claim.fly

regions:
  protectionstones:
    delegated-owner-permission: false
```

| Claim relationship | Player has `claim.fly` | Result |
|---|---:|---|
| Owner | Yes | Allowed |
| Owner | No | Denied |
| Added member/trusted | Yes | Allowed |
| Added member/trusted | No | Denied |
| Not added to another player's claim | Yes | Denied |
| Wilderness | Yes | Denied |

ProtectionStones must use `delegated-owner-permission: false` in this mode. Otherwise a member can inherit eligibility from an owner who has `permissions.owner`, even when the member does not have their own flight permission.

Grant the permission with LuckPerms, for example:

```text
/lp group vip permission set claim.fly true
```

Automatic flight additionally requires the configured `permissions.auto-flight` node, which defaults to `claim.fly.autofly`:

```text
/lp group vip permission set claim.fly.autofly true
```

Apply the YAML change with:

```text
/claimfly reload
```

## Separate trusted permission mode

The bundled default keeps trusted/member permission separate:

```yaml
permissions:
  owner: claim.fly
  trusted:
    - claim.fly.trusted
    - claim.fly.other
    - claim.fly.others
    - claim.fly.member
    - claim.fly.members
```

In this mode, a member must have `permissions.owner` and at least one node from `permissions.trusted`. For example, grant both `claim.fly` and `claim.fly.trusted`. Being added to a claim is still required.

## ProtectionStones delegated-owner compatibility

With the compatibility option enabled:

```yaml
regions:
  protectionstones:
    delegated-owner-permission: true
```

An actual ProtectionStones member is allowed when at least one region owner has `permissions.owner`. The member's personal owner/trusted permission is not required in this specific mode. Online owners are checked through Bukkit permissions; offline owners use the optional LuckPerms lookup. Access remains denied while an offline lookup is pending or when the lookup fails.

Set this option to `false` for individual permission ownership. Other claim providers always use the player's own permission policy and their real owner/member relationship.

## Security boundary

- Provider errors fail closed and never become permission bypasses.
- A configured permission does not bypass claim membership.
- Wilderness remains denied.
- Disabled providers are not queried.
- PlotSquared roads additionally require `permissions.plot-road`.
- RedProtect owner bypass identity uses `permissions.redprotect-owner-bypass` before applying the normal owner permission policy.
