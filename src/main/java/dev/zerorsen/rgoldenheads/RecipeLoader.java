package dev.zerorsen.rgoldenheads;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Parses the recipes: section into specs. Like {@link ConsumableLoader} it never throws: a bad
 * entry is logged with its config path and skipped, so one typo cannot cost the other recipes.
 */
public final class RecipeLoader {

    private static final int MAX_SIZE = 3;

    private RecipeLoader() {
    }

    public static List<RecipeSpec> load(ConfigurationSection recipes, ConsumableRegistry items,
                                        Logger log, boolean debug) {
        List<RecipeSpec> specs = new ArrayList<>();
        if (recipes == null) {
            if (debug) {
                log.info("config.yml has no 'recipes' section, so no custom recipes are registered.");
            }
            return specs;
        }

        Set<String> claimedIds = new HashSet<>();
        // Grid fingerprint -> recipe id. Two recipes with the same grid can't both work: the
        // server would always pick the same one, so the later one is refused up front.
        Map<String, String> claimedGrids = new HashMap<>();

        for (String rawId : recipes.getKeys(false)) {
            String path = "recipes." + rawId;
            ConfigurationSection section = recipes.getConfigurationSection(rawId);
            if (section == null) {
                log.warning(path + " is not a configuration block, so it was skipped.");
                continue;
            }
            if (!section.getBoolean("enabled", true)) {
                if (debug) {
                    log.info(path + " is disabled, so it was skipped.");
                }
                continue;
            }

            String id = rawId.toLowerCase(Locale.ROOT);
            if (!Key.parseableValue(id)) {
                log.warning(path + " is not a usable recipe id. "
                        + "Use only lowercase letters, digits, and . _ - / characters. Recipe skipped.");
                continue;
            }
            if (!claimedIds.add(id)) {
                log.warning(path + " duplicates the id '" + id + "'. Recipe skipped.");
                continue;
            }

            RecipeSpec spec = loadOne(id, path, section, items, log);
            if (spec == null) {
                continue;
            }

            List<String> fingerprints = fingerprints(spec);
            String owner = fingerprints.stream().map(claimedGrids::get).filter(Objects::nonNull).findFirst().orElse(null);
            if (owner != null) {
                log.warning(path + " uses the same grid as recipes." + owner
                        + ", so it was skipped. Only one of the two can be enabled at a time.");
                continue;
            }
            fingerprints.forEach(fingerprint -> claimedGrids.put(fingerprint, id));

            specs.add(spec);
            if (debug) {
                log.info("Loaded recipe '" + id + "': " + (spec.shapeless() ? "shapeless" : "shaped " + spec.shape())
                        + " -> " + spec.amount() + "x "
                        + (spec.resultItemId() != null ? Ingredient.PLUGIN_PREFIX + spec.resultItemId()
                        : spec.vanillaResult().getType().getKey().asString()));
            }
        }

        return specs;
    }

    /**
     * Parses disabled-vanilla-recipes into minecraft: keys. Whether each one is a real recipe is
     * only known when it is removed, so {@link RecipeManager} reports unknown ids.
     */
    public static List<NamespacedKey> vanillaRecipeKeys(List<String> raw, Logger log) {
        Set<NamespacedKey> keys = new LinkedHashSet<>();
        for (String entry : raw) {
            String value = entry.trim().toLowerCase(Locale.ROOT);
            NamespacedKey key = NamespacedKey.fromString(value);
            if (key == null || !key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                log.warning("disabled-vanilla-recipes: '" + entry + "' is not a vanilla recipe id "
                        + "(expected something like golden_apple), so it was ignored.");
                continue;
            }
            keys.add(key);
        }
        return List.copyOf(keys);
    }

    private static RecipeSpec loadOne(String id, String path, ConfigurationSection s,
                                      ConsumableRegistry items, Logger log) {
        String rawResult = s.getString("result", "");
        if (rawResult.isBlank()) {
            log.warning(path + " is missing the required key 'result'. Recipe skipped.");
            return null;
        }

        String resultItemId = null;
        ItemStack vanillaResult = null;
        int stackSize;
        String lowerResult = rawResult.trim().toLowerCase(Locale.ROOT);
        if (lowerResult.startsWith(Ingredient.PLUGIN_PREFIX)) {
            resultItemId = lowerResult.substring(Ingredient.PLUGIN_PREFIX.length());
            ConsumableSpec item = items.byId(resultItemId);
            if (item == null) {
                log.warning(path + ".result refers to the item '" + resultItemId + "', which does not exist "
                        + "or is disabled. Recipe skipped. Available: " + String.join(", ", items.ids()));
                return null;
            }
            stackSize = item.stackSize();
            if (s.isSet("display-name") || s.isSet("lore")) {
                log.warning(path + " sets display-name or lore, which only apply to vanilla results. "
                        + "Change them on items." + resultItemId + " instead.");
            }
        } else {
            Material resultMaterial = Ingredient.itemMaterial(rawResult);
            if (resultMaterial == null) {
                log.warning(path + ".result '" + rawResult + "' is neither a vanilla item nor "
                        + Ingredient.PLUGIN_PREFIX + "<item id>. Recipe skipped.");
                return null;
            }
            vanillaResult = vanillaResult(resultMaterial, s);
            stackSize = resultMaterial.getMaxStackSize();
        }

        int amount = s.getInt("amount", 1);
        if (amount < 1 || amount > stackSize) {
            log.warning(path + ".amount must be between 1 and " + stackSize + "; using "
                    + Math.clamp(amount, 1, stackSize) + ".");
            amount = Math.clamp(amount, 1, stackSize);
        }

        String permission = s.getString("permission", "");
        permission = permission.isBlank() ? null : permission.trim();

        NamespacedKey key = new NamespacedKey(ConsumableSpec.NAMESPACE, id);
        if (s.getBoolean("shapeless", false)) {
            List<Ingredient> ingredients = shapelessIngredients(s, path, items, log);
            if (ingredients == null) {
                return null;
            }
            return new RecipeSpec(id, key, List.of(), Map.of(), ingredients, resultItemId, vanillaResult,
                    amount, permission);
        }

        List<String> shape = shape(s, path, log);
        if (shape == null) {
            return null;
        }
        Map<Character, Ingredient> ingredients = shapedIngredients(s, shape, path, items, log);
        if (ingredients == null) {
            return null;
        }
        return new RecipeSpec(id, key, shape, ingredients, List.of(), resultItemId, vanillaResult,
                amount, permission);
    }

    private static ItemStack vanillaResult(Material material, ConfigurationSection s) {
        ItemStack result = ItemStack.of(material);
        String name = s.getString("display-name", "");
        if (!name.isBlank()) {
            result.setData(DataComponentTypes.ITEM_NAME, Text.parse(name));
        }
        List<Component> lore = s.getStringList("lore").stream().map(Text::lore).toList();
        if (!lore.isEmpty()) {
            result.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }
        return result;
    }

    private static List<String> shape(ConfigurationSection s, String path, Logger log) {
        List<String> shape = s.getStringList("shape");
        if (shape.isEmpty()) {
            log.warning(path + " needs a 'shape' (1 to 3 rows of up to 3 characters), "
                    + "or 'shapeless: true'. Recipe skipped.");
            return null;
        }
        int width = shape.getFirst().length();
        boolean valid = shape.size() <= MAX_SIZE && width >= 1 && width <= MAX_SIZE
                && shape.stream().allMatch(row -> row.length() == width);
        if (!valid) {
            log.warning(path + ".shape " + shape + " must be 1 to 3 rows, all the same length of 1 to 3 "
                    + "characters. Use spaces for empty slots, e.g. \" G \". Recipe skipped.");
            return null;
        }
        if (shape.stream().allMatch(String::isBlank)) {
            log.warning(path + ".shape is empty. Recipe skipped.");
            return null;
        }
        return List.copyOf(shape);
    }

    private static Map<Character, Ingredient> shapedIngredients(ConfigurationSection s, List<String> shape,
                                                                String path, ConsumableRegistry items, Logger log) {
        ConfigurationSection section = s.getConfigurationSection("ingredients");
        if (section == null) {
            log.warning(path + " needs an 'ingredients' block mapping each shape character to an item. "
                    + "Recipe skipped.");
            return null;
        }

        Set<Character> used = shape.stream()
                .flatMap(row -> row.chars().mapToObj(c -> (char) c))
                .filter(c -> c != ' ')
                .collect(Collectors.toCollection(TreeSet::new));

        Map<Character, Ingredient> ingredients = new LinkedHashMap<>();
        for (String symbol : section.getKeys(false)) {
            String entry = path + ".ingredients." + symbol;
            if (symbol.length() != 1 || symbol.equals(" ")) {
                // YAML reads an unquoted Y or N key as true/false, which lands here as "true"
                log.warning(entry + " must be a single character that appears in the shape. "
                        + "Quote letters like 'Y' and 'N', which YAML would otherwise read as true/false. "
                        + "Recipe skipped.");
                return null;
            }
            char c = symbol.charAt(0);
            if (!used.contains(c)) {
                log.warning(entry + " is not used in the shape, so it was ignored.");
                continue;
            }
            Ingredient ingredient = Ingredient.parse(section.get(symbol), entry, items, log);
            if (ingredient == null) {
                log.warning(path + " has an unusable ingredient, so the recipe was skipped.");
                return null;
            }
            ingredients.put(c, ingredient);
        }

        Set<Character> missing = new TreeSet<>(used);
        missing.removeAll(ingredients.keySet());
        if (!missing.isEmpty()) {
            log.warning(path + ".shape uses " + missing + " but 'ingredients' does not say what "
                    + (missing.size() == 1 ? "it is" : "they are") + ". Recipe skipped.");
            return null;
        }
        return Collections.unmodifiableMap(ingredients);
    }

    private static List<Ingredient> shapelessIngredients(ConfigurationSection s, String path,
                                                         ConsumableRegistry items, Logger log) {
        List<?> raw = s.getList("ingredients");
        if (raw == null || raw.isEmpty() || raw.size() > MAX_SIZE * MAX_SIZE) {
            log.warning(path + ".ingredients must be a list of 1 to 9 items for a shapeless recipe. "
                    + "Recipe skipped.");
            return null;
        }
        List<Ingredient> ingredients = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Ingredient ingredient = Ingredient.parse(raw.get(i), path + ".ingredients[" + i + "]", items, log);
            if (ingredient == null) {
                log.warning(path + " has an unusable ingredient, so the recipe was skipped.");
                return null;
            }
            ingredients.add(ingredient);
        }
        return List.copyOf(ingredients);
    }

    /**
     * What the server sees when it matches this recipe: materials only, empty border rows and
     * columns removed (vanilla does the same). A shaped grid also matches mirrored, so both
     * orientations are returned.
     */
    static List<String> fingerprints(RecipeSpec spec) {
        if (spec.shapeless()) {
            return List.of("shapeless:" + spec.slots().stream().map(RecipeLoader::cell).sorted()
                    .collect(Collectors.joining(";")));
        }

        List<List<String>> grid = new ArrayList<>();
        for (String row : spec.shape()) {
            List<String> cells = new ArrayList<>();
            for (char c : row.toCharArray()) {
                cells.add(c == ' ' ? "" : cell(spec.shapedIngredients().get(c)));
            }
            grid.add(cells);
        }
        grid = trim(grid);

        List<List<String>> mirrored = grid.stream().map(row -> {
            List<String> copy = new ArrayList<>(row);
            Collections.reverse(copy);
            return copy;
        }).toList();
        return List.of(render(grid), render(mirrored));
    }

    private static String cell(Ingredient ingredient) {
        return ingredient.materials().stream().map(Material::name).sorted().collect(Collectors.joining(","));
    }

    private static List<List<String>> trim(List<List<String>> grid) {
        int top = 0;
        int bottom = grid.size() - 1;
        while (top <= bottom && grid.get(top).stream().allMatch(String::isEmpty)) {
            top++;
        }
        while (bottom >= top && grid.get(bottom).stream().allMatch(String::isEmpty)) {
            bottom--;
        }
        List<List<String>> rows = grid.subList(top, bottom + 1);

        int width = rows.getFirst().size();
        int left = 0;
        int right = width - 1;
        while (left <= right && columnEmpty(rows, left)) {
            left++;
        }
        while (right >= left && columnEmpty(rows, right)) {
            right--;
        }
        int from = left;
        int to = right + 1;
        return rows.stream().map(row -> row.subList(from, to)).toList();
    }

    private static boolean columnEmpty(List<List<String>> rows, int column) {
        return rows.stream().allMatch(row -> row.get(column).isEmpty());
    }

    private static String render(List<List<String>> grid) {
        return grid.stream().map(row -> String.join("|", row)).collect(Collectors.joining("/"));
    }
}
