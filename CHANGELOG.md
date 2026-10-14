# Changelog

## 2.3.0

### Added
- **Head poles** (`player-heads.head-pole`): instead of dropping, a dead player's head is set
  on a post where they died, the classic UHC look. Breaking it drops the skinned head, ready
  to craft. The post is any block (nether brick fence by default). When there's no room, the
  head drops as an item instead.
- **`disabled-vanilla-recipes`**: switch off vanilla recipes by id. For example, `golden_apple`
  removes the 8-ingot golden apple so the Light Apple is the only way to craft one. Removing
  an id and reloading brings the recipe back.

Both are off by default, so existing configs behave exactly as before.

## 2.2.0

### Added
- **Crafting.** Four recipes ship in the new `recipes:` section:
  - **Golden Head**: a player head surrounded by 8 gold ingots.
  - **Instant Golden Head**: the same grid, off by default; enable it instead of the Golden
    Head recipe if you prefer instant heads.
  - **Light Apple**: an apple with 4 gold ingots in a plus makes a normal golden apple.
    Optionally renamed, e.g. "Light Golden Apple".
  - **Enchanted Golden Apple**: a golden apple surrounded by 8 gold blocks makes the vanilla
    Notch apple.
- **Every recipe is configurable:** result (your items or any vanilla item), amount, shape,
  shaped or shapeless, ingredient lists, item tags, your own items as ingredients, and an
  optional permission. Recipes reload live with `/rgoldenheads reload`.
- **Recipe book:** recipes are unlocked on join and are click-to-craft (`recipe-book.unlock-on-join`).
  Crafters work too.
- **Player head drops** (`player-heads.drop-on-death`, off by default): a dying player drops
  their own skinned head. `only-player-kills` limits it to PvP kills.
- Recipe config is validated like items: duplicate grids, collisions with vanilla recipes,
  unknown ingredients and unmapped shape characters are logged with their config path.

### Changed
- Your items can't be mistaken for their plain material in rGoldenHeads recipes: a Custom
  Golden Apple is not a golden apple and a Golden Head is not a golden carrot.
- `config-version` is now `2`. Existing configs keep working unchanged: until you copy the new
  `player-heads`, `recipe-book` and `recipes` sections from the default config, heads don't
  drop and no recipes are added. The startup warning tells you so.

## 2.1.0

### Added
- **One jar for Paper 1.21.4 – 26.3 and Folia.** Every supported version is booted and
  checked by an automated smoke test before release. Servers older than 1.21.4 now refuse to
  load the plugin with a clear message instead of failing halfway through.
- **New default Golden Head skin.** Heads now pick a skin by name: `head-skin: modern` (the
  new default) or `head-skin: classic` (the original look). Add your own under `head-skins:`.
  Existing configs keep their current look; switch with one line.
- **`/rgoldenheads give <player|*> <item> [amount]`**, usable from the console so kit and
  UHC plugins can hand out items. New permission `rgoldenheads.admin.give`, deliberately
  kept out of `rgoldenheads.give`.
- **Update checker.** Asks Modrinth once per startup for a newer release that matches your
  server version, and tells admins (`rgoldenheads.admin.notify`) on join. Disable with
  `update-checker.enabled: false`.
- `rgoldenheads.admin` is now a parent node of `.admin.reload`, `.admin.give` and
  `.admin.notify`, so existing admins keep reload and gain the rest.

### Changed
- `animation: TRIDENT` works on every version. Before 1.21.11 the trident animation was
  called `SPEAR`, and the plugin now maps it for you.
- Giving items is Folia-safe: inventory changes run on the receiving player's own thread.

### Fixed
- Startup no longer logs "Item '...' was added, but its command will not exist until the
  server restarts" for every item. That line now only appears for items actually added by
  a reload.
- The `drop-overflow: false` description now matches what happens: the items that fit
  are given, and the rest is discarded.

## 2.0.1

### Changed
- **Relicensed from MIT to GPL-3.0-only.** Copies obtained under 2.0.0 remain MIT-licensed;
  that grant cannot be withdrawn retroactively.
- The Modrinth page body now comes from `MODRINTH.md` rather than `README.md`, so the
  storefront page and the repo readme can say different things.

No functional changes to the plugin.

## 2.0.0

First public release.

### Added
- **`config.yml`** — every item is now a config entry. Material, skin texture, display
  name, lore, glint, amount, eating speed, animation, nutrition, saturation, cooldown and
  potion effects are all configurable, and you can define entirely new items.
- Effects use human numbers: `level: 3` and `duration: 10` (seconds), not amplifier 2 and
  200 ticks.
- Text accepts **MiniMessage** (gradients, hex) or legacy `&` codes, auto-detected per string.
- Configurable `messages:` section for every chat string.
- `/rgoldenheads reload` (alias `/rgh`, permission `rgoldenheads.admin`).
- `drop-overflow` option controlling what happens when the player's inventory is full.
- Config validation: bad materials, unknown potion types, duplicate commands, malformed
  base64 textures and the rest are logged with the exact config path and skipped, instead
  of taking the plugin down.

### Changed
- **Breaking:** item commands are no longer declared in `plugin.yml`; they are registered
  from config into Paper's Brigadier command tree at startup. Per-item permissions are
  registered at runtime as children of `rgoldenheads.give`.
- Adding, removing or renaming an item now requires a server restart. All other config
  changes apply on reload.

The three built-in items are unchanged from 1.2.0.

## 1.2.0

### Added
- **Instant Golden Head** (`/instantgoldenhead`, `/instanthead`) — eaten instantly, 12s
  cooldown, Regeneration III (10s), Absorption II (2min), Speed I (5s).
- **Custom Golden Apple** (`/goldenapple`, `/customgapple`) — Regeneration II (10s),
  Absorption I (2min), no cooldown, eats at the vanilla 1.6s.

### Changed
- Per-item permission nodes under the `rgoldenheads.give` parent.

## 1.1.0

- Golden Head (`/goldenhead`): a player-head-skinned edible item granting Regeneration III
  (5s) and Absorption I (2min) on a 10s cooldown.
