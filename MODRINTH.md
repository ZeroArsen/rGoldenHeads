# rGoldenHeads

Adds the Golden Heads feature compatible with MCPvP/McTiers.com, and ShieldlessUHC.

Golden heads are the healing item that defines UHC and tiering PvP — an edible player
head that trades a hunger slot for burst regeneration and absorption. rGoldenHeads gives
you that item, and then gets out of the way: **everything about it is a config entry**, so
you can match whatever ruleset your server runs instead of whatever the plugin author
happened to hardcode.

## The default items

| Command | Item | Effects | Eats in | Cooldown |
|---|---|---|---|---|
| `/goldenhead` | Golden Head | Regeneration III (5s), Absorption I (2min) | 1.0s | 10s |
| `/instantgoldenhead` | Instant Golden Head | Regeneration III (10s), Absorption II (2min), Speed I (5s) | instantly | 12s |
| `/goldenapple` | Custom Golden Apple | Regeneration II (10s), Absorption I (2min) | 1.6s | none |

All three are edible on a full hunger bar and restore no hunger or saturation.

Don't like those numbers? Change them. Don't want three items? Delete two, or add five more.

## Everything is configurable

Each item is a block in `config.yml`. You control its material, head texture, display
name, lore, enchantment glint, stack amount, eating speed, eating animation, hunger and
saturation values, cooldown, cooldown group, and its full list of potion effects.

Effects read the way the game shows them — no amplifier off-by-one, no tick math:

```yaml
effects:
  - type: regeneration
    level: 3          # 1 = plain, 2 = II, 3 = III
    duration: 10      # seconds. -1 for infinite
```

Text accepts **MiniMessage** (`<gold>Golden Head`, gradients, hex) or classic `&` colour
codes — whichever you already write. It detects which one you used per string.

### Adding your own item

```yaml
items:
  diamond_head:
    command: diamondhead
    display-name: "<aqua>Diamond Head"
    material: GOLDEN_CARROT
    item-model: minecraft:player_head
    head-texture: "PASTE_THE_BASE64_VALUE_HERE"
    eat-seconds: 1.0
    cooldown-seconds: 30.0
    effects:
      - type: regeneration
        level: 4
        duration: 8
      - type: absorption
        level: 3
        duration: 120
```

Restart, and `/diamondhead` exists — command, permission and all.

## Built to not fall over

A bad config value never takes your server down. Anything unparseable is logged with the
exact path that caused it — `items.golden_head.effects[1]` — then skipped or replaced with
a default. One broken item doesn't stop the rest from loading.

`/rgoldenheads reload` applies changes live. Adding, removing or renaming an item needs a
restart, because commands are registered into the server's command tree at startup; the
plugin tells you in the log when that's the case.

## Commands & permissions

| Command | Permission |
|---|---|
| `/goldenhead` | `rgoldenheads.give.goldenhead` |
| `/instantgoldenhead`, `/instanthead` | `rgoldenheads.give.instantgoldenhead` |
| `/goldenapple`, `/customgapple` | `rgoldenheads.give.goldenapple` |
| `/rgoldenheads reload`, `/rgh` | `rgoldenheads.admin` |

All default to op. `rgoldenheads.give` is the parent of every per-item node — grant it to
allow every item at once.

## Requirements

- **Paper 1.21.11** (or a fork such as Purpur)
- **Java 21**
- No other dependencies. Nothing shaded. Drop the jar in `plugins/` and start.

## Links

- Source, issues and full documentation: https://github.com/ZeroArsen/rGoldenHeads
- Licensed GPL-3.0-only: https://github.com/ZeroArsen/rGoldenHeads/blob/main/LICENSE

---

*This project is Vibe Coded.*
