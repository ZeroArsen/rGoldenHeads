# rGoldenHeads

Adds the Golden Heads feature compatible with MCPvP/McTiers.com, and ShieldlessUHC.

An edible player head that trades an inventory slot for burst regeneration and
absorption — the healing item that defines UHC and tiering PvP. Every property of every
item is a config entry, so you can match your server's ruleset instead of the numbers
someone else hardcoded.

**One jar for Paper 1.21.4 – 26.3, Folia included.**

## What you get

| Command | Item | Effects | Eats in | Cooldown |
|---|---|---|---|---|
| `/goldenhead` | Golden Head | Regeneration III (5s), Absorption I (2min) | 1.0s | 10s |
| `/instantgoldenhead` | Instant Golden Head | Regeneration III (10s), Absorption II (2min), Speed I (5s) | instantly | 12s |
| `/goldenapple` | Custom Golden Apple | Regeneration II (10s), Absorption I (2min) | 1.6s | none |

All three are edible on a full hunger bar and restore no hunger or saturation.

## Fully configurable

- **Two head skins built in.** `modern` is the default, and `classic` brings back the
  original look with one line (`head-skin: classic`). You can also add your own.
- **Add your own items.** Define as many as you like; each gets its own command and permission.
- **Tune everything:** name, lore, material, eating speed, cooldown, hunger values, effects.
- **Readable effects.** No amplifier off-by-one, no tick math:

  ```yaml
  effects:
    - type: regeneration
      level: 3        # 1 = plain, 2 = II, 3 = III
      duration: 10    # seconds
  ```

- **MiniMessage or `&` codes**: write whichever you already use.
- **Reload live** with `/rgoldenheads reload`. A bad value never takes the server down: it's
  logged with the exact config path and skipped, so one broken item can't stop the rest.

## Commands

| Command | Permission |
|---|---|
| `/goldenhead` | `rgoldenheads.give.goldenhead` |
| `/instantgoldenhead`, `/instanthead` | `rgoldenheads.give.instantgoldenhead` |
| `/goldenapple`, `/customgapple` | `rgoldenheads.give.goldenapple` |
| `/rgoldenheads give <player\|*> <item> [amount]` | `rgoldenheads.admin.give` |
| `/rgoldenheads reload`, `/rgh` | `rgoldenheads.admin.reload` |

All default to op. `/rgoldenheads give` works from the console, so kit and UHC plugins can
hand items out. Grant `rgoldenheads.give` to allow every item for self-use, or
`rgoldenheads.admin` for everything.

## Requirements

- **Paper 1.21.4 – 26.3** (or a fork such as Purpur), or **Folia**
- The Java version your server already needs: 21 for 1.21.x, 25 for 26.x
- No dependencies, nothing shaded. Drop the jar in `plugins/` and start.

Every supported version is booted and checked automatically before each release. An
optional update check asks Modrinth once per startup for a newer build matching your server
version; turn it off with `update-checker.enabled: false`.

Source, docs and issues: https://github.com/ZeroArsen/rGoldenHeads · GPL-3.0-only

---

*This project is Vibe Coded.*
