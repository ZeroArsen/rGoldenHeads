package dev.zerorsen.rgoldenheads;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * One slot of a recipe. The server only ever matches by material; the difference between a
 * plain golden carrot and a Golden Head (also a golden carrot) is enforced by
 * {@link RecipeManager#allows} on top of that.
 */
public sealed interface Ingredient {

    String PLUGIN_PREFIX = ConsumableSpec.NAMESPACE + ":";

    /** Every material the server will accept in this slot. */
    List<Material> materials();

    default RecipeChoice choice() {
        return new RecipeChoice.MaterialChoice(materials());
    }

    /** Any one of these vanilla items. Plugin items never count, even when the material matches. */
    record Vanilla(List<Material> materials) implements Ingredient {
    }

    /** Exactly this rGoldenHeads item. */
    record PluginItem(String id, Material material) implements Ingredient {
        @Override
        public List<Material> materials() {
            return List.of(material);
        }
    }

    /**
     * Parses one ingredient value: a material, a list of materials, an item tag (#minecraft:logs)
     * or a plugin item (rgoldenheads:golden_head). Logs and returns null when unusable.
     */
    static Ingredient parse(Object raw, String path, ConsumableRegistry items, Logger log) {
        if (raw instanceof List<?> list) {
            if (list.isEmpty()) {
                log.warning(path + " is an empty list.");
                return null;
            }
            Set<Material> materials = new LinkedHashSet<>();
            for (Object entry : list) {
                String value = String.valueOf(entry).trim();
                if (value.toLowerCase(Locale.ROOT).startsWith(PLUGIN_PREFIX)) {
                    log.warning(path + " lists the plugin item '" + value + "'. A plugin item must be an "
                            + "ingredient on its own, not part of a list.");
                    return null;
                }
                Ingredient part = parse(value, path, items, log);
                if (part == null) {
                    return null;
                }
                materials.addAll(part.materials());
            }
            return new Vanilla(List.copyOf(materials));
        }

        String value = raw == null ? "" : raw.toString().trim();
        if (value.isEmpty()) {
            log.warning(path + " is empty.");
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);

        if (lower.startsWith(PLUGIN_PREFIX)) {
            String id = lower.substring(PLUGIN_PREFIX.length());
            ConsumableSpec spec = items.byId(id);
            if (spec == null) {
                log.warning(path + " refers to the item '" + id + "', which does not exist or is disabled. "
                        + "Available: " + String.join(", ", items.ids()));
                return null;
            }
            return new PluginItem(id, spec.material());
        }

        if (lower.startsWith("#")) {
            NamespacedKey key = NamespacedKey.fromString(lower.substring(1));
            Tag<Material> tag = key == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
            if (tag == null) {
                log.warning(path + " '" + value + "' is not a known item tag.");
                return null;
            }
            List<Material> materials = new ArrayList<>();
            for (Material material : tag.getValues()) {
                if (isUsableItem(material)) {
                    materials.add(material);
                }
            }
            if (materials.isEmpty()) {
                log.warning(path + " '" + value + "' is an empty item tag.");
                return null;
            }
            materials.sort(null);
            return new Vanilla(List.copyOf(materials));
        }

        Material material = itemMaterial(value);
        if (material == null) {
            log.warning(path + " '" + value + "' is not a valid item.");
            return null;
        }
        return new Vanilla(List.of(material));
    }

    /** The item material named by raw (gold_ingot, GOLD_INGOT or minecraft:gold_ingot), or null. */
    static Material itemMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(raw.trim());
        return material != null && isUsableItem(material) ? material : null;
    }

    private static boolean isUsableItem(Material material) {
        return !material.isLegacy() && !material.isAir() && material.isItem();
    }
}
