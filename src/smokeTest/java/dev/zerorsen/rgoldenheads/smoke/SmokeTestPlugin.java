package dev.zerorsen.rgoldenheads.smoke;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.zerorsen.rgoldenheads.ConsumableRegistry;
import dev.zerorsen.rgoldenheads.ConsumableSpec;
import dev.zerorsen.rgoldenheads.HeadPole;
import dev.zerorsen.rgoldenheads.ItemBuilder;
import dev.zerorsen.rgoldenheads.PlayerHeadDrops;
import dev.zerorsen.rgoldenheads.RecipeManager;
import dev.zerorsen.rgoldenheads.RecipeSpec;
import dev.zerorsen.rgoldenheads.RGoldenHeadsPlugin;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.Recipe;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Throwaway test plugin, never shipped. Booted next to rGoldenHeads on every supported server
 * version by `gradlew smokeTest`: it checks the plugin loads, builds every item with the
 * expected data components, registers its permissions and commands, and survives a broken
 * config. It then writes smoke-result.txt and stops the server.
 */
public final class SmokeTestPlugin extends JavaPlugin implements Listener {

    private static final UUID MODERN_SKIN = UUID.fromString("38903a1f-09fb-5757-966e-c748605e243a");
    private static final UUID CLASSIC_SKIN = UUID.fromString("befd0365-bdc7-5023-9013-a57583aef725");
    private static final String CLASSIC_TEXTURE =
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmZkNTA2MDI2MTFl"
                    + "OGM0ZjkwY2UxYWRhNDhlNjMyY2UyYTFkNTVlZDg0YTRiNWNkYTBhZGJiY2Q5MGY1NTU1OSJ9fX0=";
    private static final Set<String> DEFAULT_IDS = Set.of("golden_head", "instant_golden_head", "custom_golden_apple");
    private static final Set<String> DEFAULT_RECIPES = Set.of("golden_head", "light_apple", "enchanted_golden_apple");
    private static final Map<Character, Material> LEGEND = Map.of('G', Material.GOLD_INGOT, 'H', Material.PLAYER_HEAD,
            'A', Material.APPLE, 'B', Material.GOLD_BLOCK, 'P', Material.GOLDEN_APPLE);

    private final List<String> lines = new ArrayList<>();
    private boolean failed;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        // A second after startup, so the command tree built in the COMMANDS lifecycle event is live.
        // The global region scheduler is the one scheduler that exists on both Paper and Folia.
        getServer().getGlobalRegionScheduler().runDelayed(this, task -> runAndShutdown(), 20L);
    }

    private void runAndShutdown() {
        guarded(this::runChecks);
        // Block edits belong to the region that owns the blocks (on Folia a different thread),
        // so the head pole check runs there and finishes the run afterwards.
        World world = getServer().getWorlds().getFirst();
        getServer().getRegionScheduler().run(this, world.getSpawnLocation(), task -> {
            guarded(() -> checkHeadPole(world));
            writeResult();
            getServer().shutdown();
        });
    }

    private interface Check {
        void run() throws Exception;
    }

    private void guarded(Check check) {
        try {
            check.run();
        } catch (Throwable t) {
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            fail("unexpected exception: " + trace);
        }
    }

    private void runChecks() throws Exception {
        info("server: " + getServer().getName() + " " + getServer().getVersion());

        Plugin raw = getServer().getPluginManager().getPlugin("rGoldenHeads");
        if (!(raw instanceof RGoldenHeadsPlugin plugin) || !raw.isEnabled()) {
            fail("rGoldenHeads is not enabled");
            return;
        }
        ConsumableRegistry registry = plugin.registry();
        expect(registry.ids().equals(DEFAULT_IDS), "default items loaded: " + registry.ids());

        checkItem(registry.byId("golden_head"), 1.0f, null, 10.0f, true,
                List.of("regeneration:2:100", "absorption:0:2400"));
        checkItem(registry.byId("instant_golden_head"), 0.0f, "NONE", 12.0f, true,
                List.of("regeneration:2:200", "absorption:1:2400", "speed:0:100"));
        checkItem(registry.byId("custom_golden_apple"), 1.6f, null, null, false,
                List.of("regeneration:1:200", "absorption:0:2400"));

        checkPermissions();
        checkCommands();
        checkRecipes(plugin);
        checkPlayerHead();
        checkValidation(plugin);
    }

    private void checkItem(ConsumableSpec spec, float eatSeconds, String animation, Float cooldown,
                           boolean head, List<String> effects) {
        if (spec == null) {
            fail("item missing from registry");
            return;
        }
        String id = spec.id();
        ItemStack item = ItemBuilder.build(spec);

        expect(id.equals(ItemBuilder.idOf(item)), id + ": PDC id written");
        expect(item.hasData(DataComponentTypes.FOOD), id + ": FOOD component set");

        Consumable consumable = item.getData(DataComponentTypes.CONSUMABLE);
        expect(consumable != null && consumable.consumeSeconds() == eatSeconds,
                id + ": eats in " + eatSeconds + "s (got " + (consumable == null ? "no CONSUMABLE" : consumable.consumeSeconds()) + ")");
        if (animation != null) {
            ItemUseAnimation actual = consumable == null ? null : consumable.animation();
            expect(actual != null && actual.name().equals(animation), id + ": animation " + animation + " (got " + actual + ")");
        }
        List<String> actualEffects = consumable == null ? List.of() : effectSummary(consumable);
        expect(actualEffects.equals(effects), id + ": effects " + effects + " (got " + actualEffects + ")");

        UseCooldown useCooldown = item.getData(DataComponentTypes.USE_COOLDOWN);
        if (cooldown == null) {
            expect(useCooldown == null, id + ": no USE_COOLDOWN component");
        } else {
            expect(useCooldown != null && useCooldown.seconds() == cooldown
                            && useCooldown.cooldownGroup().equals(Key.key("rgoldenheads", id)),
                    id + ": " + cooldown + "s cooldown in group rgoldenheads:" + id);
        }

        ResolvableProfile profile = item.getData(DataComponentTypes.PROFILE);
        if (head) {
            expect(profile != null && MODERN_SKIN.equals(profile.uuid()), id + ": modern skin profile");
            expect(Key.key("minecraft", "player_head").equals(item.getData(DataComponentTypes.ITEM_MODEL)),
                    id + ": player_head item model");
        } else {
            expect(profile == null, id + ": no skin profile");
        }
    }

    private static List<String> effectSummary(Consumable consumable) {
        List<String> summary = new ArrayList<>();
        for (ConsumeEffect effect : consumable.consumeEffects()) {
            if (effect instanceof ConsumeEffect.ApplyStatusEffects apply) {
                for (PotionEffect potion : apply.effects()) {
                    summary.add(potion.getType().key().value() + ":" + potion.getAmplifier() + ":" + potion.getDuration());
                }
            }
        }
        return summary;
    }

    private void checkPermissions() {
        PluginManager pluginManager = getServer().getPluginManager();
        Permission give = pluginManager.getPermission("rgoldenheads.give");
        for (String node : List.of("goldenhead", "instantgoldenhead", "goldenapple")) {
            String full = "rgoldenheads.give." + node;
            expect(pluginManager.getPermission(full) != null && give != null && give.getChildren().containsKey(full),
                    full + " registered under rgoldenheads.give");
        }
        Permission admin = pluginManager.getPermission("rgoldenheads.admin");
        expect(admin != null && admin.getChildren().keySet().containsAll(
                        Set.of("rgoldenheads.admin.reload", "rgoldenheads.admin.give", "rgoldenheads.admin.notify")),
                "rgoldenheads.admin has reload/give/notify children");
        expect(give != null && !give.getChildren().containsKey("rgoldenheads.admin.give"),
                "admin.give is NOT a child of rgoldenheads.give");
    }

    private void checkCommands() {
        List<Component> replies = new ArrayList<>();
        CommandSender sender = getServer().createCommandSender(replies::add);

        expectReply(sender, replies, "goldenhead", "Only players");
        expectReply(sender, replies, "instanthead", "Only players");
        expectReply(sender, replies, "goldenapple", "Only players");
        expectReply(sender, replies, "rgh", "Usage");
        expectReply(sender, replies, "rgoldenheads give Nobody golden_head", "No online player");
        expectReply(sender, replies, "rgoldenheads give * golden_head", "Nobody is online");
        expectReply(sender, replies, "rgoldenheads give Nobody not_an_item", "Unknown item");
        expectReply(sender, replies, "rgoldenheads give Nobody golden_head 0", "amount must be");
        expectReply(sender, replies, "rgoldenheads reload", "reloaded");
    }

    private void expectReply(CommandSender sender, List<Component> replies, String command, String needle) {
        replies.clear();
        getServer().dispatchCommand(sender, command);
        String text = replies.stream()
                .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                .collect(Collectors.joining(" | "));
        expect(text.contains(needle), "/" + command + " -> \"" + text + "\"");
    }

    private void checkRecipes(RGoldenHeadsPlugin plugin) {
        RecipeManager recipes = plugin.recipes();
        expect(recipeIds(plugin).equals(DEFAULT_RECIPES), "default recipes registered: " + recipeIds(plugin));
        for (String id : DEFAULT_RECIPES) {
            expect(getServer().getRecipe(key(id)) != null, "server knows rgoldenheads:" + id);
        }
        expect(getServer().getRecipe(key("instant_golden_head")) == null, "disabled instant_golden_head recipe not registered");

        expectCrafts(grid(LEGEND, "GGG", "GHG", "GGG"), "rgoldenheads:golden_head");
        expectCrafts(grid(LEGEND, " G ", "GAG", " G "), "rgoldenheads:light_apple");
        expectCrafts(grid(LEGEND, "BBB", "BPB", "BBB"), "rgoldenheads:enchanted_golden_apple");
        expectCrafts(grid(LEGEND, "GGG", "GAG", "GGG"), "minecraft:golden_apple");

        ConsumableRegistry registry = plugin.registry();
        RecipeSpec head = recipes.byKey(key("golden_head"));
        expect(head != null && "golden_head".equals(ItemBuilder.idOf(head.buildResult(registry))),
                "golden_head recipe makes a real Golden Head");
        RecipeSpec light = recipes.byKey(key("light_apple"));
        ItemStack lightResult = light == null ? null : light.buildResult(registry);
        expect(lightResult != null && lightResult.isSimilar(ItemStack.of(Material.GOLDEN_APPLE)),
                "light_apple makes a plain vanilla golden apple");
        RecipeSpec enchanted = recipes.byKey(key("enchanted_golden_apple"));
        ItemStack enchantedResult = enchanted == null ? null : enchanted.buildResult(registry);
        expect(enchantedResult != null && enchantedResult.isSimilar(ItemStack.of(Material.ENCHANTED_GOLDEN_APPLE)),
                "enchanted_golden_apple makes the vanilla item");

        if (enchanted != null) {
            ItemStack[] plain = grid(LEGEND, "BBB", "BPB", "BBB");
            expect(RecipeManager.allows(enchanted, plain), "guard: plain golden apple accepted");
            ItemStack[] custom = grid(LEGEND, "BBB", "BPB", "BBB");
            custom[4] = ItemBuilder.build(registry.byId("custom_golden_apple"));
            expect(!RecipeManager.allows(enchanted, custom), "guard: Custom Golden Apple rejected as a plain golden apple");
        }
    }

    private void checkPlayerHead() {
        UUID uuid = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
        ItemStack head = PlayerHeadDrops.headOf(getServer().createProfile(uuid, "Steve"));
        ResolvableProfile profile = head.getData(DataComponentTypes.PROFILE);
        expect(head.getType() == Material.PLAYER_HEAD && profile != null && "Steve".equals(profile.name()),
                "dropped head carries the player's profile");
    }

    private void checkHeadPole(World world) {
        PlayerProfile profile = getServer().createProfile(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"), "Steve");
        Location spot = world.getSpawnLocation().toBlockLocation().add(0, 10, 0);
        spot.setYaw(90.0f); // looking west

        expect(HeadPole.place(spot, profile, Material.NETHER_BRICK_FENCE), "head pole placed in open air");
        Block pole = spot.getBlock();
        Block head = pole.getRelative(BlockFace.UP);
        expect(pole.getType() == Material.NETHER_BRICK_FENCE, "pole block is the configured fence");
        expect(head.getType() == Material.PLAYER_HEAD && head.getState() instanceof Skull skull
                        && skull.getPlayerProfile() != null && "Steve".equals(skull.getPlayerProfile().getName()),
                "head block carries the player's profile");
        expect(head.getBlockData() instanceof Rotatable rotatable && rotatable.getRotation() == BlockFace.WEST,
                "head faces the way the player looked");

        ItemStack drop = head.getDrops().stream().findFirst().orElse(null);
        ResolvableProfile dropProfile = drop == null ? null : drop.getData(DataComponentTypes.PROFILE);
        expect(drop != null && drop.getType() == Material.PLAYER_HEAD && dropProfile != null
                && "Steve".equals(dropProfile.name()), "breaking the head drops the skinned head");
        head.setType(Material.AIR);
        pole.setType(Material.AIR);

        // The flat world's bottom four layers are solid, so there's no room anywhere it looks
        Location buried = spot.clone();
        buried.setY(world.getMinHeight());
        expect(!HeadPole.place(buried, profile, Material.NETHER_BRICK_FENCE), "no room: no pole, head drops instead");
    }

    private void expectCrafts(ItemStack[] grid, String expected) {
        Recipe recipe = getServer().getCraftingRecipe(grid, getServer().getWorlds().getFirst());
        String actual = recipe instanceof Keyed keyed ? keyed.getKey().asString() : String.valueOf(recipe);
        expect(expected.equals(actual), "grid crafts " + expected + " (got " + actual + ")");
    }

    private static ItemStack[] grid(Map<Character, Material> legend, String... rows) {
        ItemStack[] grid = new ItemStack[9];
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                Material material = legend.get(rows[row].charAt(column));
                grid[row * 3 + column] = material == null ? null : ItemStack.of(material);
            }
        }
        return grid;
    }

    private static NamespacedKey key(String id) {
        return new NamespacedKey("rgoldenheads", id);
    }

    private static Set<String> recipeIds(RGoldenHeadsPlugin plugin) {
        return plugin.recipes().all().stream().map(RecipeSpec::id).collect(Collectors.toSet());
    }

    /** Breaks the config on purpose, reloads, and checks the damage stays contained. */
    private void checkValidation(RGoldenHeadsPlugin plugin) throws Exception {
        Path config = new File(plugin.getDataFolder(), "config.yml").toPath();
        byte[] original = Files.readAllBytes(config);
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(config.toFile());
            yaml.set("items.broken_material", Map.of("command", "brokenmaterial", "material", "NOT_A_THING"));
            yaml.set("items.duplicate_command", Map.of("command", "goldenhead", "material", "APPLE"));
            yaml.set("items.broken_effect", Map.of("command", "brokeneffect", "material", "GOLDEN_APPLE",
                    "effects", List.of(Map.of("type", "regenerationn", "level", 1, "duration", 5))));
            yaml.set("items.unknown_skin", Map.of("command", "unknownskin", "material", "GOLDEN_CARROT",
                    "item-model", "minecraft:player_head", "head-skin", "does_not_exist"));
            yaml.set("items.classic_skin", Map.of("command", "classicskin", "material", "GOLDEN_CARROT",
                    "item-model", "minecraft:player_head", "head-skin", "classic"));
            yaml.set("head-skins.mine.texture", CLASSIC_TEXTURE);
            yaml.set("items.custom_skin", Map.of("command", "customskin", "material", "GOLDEN_CARROT",
                    "item-model", "minecraft:player_head", "head-skin", "mine"));
            yaml.set("items.trident_animation", Map.of("command", "tridentanim", "material", "APPLE",
                    "animation", "TRIDENT"));
            yaml.set("recipes.instant_golden_head.enabled", true);
            yaml.set("recipes.bad_ingredient", Map.of("result", "apple", "shape", List.of("X"),
                    "ingredients", Map.of("X", "not_a_thing")));
            yaml.set("recipes.unmapped_symbol", Map.of("result", "apple", "shape", List.of("XY"),
                    "ingredients", Map.of("X", "stick")));
            yaml.set("recipes.bad_result", Map.of("result", "rgoldenheads:nope", "shape", List.of("X"),
                    "ingredients", Map.of("X", "stick")));
            yaml.set("recipes.uses_head", Map.of("result", "rgoldenheads:custom_golden_apple", "shapeless", true,
                    "ingredients", List.of("rgoldenheads:golden_head", "stick")));
            yaml.set("disabled-vanilla-recipes", List.of("golden_apple", "not_a_recipe", "other:thing"));
            yaml.save(config.toFile());

            expect(plugin.reloadAll(), "reload with broken entries succeeds");
            checkBrokenRecipes(plugin);
            expectCrafts(goldenAppleGrid(), "null");
            expectCrafts(grid(LEGEND, " G ", "GAG", " G "), "rgoldenheads:light_apple");
            ConsumableRegistry registry = plugin.registry();
            expect(registry.ids().containsAll(DEFAULT_IDS), "good items survive a broken config");
            expect(registry.byId("broken_material") == null, "bad material: item skipped");
            expect(registry.byId("duplicate_command") == null, "duplicate command: item skipped");

            ConsumableSpec brokenEffect = registry.byId("broken_effect");
            expect(brokenEffect != null && brokenEffect.effects().isEmpty(), "unknown potion type: effect dropped, item kept");
            ConsumableSpec unknownSkin = registry.byId("unknown_skin");
            expect(unknownSkin != null && unknownSkin.skinTexture() == null, "unknown head-skin: item kept without a skin");
            ConsumableSpec classic = registry.byId("classic_skin");
            expect(classic != null && CLASSIC_SKIN.equals(classic.skinUuid()), "head-skin: classic resolves to the old skin");
            ConsumableSpec custom = registry.byId("custom_skin");
            expect(custom != null && CLASSIC_TEXTURE.equals(custom.skinTexture()), "custom head-skins entry resolves");
            ConsumableSpec trident = registry.byId("trident_animation");
            expect(trident != null && trident.animation() != null,
                    "animation: TRIDENT resolves on this version (" + (trident == null ? null : trident.animation()) + ")");

            for (ConsumableSpec spec : registry.all()) {
                ItemBuilder.build(spec);
            }
            info("ok   every item from the broken config still builds");
        } finally {
            Files.write(config, original);
        }
        expect(plugin.reloadAll() && plugin.registry().ids().equals(DEFAULT_IDS), "restored config reloads cleanly");
        expect(recipeIds(plugin).equals(DEFAULT_RECIPES) && getServer().getRecipe(key("uses_head")) == null,
                "restored config leaves exactly the default recipes: " + recipeIds(plugin));
        expectCrafts(goldenAppleGrid(), "minecraft:golden_apple");
    }

    private static ItemStack[] goldenAppleGrid() {
        return grid(LEGEND, "GGG", "GAG", "GGG");
    }

    private void checkBrokenRecipes(RGoldenHeadsPlugin plugin) {
        Set<String> ids = recipeIds(plugin);
        expect(ids.containsAll(DEFAULT_RECIPES), "good recipes survive a broken config");
        expect(!ids.contains("instant_golden_head"), "duplicate grid: instant_golden_head skipped");
        expect(!ids.contains("bad_ingredient"), "unknown ingredient: recipe skipped");
        expect(!ids.contains("unmapped_symbol"), "unmapped shape character: recipe skipped");
        expect(!ids.contains("bad_result"), "unknown rgoldenheads: result: recipe skipped");

        RecipeSpec usesHead = plugin.recipes().byKey(key("uses_head"));
        expect(usesHead != null, "plugin-item ingredient recipe registered");
        if (usesHead != null) {
            ItemStack goldenHead = ItemBuilder.build(plugin.registry().byId("golden_head"));
            ItemStack stick = ItemStack.of(Material.STICK);
            ItemStack[] real = {goldenHead, stick, null, null, null, null, null, null, null};
            ItemStack[] fake = {ItemStack.of(Material.GOLDEN_CARROT), stick, null, null, null, null, null, null, null};
            expectCrafts(real, "rgoldenheads:uses_head");
            expect(RecipeManager.allows(usesHead, real), "guard: real Golden Head accepted as rgoldenheads:golden_head");
            expect(!RecipeManager.allows(usesHead, fake), "guard: plain golden carrot rejected as rgoldenheads:golden_head");
        }
    }

    private void expect(boolean condition, String description) {
        if (condition) {
            info("ok   " + description);
        } else {
            fail(description);
        }
    }

    private void fail(String description) {
        failed = true;
        lines.add("FAIL " + description);
        getLogger().severe("FAIL " + description);
    }

    private void info(String line) {
        lines.add(line);
        getLogger().info(line);
    }

    private void writeResult() {
        List<String> out = new ArrayList<>();
        out.add(failed ? "FAIL" : "PASS");
        out.addAll(lines);
        try {
            // Relative to the server's working directory, which is the task's run directory
            Files.write(Path.of("smoke-result.txt"), out);
        } catch (Exception e) {
            getLogger().severe("Could not write smoke-result.txt: " + e);
        }
    }
}
