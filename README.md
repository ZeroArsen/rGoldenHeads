# rGoldenHeads

The UHC healing kit for Paper and Folia **1.21.4 – 26.3**: Golden Heads, Light Apples,
UHC-style crafting, player head drops and head poles, compatible with MCPvP and ShieldlessUHC.

Every item and every recipe is a config entry. You choose an item's material, skin, name,
lore, eating speed, cooldown and potion effects, and the plugin registers a command for it.
Recipes take any shape and ingredients you like. Nothing is hardcoded, so you can ship a
completely different set of items without touching Java.

Out of the box you get the three items and four recipes below. Delete them, retune them, or
add your own.

## Requirements

One jar runs on every version below. Every one of them is booted and checked by the
[smoke tests](#smoke-tests) before each release.

| Server | Versions |
|---|---|
| Paper (and forks such as Purpur) | 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1.1, 26.1.2, 26.2, 26.3 |
| Folia | tested on 1.21.4, 1.21.11 and 26.2 |

Use whatever Java your server version needs: 21 for 1.21.x, 25 for 26.x. The plugin
itself is built for Java 21 and runs on both. There's no 26.1 entry because Paper never
released a 26.1 build.

Servers older than 1.21.4 refuse to load the plugin with a clear message. That's the floor
because re-skinning an item as a player head only works from 1.21.4 on.

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

## Crafting

| Recipe | Grid | Result | Default |
|---|---|---|---|
| `golden_head` | a player head surrounded by 8 gold ingots | Golden Head | on |
| `instant_golden_head` | the same grid | Instant Golden Head | off |
| `light_apple` | an apple with a gold ingot on each side (4 in a plus) | a normal golden apple | on |
| `enchanted_golden_apple` | a golden apple surrounded by 8 gold blocks | the vanilla Enchanted Golden Apple | on |

The vanilla 8-ingot golden apple recipe keeps working next to the Light Apple, unless you
switch it off (see [Disabling vanilla recipes](#disabling-vanilla-recipes)). Recipes are
unlocked in players' recipe books, work in crafters, and can be added, changed or removed
with `/rgoldenheads reload`; no restart needed.

The two head recipes share a grid, and two recipes with the same grid can't both work, so
`instant_golden_head` ships disabled. To hand out Instant Golden Heads instead, disable
`golden_head` and enable `instant_golden_head`.

A Light Apple is a plain golden apple, so it stacks with the ones players already have. Give
it a `display-name` (e.g. `"<gold>Light Golden Apple"`) to tell them apart; renamed apples
then only stack with each other.

```yaml
recipes:
  golden_head:
    result: rgoldenheads:golden_head   # an item from items:, or a vanilla item like golden_apple
    amount: 1
    permission: ""                      # optional; empty = anyone may craft it
    shape:
      - "GGG"
      - "GHG"
      - "GGG"
    ingredients:
      G: gold_ingot
      H: player_head
```

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` removes the recipe. |
| `result` | — | **Required.** `rgoldenheads:<item id>` for one of your items, or a vanilla item id. |
| `amount` | `1` | How many one craft makes, up to the stack size. |
| `display-name`, `lore` | vanilla | Vanilla results only. Rename the result or add lore. |
| `permission` | none | Node needed to craft it. Players without it get no result, and the recipe is hidden from their recipe book. An undeclared node defaults to op. |
| `shape` | — | Up to 3 rows of up to 3 characters, all the same length. A space is an empty slot. |
| `ingredients` | — | What each shape character means. |
| `shapeless` | `false` | `true` = `ingredients` is a plain list and placement doesn't matter. |

An ingredient is a vanilla item (`gold_ingot`), a list meaning any one of them
(`[gold_ingot, gold_block]`), an item tag (`"#minecraft:planks"`, quoted because `#` starts a
YAML comment), or one of your items (`rgoldenheads:golden_head`).

The server matches crafting by material alone, and a Golden Head is a golden carrot
underneath. rGoldenHeads closes that gap for its own recipes:

- an `rgoldenheads:` ingredient only accepts the real item, not the plain material;
- a plain-material ingredient never accepts one of your items. A Custom Golden Apple can't
  be turned into an Enchanted Golden Apple.

Grids that collide (with each other or with a vanilla recipe) are reported in the console at
load. A recipe with a `permission` can't be crafted in a crafter, which has no player to
check it against.

### Disabling vanilla recipes

```yaml
disabled-vanilla-recipes:
  - golden_apple   # the 8-ingot golden apple; the Light Apple becomes the only way to craft one
```

Any vanilla recipe id works (the `minecraft:` prefix is optional). Remove an id and run
`/rgoldenheads reload` to bring the recipe back, and every removed recipe is restored when the
plugin is disabled. Unknown ids are reported in the console.

## Player heads

```yaml
player-heads:
  drop-on-death: false      # true = a dying player drops their own head, wearing their skin
  only-player-kills: false  # true = only when another player made the kill
  head-pole:
    enabled: false          # true = set the head on a post instead of dropping it
    pole: nether_brick_fence
```

The dropped head is a real player head named after the player, ready to craft into a Golden
Head. With `keepInventory` on, the head still drops.

With **`head-pole`** on, you get the classic UHC head pole instead: the head is placed on a
post (any block, a nether brick fence by default) where the player died, facing the way they
were looking. Breaking it drops the skinned head, ready to craft. If there's no room within a
few blocks above the death spot (e.g. the player died inside a wall), or the spot is outside
the world, the head drops as an item instead. Region protection isn't checked: like every UHC
plugin, it places the pole where the player died.

## Head skins

Heads pick a skin by name. Two are built in:

| Name | Look |
|---|---|
| `modern` | The default since 2.1 ([source](https://namemc.com/skin/7e3c84ef1342fb4d)) |
| `classic` | The look rGoldenHeads shipped with before 2.1 |

```yaml
items:
  golden_head:
    head-skin: classic   # back to the old look
```

Then run `/rgoldenheads reload`. A newly given classic head still stacks with classic heads
handed out by older versions.

**Upgrading from 2.0.x?** Your config already names a texture directly, so your heads keep
their current look. To switch to the new one, replace each head's `head-texture` and
`head-uuid` lines with `head-skin: modern`.

Add your own skins in the `head-skins:` section and use them by name:

```yaml
head-skins:
  diamond:
    texture: "PASTE_THE_BASE64_VALUE_HERE"   # the "value" field from a head database site
```

## Configuration

Text values accept **MiniMessage** (`<gold>Golden Head`, gradients and `<#ff8800>` hex) or
**legacy** `&` codes (`&6Golden Head`, including `&#ff8800`). A string is treated as legacy
as soon as it contains an `&` colour code; otherwise it is parsed as MiniMessage.

### Global keys

| Key | Default | Meaning |
|---|---|---|
| `config-version` | `2` | Bump-guard. Don't edit; a mismatch just logs a warning telling you to diff your file. |
| `debug` | `false` | Logs every item as it is built. Use it when an item isn't behaving as configured. |
| `drop-overflow` | `true` | Inventory full: `true` drops the rest at the player's feet; `false` gives only what fits and says so. |
| `update-checker.enabled` | `true` | Checks Modrinth once per startup for a newer release. See [Update checker](#update-checker). |
| `messages.*` | — | Every chat string. Placeholders like `<item>` and `<player>` are filled in. Set one to `""` to silence it; a missing one falls back to its default. |
| `head-skins.*` | none | Your own named skins, on top of the built-in `modern` and `classic`. |
| `player-heads.drop-on-death` | `false` | Dying players drop their own head. See [Player heads](#player-heads). |
| `player-heads.only-player-kills` | `false` | Limit head drops to PvP kills. |
| `player-heads.head-pole.enabled` | `false` | Set the head on a post instead of dropping it. |
| `player-heads.head-pole.pole` | `nether_brick_fence` | The block the head sits on. |
| `disabled-vanilla-recipes` | none | Vanilla recipe ids to switch off, e.g. `golden_apple`. |
| `recipe-book.unlock-on-join` | `true` | Shows the recipes a player may craft in their recipe book. |
| `recipes.*` | four recipes | See [Crafting](#crafting). |

**Upgrading from 2.1 or older?** Your config has no `player-heads`, `recipe-book` or
`recipes` section, so nothing about your server's gameplay changes: heads don't drop and no
custom recipes exist. The console reminds you on startup. Copy those sections from the
[default config](src/main/resources/config.yml), set `config-version: 2`, and run
`/rgoldenheads reload`.

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
| `head-skin` | unset | A skin name: `modern`, `classic`, or one from `head-skins`. |
| `head-texture` | unset | A raw base64 skin blob instead of a named skin. Wins over `head-skin`. |
| `head-uuid` | derived | With `head-texture`. Leave it out; a stable UUID is derived from the texture so identical items stack. |
| `glint` | vanilla | Force the enchantment shimmer on or off. |
| `max-stack-size` | material's own | How many fit in one slot, 1–99, e.g. `16` for Golden Heads that stack to 16. Items given before a change keep their old limit and won't stack with new ones. Ignored on items that take damage. |
| `amount` | `1` | How many per command, up to the stack size. |
| `instant` | `false` | `true` = swallowed with no eating animation at all. |
| `eat-seconds` | `1.6` | Eating time. Vanilla food is `1.6`. Ignored when `instant` is true. |
| `animation` | vanilla | `NONE`, `EAT`, `DRINK`, `BLOCK`, `BOW`, `TRIDENT`, `CROSSBOW`, `SPYGLASS`, `TOOT_HORN`, `BRUSH`, `BUNDLE`, `SPEAR`. `SPEAR` is the spear animation only on 1.21.11+; older servers show the trident one. |
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
    head-skin: diamond          # defined under head-skins, or use modern / classic
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
   effects, names, skins, cooldowns, materials and everything else.
2. **Items already in an inventory never change.** Every property is baked into the item
   when it is given. Reloading changes what the *next* command hands out, not the one in
   someone's hotbar.

A bad value never takes the server down. Anything the plugin can't parse is logged with the
exact config path that caused it, then skipped or replaced with a default. One broken item
doesn't stop the others from loading.

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/goldenhead` | `rgoldenheads.give.goldenhead` | Gives you a Golden Head. |
| `/instantgoldenhead`, `/instanthead` | `rgoldenheads.give.instantgoldenhead` | Gives you an Instant Golden Head. |
| `/goldenapple`, `/customgapple` | `rgoldenheads.give.goldenapple` | Gives you a custom Golden Apple. |
| `/rgoldenheads give <player\|*> <item> [amount]` | `rgoldenheads.admin.give` | Gives an item to a player, or to everyone online with `*`. Works from the console, so kit and UHC plugins can call it. |
| `/rgoldenheads reload` (alias `/rgh`) | `rgoldenheads.admin.reload` | Reloads the configuration live. |

Everything defaults to op. The permission tree:

```
rgoldenheads.give            every item command a player can use on themselves
├── rgoldenheads.give.<item> one per item, registered at runtime from your config
rgoldenheads.admin           everything below
├── rgoldenheads.admin.reload
├── rgoldenheads.admin.give
└── rgoldenheads.admin.notify   sees the update message on join
```

`rgoldenheads.admin.give` is deliberately **not** under `rgoldenheads.give`. You can hand
`rgoldenheads.give` to ordinary players without letting them give items to others.

Per-item nodes are registered at runtime, so a permission plugin won't suggest them until
the server has started once with that item configured.

## Update checker

Once per server start, rGoldenHeads asks Modrinth's public API whether a newer release
exists **for your Minecraft version and server type**, so it never points you at a build
that wouldn't run. If there is one, it logs a single console line and tells players with
`rgoldenheads.admin.notify` when they join.

The request carries nothing about your server beyond what any web request does: Modrinth
sees your server's IP address. A failed check is silent. To turn it off:

```yaml
update-checker:
  enabled: false
```

## Building

```
gradlew build
```

Output: `build/libs/rGoldenHeads-<version>.jar`. Nothing is shaded, so drop it in `plugins/`.

`gradlew modrinth` publishes the jar to Modrinth; it needs a `MODRINTH_TOKEN` environment
variable. Pushing a `v*` tag does the same through GitHub Actions.

### Smoke tests

```
gradlew smokeTest
```

Boots a real, throwaway server for every supported Paper version and the three Folia
versions, one after another. Each run loads the plugin next to a small test plugin
(`src/smokeTest`), which checks:

- every default item builds with exactly the expected data components;
- permissions and commands register, and the commands reply correctly;
- every default recipe registers, each grid crafts the expected recipe, and the vanilla
  golden apple recipe still works;
- plugin items and plain materials can't stand in for each other in a recipe;
- a dropped player head carries the player's profile;
- a head pole builds the post and a correctly facing head, breaking that head drops the
  skinned head, and a spot with no room falls back to a drop;
- disabling the vanilla golden apple recipe stops it, leaves the Light Apple working, and a
  reload brings it back;
- a deliberately broken config is contained: bad items and recipes are skipped, the good
  ones survive, and a reload leaves no stale recipes behind.

It prints a pass/fail table and fails if any version fails or if the plugin threw anywhere
in a server's log.

Before running it, know that:

- It's opt-in and local. It never runs as part of `build` or in CI.
- It downloads each server jar on the first run (cached afterwards).
- **Running these servers means agreeing to the
  [Minecraft EULA](https://aka.ms/MinecraftEULA)**, which the build does on your behalf for
  these local test servers only.

Per-version logs end up in `build/smoke/<platform>-<version>/`.

## In-game checklist

The smoke tests can't see the client or eat anything. Before a release, on at least one
1.21.x and one 26.x server, check that:

1. The modern skin renders on both heads. `head-skin: classic` plus a reload gives the old look.
2. Each item is edible at full hunger and restores no hunger or saturation.
3. Effect levels and durations match the table above (`/data get entity @s active_effects`).
4. The instant head has no eating animation, and the apple has no cooldown.
5. The two heads have separate cooldowns, so eating one doesn't block the other.
6. `/rgoldenheads give <you> golden_head 100` fills your inventory and handles the overflow.
7. Every default recipe crafts by hand, by shift-click and from the recipe book.
8. A Custom Golden Apple in the Enchanted Golden Apple grid gives no result.
9. A recipe `permission` hides the result from players without it.
10. With `drop-on-death: true`, `/kill` drops your skinned head, also with `keepInventory`;
    `only-player-kills: true` stops that.
11. Editing a recipe and running `/rgoldenheads reload` applies without a restart.
12. A crafter crafts a Light Apple.
13. With `head-pole` on, `/kill` builds a pole; breaking the head gives your skinned head, and
    dying in a 1-block gap or in water behaves sensibly.
14. `disabled-vanilla-recipes: [golden_apple]` plus a reload stops the 8-ingot craft while the
    Light Apple still works; removing it and reloading brings it back.

## Credits

By 0Arsen. Built on Paper's item data-component and Brigadier command APIs.

## License

GPL-3.0-only — see [LICENSE](LICENSE).
