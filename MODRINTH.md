# rGoldenHeads

Golden Heads, Light Apples, enchanted apple crafting, head drops and head poles for UHC on
Paper and Folia. Compatible with MCPvP and ShieldlessUHC. Fully configurable.

The healing kit that defines UHC and tiering PvP: an edible player head that trades an
inventory slot for burst regeneration and absorption, cheap Light Apples, and the classic
crafting recipes, all in one plugin. Every value is a config entry, so you can match your
server's ruleset instead of the numbers someone else hardcoded.

**One jar for Paper 1.21.4 – 26.3, Folia included.**

## What you get

| Command | Item | Effects | Eats in | Cooldown |
|---|---|---|---|---|
| `/goldenhead` | Golden Head | Regeneration III (5s), Absorption I (2min) | 1.0s | 10s |
| `/instantgoldenhead` | Instant Golden Head | Regeneration III (10s), Absorption II (2min), Speed I (5s) | instantly | 12s |
| `/goldenapple` | Custom Golden Apple | Regeneration II (10s), Absorption I (2min) | 1.6s | none |

All three are edible on a full hunger bar and restore no hunger or saturation.

## Crafting

| Recipe | Makes |
|---|---|
| Player head surrounded by 8 gold ingots | Golden Head (or the Instant one, your choice) |
| Apple with 4 gold ingots in a plus | Light Apple: a normal golden apple for half the gold |
| Golden apple surrounded by 8 gold blocks | the vanilla Enchanted Golden Apple |

- **Optional head drops:** dying players drop their own skinned head, ready to craft. Every
  death, or PvP kills only.
- **Head poles:** or set the head on a nether brick fence where the player died, classic
  UHC style. Break it to collect the head.
- **Switch off vanilla recipes:** e.g. remove the 8-ingot golden apple so the Light Apple is
  the only way to craft one.
- **Every recipe is yours to change:** shape, ingredients, result, amount, shaped or
  shapeless, item tags, your own plugin items as ingredients, and an optional permission.
- **Recipe book support:** recipes are unlocked automatically and click-to-craft. Crafters
  work too.
- **No exploits:** a Golden Head is never mistaken for a golden carrot, and a Custom Golden
  Apple can't be turned into a Notch apple.
- **Reload live:** recipes are added, changed or removed with `/rgoldenheads reload`.

## Fully configurable items

- **Two head skins built in.** `modern` is the default, and `classic` brings back the
  original look with one line (`head-skin: classic`). You can also add your own.
- **Add your own items.** Define as many as you like; each gets its own command and permission.
- **Tune everything:** name, lore, material, stack size, eating speed, cooldown, hunger
  values, effects.
- **Readable effects.** No amplifier off-by-one, no tick math:

  ```yaml
  effects:
    - type: regeneration
      level: 3        # 1 = plain, 2 = II, 3 = III
      duration: 10    # seconds
  ```

- **MiniMessage or `&` codes**: write whichever you already use.
- **A bad value never takes the server down:** it's logged with the exact config path and
  skipped, so one broken item or recipe can't stop the rest.

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

**Upgrading?** Your existing config keeps working and nothing changes until you copy the new
`player-heads`, `recipe-book` and `recipes` sections from the default config.

Source, docs and issues: https://github.com/ZeroArsen/rGoldenHeads · GPL-3.0-only

---

*This project is Vibe Coded.*
