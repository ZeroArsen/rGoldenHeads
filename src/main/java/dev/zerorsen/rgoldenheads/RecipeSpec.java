package dev.zerorsen.rgoldenheads;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One fully-validated crafting recipe, built by {@link RecipeLoader} from a config entry.
 * Exactly one of {@code shape}/{@code shapedIngredients} (shaped) or
 * {@code shapelessIngredients} (shapeless) is in use, and exactly one of
 * {@code resultItemId} or {@code vanillaResult} is set.
 */
public record RecipeSpec(
        String id,
        NamespacedKey key,
        List<String> shape,                             // shaped only; ' ' = empty slot
        Map<Character, Ingredient> shapedIngredients,  // shaped only
        List<Ingredient> shapelessIngredients,         // shapeless only; empty when shaped
        String resultItemId,                           // optional: an rGoldenHeads item
        ItemStack vanillaResult,                       // optional: a vanilla item, name/lore applied
        int amount,
        String permission                              // optional; null = anyone may craft it
) {

    public boolean shapeless() {
        return !shapelessIngredients.isEmpty();
    }

    /** Every ingredient, once per slot it occupies. */
    public List<Ingredient> slots() {
        if (shapeless()) {
            return shapelessIngredients;
        }
        return shape.stream()
                .flatMap(row -> row.chars().mapToObj(c -> (char) c))
                .filter(c -> c != ' ')
                .map(shapedIngredients::get)
                .toList();
    }

    /** How many of each plugin item the grid must hold, by item id. */
    public Map<String, Integer> requiredPluginItems() {
        Map<String, Integer> required = new HashMap<>();
        for (Ingredient ingredient : slots()) {
            if (ingredient instanceof Ingredient.PluginItem item) {
                required.merge(item.id(), 1, Integer::sum);
            }
        }
        return required;
    }

    /**
     * A fresh result, or null when the result item has been disabled or removed by a reload
     * since this recipe was loaded.
     */
    public ItemStack buildResult(ConsumableRegistry items) {
        ItemStack result;
        if (resultItemId != null) {
            ConsumableSpec spec = items.byId(resultItemId);
            if (spec == null) {
                return null;
            }
            result = ItemBuilder.build(spec);
        } else {
            result = vanillaResult.clone();
        }
        result.setAmount(amount);
        return result;
    }

    CraftingRecipe toBukkit(ItemStack result) {
        CraftingRecipe recipe;
        if (shapeless()) {
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, result);
            shapelessIngredients.forEach(ingredient -> shapelessRecipe.addIngredient(ingredient.choice()));
            recipe = shapelessRecipe;
        } else {
            ShapedRecipe shapedRecipe = new ShapedRecipe(key, result).shape(shape.toArray(String[]::new));
            shapedIngredients.forEach((symbol, ingredient) -> shapedRecipe.setIngredient(symbol, ingredient.choice()));
            recipe = shapedRecipe;
        }
        recipe.setCategory(CraftingBookCategory.MISC);
        return recipe;
    }
}
