# rGoldenHeads

Adds the Golden Heads feature compatible with MCPvP/McTiers.com, and ShieldlessUHC.

An edible player head that trades an inventory slot for burst regeneration and
absorption — the healing item that defines UHC and tiering PvP. Every property of every
item is a config entry, so you can match your server's ruleset instead of the numbers
someone else hardcoded.

## What you get

| Command | Item | Effects | Eats in | Cooldown |
|---|---|---|---|---|
| `/goldenhead` | Golden Head | Regeneration III (5s), Absorption I (2min) | 1.0s | 10s |
| `/instantgoldenhead` | Instant Golden Head | Regeneration III (10s), Absorption II (2min), Speed I (5s) | instantly | 12s |
| `/goldenapple` | Custom Golden Apple | Regeneration II (10s), Absorption I (2min) | 1.6s | none |

All three are edible on a full hunger bar and restore no hunger or saturation.

## Fully configurable

- **Add your own items.** Define as many as you like — each gets its own command and permission.
- **Tune everything:** head texture, name, lore, material, eating speed, cooldown, hunger values, effects.
- **Readable effects.** No amplifier off-by-one, no tick math:

  ```yaml
  effects:
    - type: regeneration
      level: 3        # 1 = plain, 2 = II, 3 = III
      duration: 10    # seconds
  ```

- **MiniMessage or `&` codes** — write whichever you already use.
- **Reload live** with `/rgoldenheads reload`. A bad value never takes the server down: it's
  logged with the exact config path and skipped, so one broken item can't stop the rest.

## Commands

| Command | Permission |
|---|---|
| `/goldenhead` | `rgoldenheads.give.goldenhead` |
| `/instantgoldenhead`, `/instanthead` | `rgoldenheads.give.instantgoldenhead` |
| `/goldenapple`, `/customgapple` | `rgoldenheads.give.goldenapple` |
| `/rgoldenheads reload`, `/rgh` | `rgoldenheads.admin` |

All default to op. Grant `rgoldenheads.give` to allow every item at once.

## Requirements

- **Paper 1.21.11** (or a fork such as Purpur)
- **Java 21**
- No dependencies, nothing shaded — drop the jar in `plugins/` and start.

Source, docs and issues: https://github.com/ZeroArsen/rGoldenHeads · GPL-3.0-only

---

*This project is Vibe Coded.*
