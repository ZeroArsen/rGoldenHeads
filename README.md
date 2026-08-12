# rGoldenHeads

Configurable custom consumables for Paper **1.21.11** — golden heads, custom golden
apples, and anything else you care to define.

Every item is a config entry. You choose its material, skin, name, lore, eating speed,
cooldown and potion effects, and the plugin registers a command for it. Nothing is
hardcoded, so you can ship a completely different set of items without touching Java.

Out of the box you get the three items below. Delete them, retune them, or add your own.

## Requirements

- Paper 1.21.11 (or a fork of it, e.g. Purpur)
- Java 21

No other dependencies, nothing shaded.

## The default items

| Command | Item | Effects | Eats in | Cooldown |
|---|---|---|---|---|
| `/goldenhead` | Golden Head — a player head that behaves as food | Regeneration III (5s), Absorption I (2min) | 1.0s | 10s |
| `/instantgoldenhead` (`/instanthead`) | Instant Golden Head | Regeneration III (10s), Absorption II (2min), Speed I (5s) | instantly | 12s |
| `/goldenapple` (`/customgapple`) | Custom Golden Apple | Regeneration II (10s), Absorption I (2min) | 1.6s | none |

All three are edible on a full hunger bar and restore no hunger or saturation.

The heads are a `GOLDEN_CARROT` re-skinned to a player head via the `item_model` and
`profile` data components, not a real `PLAYER_HEAD`. A `PLAYER_HEAD` is a block item, so
right-clicking a block would place it instead of eating it.

## Configuration

Text values accept **MiniMessage** (`<gold>Golden Head`, gradients and `<#ff8800>` hex) or
**legacy** `&` codes (`&6Golden Head`, including `&#ff8800`). A string is treated as legacy
as soon as it contains an `&` colour code; otherwise it is parsed as MiniMessage.

### Global keys

| Key | Default | Meaning |
|---|---|---|
| `config-version` | `1` | Bump-guard. Don't edit — a mismatch just logs a warning telling you to diff your file. |
| `debug` | `false` | Logs every item as it is built. Use it when an item isn't behaving as configured. |
| `drop-overflow` | `true` | Inventory full: `true` drops the item at the player's feet, `false` gives nothing and says so. |
| `messages.*` | — | Every chat string. `<item>` is replaced by the item's display name. Set one to `""` to silence it. |

### Per-item keys

Only `command` and `material` are required.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` removes the command and permission entirely. |
| `command` | — | **Required.** Command label without the slash. Must be unique. |
| `aliases` | none | Extra labels. Also must be unique. |
| `description` | `Gives you <id>` | Shown in `/help`. |
| `permission` | `rgoldenheads.give.<id>` | Node required to use the command. |
| `display-name` | the id | The item's name. |
| `lore` | none | List of lore lines. Rendered non-italic. |
| `material` | — | **Required.** Vanilla item id, e.g. `GOLDEN_CARROT`. |
| `item-model` | unset | Overrides the rendered model, e.g. `minecraft:player_head`. |
| `head-texture` | unset | Base64 skin blob — the `value` field from a head database site. |
| `head-uuid` | derived | Leave it out; a stable UUID is derived from the texture so identical items stack. |
| `glint` | vanilla | Force the enchantment shimmer on or off. |
| `amount` | `1` | How many per command, up to the material's stack size. |
| `instant` | `false` | `true` = swallowed with no eating animation at all. |
| `eat-seconds` | `1.6` | Eating time. Vanilla food is `1.6`. Ignored when `instant` is true. |
| `animation` | vanilla | `NONE`, `EAT`, `DRINK`, `BLOCK`, `BOW`, `TRIDENT`, `CROSSBOW`, `SPYGLASS`, `TOOT_HORN`, `BRUSH`, `BUNDLE`, `SPEAR`. |
| `nutrition` | `0` | Hunger points restored. |
| `saturation` | `0.0` | Saturation restored. |
| `can-always-eat` | `true` | Edible on a full hunger bar, like a golden apple. |
| `consume-particles` | vanilla | Eating particles on/off. |
| `consume-sound` | vanilla | e.g. `minecraft:entity.generic.eat`. |
| `cooldown-seconds` | none | Use-cooldown after eating. Omit the key entirely for no cooldown. |
| `cooldown-group` | `rgoldenheads:<id>` | Items sharing a group share one cooldown bar. |
| `effect-chance` | `1.0` | Probability (0.0–1.0) that the effects apply at all. |
| `effects` | none | List of `{ type, level, duration }`. |

### Effects

Effects use the numbers the game shows you, not internal ones:

```yaml
effects:
  - type: regeneration   # potion id; case and the minecraft: prefix are optional
    level: 3             # 1 = plain, 2 = II, 3 = III. Starts at 1, not 0
    duration: 10         # SECONDS. Use -1 for infinite
```

`ambient`, `particles` and `icon` are optional per effect (default `false` / `true` / `true`).

### Adding your own item

```yaml
items:
  diamond_head:
    command: diamondhead
    display-name: "<aqua>Diamond Head"
    lore:
      - "<gray>A rare drop."
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

Restart the server and `/diamondhead` exists.

### Two rules worth knowing

1. **Adding, removing or renaming an item — or changing its `command`, `aliases` or
   `permission` — needs a full restart.** Commands are registered into the server's command
   tree once, at startup. `/rgoldenheads reload` cannot add or rename them. It *can* change
   effects, names, cooldowns, materials and everything else.
2. **Items already in an inventory never change.** Every property is baked into the item
   when it is given. Reloading changes what the *next* command hands out, not the one in
   someone's hotbar.

A bad value never takes the server down. Anything the plugin can't parse is logged with the
exact config path that caused it, then skipped or replaced with a default — one broken item
doesn't stop the others from loading.

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/goldenhead` | `rgoldenheads.give.goldenhead` | Gives a Golden Head. |
| `/instantgoldenhead`, `/instanthead` | `rgoldenheads.give.instantgoldenhead` | Gives an Instant Golden Head. |
| `/goldenapple`, `/customgapple` | `rgoldenheads.give.goldenapple` | Gives a custom Golden Apple. |
| `/rgoldenheads reload` (alias `/rgh`) | `rgoldenheads.admin` | Reload the configuration live. |

All default to op. `rgoldenheads.give` is the parent of every per-item node — grant it to
allow every item at once. Per-item nodes are registered at runtime from your config, so a
permission plugin won't suggest them until the server has started once with that item
configured.

## Building

```
gradlew build
```

Output: `build/libs/rGoldenHeads-<version>.jar`. Nothing is shaded — drop it in `plugins/`.

To publish to Modrinth, set the `projectId` in `build.gradle`, export a `MODRINTH_TOKEN`,
and run `gradlew modrinth`.

## Manual test checklist

1. Fresh server with no `config.yml` → the default file is written and all three items work.
2. Each item is edible at full hunger and restores no hunger or saturation.
3. Effect levels and durations match the table above (`/data get entity @s active_effects`).
4. The instant head has no eating animation; the apple has no cooldown.
5. The two heads have separate cooldowns — eating one does not block the other.
6. Break a value on purpose (bad material, misspelled potion type, duplicate command) →
   one clear warning naming the config path, and the other items still load.
7. Change an effect and `/rgoldenheads reload` → a newly given item has the new values, the
   one already in your hotbar does not.
8. Add a new item and reload → the log tells you it needs a restart. Restart → its command exists.
9. Break the YAML and reload → failure message, and the previously loaded items still work.
10. A non-op sees none of the commands in tab-completion.

## Credits

By 0Arsen. Built on Paper's item data-component and Brigadier command APIs.

## License

MIT — see [LICENSE](LICENSE).
