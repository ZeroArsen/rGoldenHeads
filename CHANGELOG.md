# Changelog

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
