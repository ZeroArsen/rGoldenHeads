package dev.zerorsen.rgoldenheads;

import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Parses the items: section into specs. Never throws and never disables the plugin:
 * every bad value is logged with the config path that caused it, then skipped or
 * replaced with a default so one broken entry cannot take the server down.
 */
public final class ConsumableLoader {

    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 256;
    private static final float DEFAULT_EAT_SECONDS = 1.6f;
    private static final Pattern VALID_LABEL = Pattern.compile("[a-z0-9_-]+");

    // Labels the plugin's own admin command owns; an item may not steal them.
    private static final Set<String> RESERVED_LABELS = Set.of("rgoldenheads", "rgh");

    // Bukkit renamed a batch of potion types in 1.20.5. Accept the old spellings so configs
    // copied from older plugins keep working instead of silently losing effects.
    private static final Map<String, String> LEGACY_EFFECT_NAMES = Map.ofEntries(
            Map.entry("increase_damage", "strength"),
            Map.entry("damage_resistance", "resistance"),
            Map.entry("jump", "jump_boost"),
            Map.entry("slow", "slowness"),
            Map.entry("fast_digging", "haste"),
            Map.entry("slow_digging", "mining_fatigue"),
            Map.entry("heal", "instant_health"),
            Map.entry("harm", "instant_damage"),
            Map.entry("confusion", "nausea"));

    private ConsumableLoader() {
    }

    public static List<ConsumableSpec> load(ConfigurationSection items, Map<String, HeadSkins.HeadSkin> skins,
                                            Logger log, boolean debug) {
        List<ConsumableSpec> specs = new ArrayList<>();
        if (items == null) {
            log.warning("config.yml has no 'items' section, so no items are available. "
                    + "Compare your file against the bundled default.");
            return specs;
        }

        Set<String> claimedLabels = new HashSet<>(RESERVED_LABELS);
        Set<String> claimedIds = new HashSet<>();

        for (String rawId : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(rawId);
            if (section == null) {
                log.warning("items." + rawId + " is not a configuration block, so it was skipped.");
                continue;
            }
            if (!section.getBoolean("enabled", true)) {
                if (debug) {
                    log.info("items." + rawId + " is disabled, so it was skipped.");
                }
                continue;
            }

            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.equals(rawId)) {
                log.warning("items." + rawId + " has uppercase characters in its key; using '" + id + "'.");
            }
            if (!Key.parseableValue(id)) {
                log.warning("items." + rawId + " is not a usable item id. "
                        + "Use only lowercase letters, digits, and . _ - / characters. Item skipped.");
                continue;
            }
            if (!claimedIds.add(id)) {
                log.warning("items." + rawId + " duplicates the id '" + id + "'. Item skipped.");
                continue;
            }

            ConsumableSpec spec = loadOne(id, section, claimedLabels, skins, log);
            if (spec != null) {
                specs.add(spec);
                if (debug) {
                    log.info("Loaded item '" + id + "': command=/" + spec.commandName()
                            + " material=" + spec.material()
                            + " eat=" + spec.consumeSeconds() + "s"
                            + " cooldown=" + spec.cooldownSeconds()
                            + " effects=" + spec.effects().size());
                }
            }
        }

        return specs;
    }

    private static ConsumableSpec loadOne(String id, ConfigurationSection s, Set<String> claimedLabels,
                                          Map<String, HeadSkins.HeadSkin> skins, Logger log) {
        String path = "items." + id;

        String command = s.getString("command", "");
        if (command.isBlank()) {
            log.warning(path + " is missing the required key 'command'. Item skipped.");
            return null;
        }
        command = command.toLowerCase(Locale.ROOT);
        if (!VALID_LABEL.matcher(command).matches()) {
            log.warning(path + ".command '" + command
                    + "' is not a usable command name. Use only lowercase letters, digits, _ and -. Item skipped.");
            return null;
        }
        if (!claimedLabels.add(command)) {
            log.warning(path + ".command '" + command
                    + "' is already taken by another item or reserved by the plugin. Item skipped.");
            return null;
        }

        Material material = material(s.getString("material"), path, log);
        if (material == null) {
            return null;
        }

        List<String> aliases = new ArrayList<>();
        for (String rawAlias : s.getStringList("aliases")) {
            String alias = rawAlias.toLowerCase(Locale.ROOT);
            if (!VALID_LABEL.matcher(alias).matches()) {
                log.warning(path + " has an unusable alias '" + rawAlias + "', which was dropped.");
                continue;
            }
            if (!claimedLabels.add(alias)) {
                log.warning(path + " has the alias '" + alias
                        + "' which is already taken, so it was dropped. The item itself still works.");
                continue;
            }
            aliases.add(alias);
        }

        Component displayName = Text.parse(s.getString("display-name", id));
        List<Component> lore = new ArrayList<>();
        for (String line : s.getStringList("lore")) {
            lore.add(Text.lore(line));
        }

        String description = s.getString("description", "Gives you " + id);
        String permission = s.getString("permission", "rgoldenheads.give." + id);

        Key itemModel = optionalKey(s.getString("item-model"), path, "item-model", log);

        HeadSkins.HeadSkin skin = headSkin(s, skins, path, log);
        if (skin != null && (itemModel == null || !"minecraft:player_head".equals(itemModel.asString()))) {
            log.warning(path + " has a head skin but item-model is not minecraft:player_head, "
                    + "so the skin will not be visible.");
        }
        String skinTexture = skin == null ? null : skin.texture();
        UUID skinUuid = skin == null ? null : skin.uuid();

        Boolean glint = s.isSet("glint") ? s.getBoolean("glint") : null;

        int amount = s.getInt("amount", 1);
        int maxAmount = material.getMaxStackSize();
        if (amount < 1 || amount > maxAmount) {
            log.warning(path + ".amount must be between 1 and " + maxAmount + " for " + material
                    + "; using " + Math.clamp(amount, 1, maxAmount) + ".");
            amount = Math.clamp(amount, 1, maxAmount);
        }

        // instant: true is shorthand for "no eating delay and no animation at all"
        boolean instant = s.getBoolean("instant", false);
        float consumeSeconds = instant ? 0.0f : (float) s.getDouble("eat-seconds", DEFAULT_EAT_SECONDS);
        if (!Float.isFinite(consumeSeconds) || consumeSeconds < 0.0f) {
            log.warning(path + ".eat-seconds must be zero or more; using 0.");
            consumeSeconds = 0.0f;
        }

        ItemUseAnimation animation = instant ? ItemUseAnimation.NONE : animation(s.getString("animation"), path, log);

        Boolean consumeParticles = s.isSet("consume-particles") ? s.getBoolean("consume-particles") : null;
        Key consumeSound = optionalKey(s.getString("consume-sound"), path, "consume-sound", log);

        int nutrition = s.getInt("nutrition", 0);
        if (nutrition < 0) {
            log.warning(path + ".nutrition cannot be negative; using 0.");
            nutrition = 0;
        }
        float saturation = (float) s.getDouble("saturation", 0.0d);
        if (!Float.isFinite(saturation) || saturation < 0.0f) {
            log.warning(path + ".saturation cannot be negative; using 0.");
            saturation = 0.0f;
        }
        boolean canAlwaysEat = s.getBoolean("can-always-eat", true);

        Float cooldownSeconds = null;
        if (s.isSet("cooldown-seconds")) {
            float cooldown = (float) s.getDouble("cooldown-seconds");
            if (!Float.isFinite(cooldown) || cooldown <= 0.0f) {
                log.warning(path + ".cooldown-seconds must be greater than 0; this item now has no cooldown.");
            } else {
                cooldownSeconds = cooldown;
            }
        }

        Key cooldownGroup = optionalKey(s.getString("cooldown-group"), path, "cooldown-group", log);
        if (cooldownGroup == null) {
            cooldownGroup = Key.key(ConsumableSpec.NAMESPACE, id);
        }

        float effectChance = (float) s.getDouble("effect-chance", 1.0d);
        if (!Float.isFinite(effectChance) || effectChance < 0.0f || effectChance > 1.0f) {
            log.warning(path + ".effect-chance must be between 0.0 and 1.0; using 1.0.");
            effectChance = 1.0f;
        }

        List<PotionEffect> effects = effects(s, path, log);

        return new ConsumableSpec(id, command, List.copyOf(aliases), description, permission,
                displayName, List.copyOf(lore), material, itemModel, skinUuid, skinTexture, glint,
                amount, consumeSeconds, animation, consumeParticles, consumeSound,
                nutrition, saturation, canAlwaysEat, cooldownSeconds, cooldownGroup,
                effectChance, List.copyOf(effects));
    }

    private static Material material(String raw, String path, Logger log) {
        if (raw == null || raw.isBlank()) {
            log.warning(path + " is missing the required key 'material'. Item skipped.");
            return null;
        }
        Material material = Material.matchMaterial(raw);
        if (material == null || material.isLegacy() || material.isAir() || !material.isItem()) {
            log.warning(path + ".material '" + raw + "' is not a valid item material. Item skipped.");
            return null;
        }
        return material;
    }

    /**
     * Resolves the item's skin, or null for none. An explicit head-texture wins over a named
     * head-skin, which keeps every 2.0.x config (written with raw textures) working unchanged.
     */
    private static HeadSkins.HeadSkin headSkin(ConfigurationSection s, Map<String, HeadSkins.HeadSkin> skins,
                                               String path, Logger log) {
        String texture = s.getString("head-texture");
        String skinName = s.getString("head-skin");

        if (texture != null && !texture.isBlank()) {
            if (skinName != null && !skinName.isBlank()) {
                log.warning(path + " sets both head-texture and head-skin; head-texture is used.");
            }
            if (!HeadSkins.isUsableTexture(texture, path + ".head-texture", log)) {
                return null;
            }
            return new HeadSkins.HeadSkin(texture,
                    HeadSkins.uuid(s.getString("head-uuid"), texture, path + ".head-uuid", log));
        }

        if (skinName == null || skinName.isBlank()) {
            return null;
        }
        HeadSkins.HeadSkin skin = skins.get(skinName.toLowerCase(Locale.ROOT));
        if (skin == null) {
            log.warning(path + ".head-skin '" + skinName + "' is not a known skin, so this item has no skin. "
                    + "Available: " + String.join(", ", skins.keySet()));
        }
        return skin;
    }

    private static Key optionalKey(String raw, String path, String key, Logger log) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (!Key.parseable(raw)) {
            log.warning(path + "." + key + " '" + raw + "' is not a valid key "
                    + "(expected something like minecraft:player_head), so it was ignored.");
            return null;
        }
        return Key.key(raw);
    }

    private static ItemUseAnimation animation(String raw, String path, Logger log) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.toUpperCase(Locale.ROOT);
        try {
            return ItemUseAnimation.valueOf(name);
        } catch (IllegalArgumentException e) {
            // 1.21.11 renamed the trident animation from SPEAR to TRIDENT. Servers before that
            // have no TRIDENT, but their SPEAR *is* the trident animation, so TRIDENT behaves
            // the same on every supported version.
            if (name.equals("TRIDENT")) {
                try {
                    return ItemUseAnimation.valueOf("SPEAR");
                } catch (IllegalArgumentException ignored) {
                    // fall through to the warning
                }
            }
            log.warning(path + ".animation '" + raw + "' is not available on this server, so the vanilla "
                    + "default is used. Valid values here: " + Arrays.toString(ItemUseAnimation.values()));
            return null;
        }
    }

    private static List<PotionEffect> effects(ConfigurationSection s, String path, Logger log) {
        List<PotionEffect> effects = new ArrayList<>();
        if (!s.isSet("effects")) {
            return effects;
        }

        List<Map<?, ?>> raw = s.getMapList("effects");
        if (raw.isEmpty() && !s.getList("effects", List.of()).isEmpty()) {
            log.warning(path + ".effects must be a list of '- type: ... / level: ... / duration: ...' "
                    + "blocks, so it was ignored.");
            return effects;
        }

        for (int i = 0; i < raw.size(); i++) {
            String entry = path + ".effects[" + i + "]";
            Map<?, ?> map = raw.get(i);

            Object rawType = map.get("type");
            if (rawType == null) {
                log.warning(entry + " is missing 'type', so it was skipped.");
                continue;
            }
            PotionEffectType type = effectType(rawType.toString());
            if (type == null) {
                log.warning(entry + " has unknown potion type '" + rawType
                        + "', so it was skipped. Use ids like regeneration, absorption or speed.");
                continue;
            }

            int level = intOf(map.get("level"), 1);
            if (level < MIN_LEVEL) {
                log.warning(entry + ".level starts at 1, not 0, so level 1 was used.");
                level = MIN_LEVEL;
            } else if (level > MAX_LEVEL) {
                log.warning(entry + ".level is capped at " + MAX_LEVEL + ".");
                level = MAX_LEVEL;
            }

            int durationSeconds = intOf(map.get("duration"), 0);
            int durationTicks;
            if (durationSeconds == PotionEffect.INFINITE_DURATION) {
                durationTicks = PotionEffect.INFINITE_DURATION;
            } else if (durationSeconds <= 0) {
                log.warning(entry + ".duration must be greater than 0 seconds (or -1 for infinite), "
                        + "so it was skipped.");
                continue;
            } else {
                long ticks = (long) durationSeconds * 20L;
                if (ticks > Integer.MAX_VALUE) {
                    log.warning(entry + ".duration is too large; capping it.");
                    ticks = Integer.MAX_VALUE;
                }
                durationTicks = (int) ticks;
            }

            boolean ambient = boolOf(map.get("ambient"), false);
            boolean particles = boolOf(map.get("particles"), true);
            boolean icon = boolOf(map.get("icon"), true);

            effects.add(new PotionEffect(type, durationTicks, level - 1, ambient, particles, icon));
        }

        return effects;
    }

    private static PotionEffectType effectType(String raw) {
        String name = raw.trim().toLowerCase(Locale.ROOT);
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }
        name = LEGACY_EFFECT_NAMES.getOrDefault(name, name);

        NamespacedKey key = NamespacedKey.fromString(name);
        return key == null ? null : Registry.MOB_EFFECT.get(key);
    }

    private static int intOf(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean boolOf(Object value, boolean fallback) {
        return value instanceof Boolean bool ? bool : fallback;
    }
}
