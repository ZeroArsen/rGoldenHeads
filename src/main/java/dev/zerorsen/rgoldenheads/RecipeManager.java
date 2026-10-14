package dev.zerorsen.rgoldenheads;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Crafter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the plugin's crafting recipes on the server: registers and swaps them on reload, decides
 * each craft's result, and unlocks them in players' recipe books.
 *
 * <p>The server matches recipes by material only, so it cannot tell a Golden Head from a plain
 * golden carrot. Every craft of one of our recipes therefore passes {@link #allows} too, and its
 * result is rebuilt from the live config rather than taken from the registered recipe.
 */
public final class RecipeManager implements Listener {

    private final Plugin plugin;
    private final ConsumableRegistry items;
    private final Settings settings;

    private volatile Map<NamespacedKey, RecipeSpec> active = Map.of();

    public RecipeManager(Plugin plugin, ConsumableRegistry items, Settings settings) {
        this.plugin = plugin;
        this.items = items;
        this.settings = settings;
    }

    /** Replaces every registered recipe with these. On Folia this happens on the global region thread. */
    public void apply(List<RecipeSpec> specs) {
        Server server = plugin.getServer();
        if (server.isGlobalTickThread()) {
            swap(specs);
        } else {
            server.getGlobalRegionScheduler().execute(plugin, () -> swap(specs));
        }
    }

    /** Removes every recipe this plugin registered. */
    public void unregisterAll() {
        Server server = plugin.getServer();
        active.keySet().forEach(key -> server.removeRecipe(key, false));
        active = Map.of();
    }

    /** The registered recipes. Public for the smoke-test harness; not a supported API. */
    public List<RecipeSpec> all() {
        return List.copyOf(active.values());
    }

    public RecipeSpec byKey(NamespacedKey key) {
        return active.get(key);
    }

    /**
     * True when every rGoldenHeads item in the grid is one the recipe asks for, in the number it
     * asks for. A slot the recipe fills with a plain vanilla item may not hold a plugin item of
     * the same material, and a slot asking for a plugin item may not hold the plain material.
     */
    public static boolean allows(RecipeSpec spec, ItemStack[] matrix) {
        Map<String, Integer> found = new HashMap<>();
        for (ItemStack item : matrix) {
            String id = ItemBuilder.idOf(item);
            if (id != null) {
                found.merge(id, 1, Integer::sum);
            }
        }
        return found.equals(spec.requiredPluginItems());
    }

    private void swap(List<RecipeSpec> specs) {
        Server server = plugin.getServer();
        active.keySet().forEach(key -> server.removeRecipe(key, false));

        Map<NamespacedKey, RecipeSpec> next = new LinkedHashMap<>();
        for (RecipeSpec spec : specs) {
            ItemStack result = spec.buildResult(items);
            if (result == null) {
                continue;
            }
            boolean added;
            try {
                added = server.addRecipe(spec.toBukkit(result), false);
            } catch (IllegalArgumentException | IllegalStateException e) {
                plugin.getLogger().warning("recipes." + spec.id() + " could not be registered: " + e.getMessage());
                continue;
            }
            if (added) {
                next.put(spec.key(), spec);
            } else {
                plugin.getLogger().warning("recipes." + spec.id() + " could not be registered; "
                        + "another recipe already uses the key " + spec.key() + ".");
            }
        }
        active = Collections.unmodifiableMap(next);
        warnAboutShadowedRecipes(server);

        if (!server.getOnlinePlayers().isEmpty()) {
            server.updateRecipes();
            for (Player player : server.getOnlinePlayers()) {
                player.getScheduler().run(plugin, task -> unlockRecipes(player), null);
            }
        }
    }

    /**
     * Crafts a sample of each recipe's own grid and checks the server picks that recipe. When it
     * doesn't, another recipe (often a vanilla one) has the same grid and always wins.
     */
    private void warnAboutShadowedRecipes(Server server) {
        List<World> worlds = server.getWorlds();
        if (worlds.isEmpty()) {
            return;
        }
        for (RecipeSpec spec : active.values()) {
            Recipe found = server.getCraftingRecipe(sampleGrid(spec), worlds.getFirst());
            NamespacedKey foundKey = found instanceof Keyed keyed ? keyed.getKey() : null;
            if (!spec.key().equals(foundKey)) {
                plugin.getLogger().warning("recipes." + spec.id() + " has the same grid as "
                        + (foundKey == null ? "another recipe" : foundKey.asString())
                        + ", which the server picks instead, so this recipe may never be craftable. "
                        + "Change its shape or ingredients.");
            }
        }
    }

    static ItemStack[] sampleGrid(RecipeSpec spec) {
        ItemStack[] grid = new ItemStack[9];
        if (spec.shapeless()) {
            List<Ingredient> slots = spec.slots();
            for (int i = 0; i < slots.size(); i++) {
                grid[i] = ItemStack.of(slots.get(i).materials().getFirst());
            }
            return grid;
        }
        List<String> shape = spec.shape();
        for (int row = 0; row < shape.size(); row++) {
            for (int column = 0; column < shape.get(row).length(); column++) {
                char symbol = shape.get(row).charAt(column);
                if (symbol != ' ') {
                    grid[row * 3 + column] = ItemStack.of(spec.shapedIngredients().get(symbol).materials().getFirst());
                }
            }
        }
        return grid;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        NamespacedKey key = ownKey(event.getRecipe());
        if (key == null) {
            return;
        }
        RecipeSpec spec = active.get(key);
        HumanEntity crafter = event.getView().getPlayer();
        boolean permitted = spec != null && (spec.permission() == null || crafter.hasPermission(spec.permission()));
        ItemStack result = permitted && allows(spec, event.getInventory().getMatrix()) ? spec.buildResult(items) : null;
        event.getInventory().setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        NamespacedKey key = ownKey(event.getRecipe());
        if (key == null) {
            return;
        }
        RecipeSpec spec = active.get(key);
        // A crafter has no player to check a permission against, so gated recipes stay hand-only
        if (spec == null || spec.permission() != null
                || !(event.getBlock().getState(false) instanceof Crafter crafter)
                || !allows(spec, crafter.getInventory().getContents())) {
            event.setCancelled(true);
            return;
        }
        ItemStack result = spec.buildResult(items);
        if (result == null) {
            event.setCancelled(true);
        } else {
            event.setResult(result);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        unlockRecipes(event.getPlayer());
    }

    /** Shows the player every recipe they may craft, and hides ours they may not. */
    private void unlockRecipes(Player player) {
        if (!settings.unlockRecipes()) {
            return;
        }
        List<NamespacedKey> allowed = new ArrayList<>();
        List<NamespacedKey> denied = new ArrayList<>();
        for (RecipeSpec spec : active.values()) {
            boolean permitted = spec.permission() == null || player.hasPermission(spec.permission());
            (permitted ? allowed : denied).add(spec.key());
        }
        player.discoverRecipes(allowed);
        player.undiscoverRecipes(denied);
    }

    private static NamespacedKey ownKey(Recipe recipe) {
        if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals(ConsumableSpec.NAMESPACE)) {
            return keyed.getKey();
        }
        return null;
    }
}
