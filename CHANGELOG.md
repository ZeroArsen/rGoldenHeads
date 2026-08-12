# Changelog

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
