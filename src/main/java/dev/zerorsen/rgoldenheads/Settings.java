package dev.zerorsen.rgoldenheads;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * General (non per-item) config values. Fields are volatile so a reload on the main
 * thread is safely visible to anything that reads them later.
 */
public final class Settings {

    private static final int EXPECTED_CONFIG_VERSION = 2;
    private static final Material DEFAULT_POLE = Material.NETHER_BRICK_FENCE;

    private final JavaPlugin plugin;

    private volatile boolean debug = false;
    private volatile boolean dropOverflow = true;
    private volatile boolean updateChecker = true;
    private volatile boolean dropHeads = false;
    private volatile boolean onlyPlayerKills = false;
    private volatile boolean unlockRecipes = true;
    private volatile boolean headPole = false;
    private volatile Material poleMaterial = DEFAULT_POLE;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        int version = config.getInt("config-version", 0);
        if (version != EXPECTED_CONFIG_VERSION) {
            plugin.getLogger().warning("config.yml is version " + version + " but this build expects "
                    + EXPECTED_CONFIG_VERSION + ". Compare your file against the bundled default; "
                    + "any missing keys fall back to their defaults.");
            if (version == 1) {
                plugin.getLogger().warning("Version 2 added the player-heads, recipe-book and recipes sections. "
                        + "Until you copy them in, head drops stay off and no custom recipes exist.");
            }
        }

        this.debug = config.getBoolean("debug", false);
        this.dropOverflow = config.getBoolean("drop-overflow", true);
        this.updateChecker = config.getBoolean("update-checker.enabled", true);
        this.dropHeads = config.getBoolean("player-heads.drop-on-death", false);
        this.onlyPlayerKills = config.getBoolean("player-heads.only-player-kills", false);
        this.unlockRecipes = config.getBoolean("recipe-book.unlock-on-join", true);
        this.headPole = config.getBoolean("player-heads.head-pole.enabled", false);
        this.poleMaterial = poleMaterial(config.getString("player-heads.head-pole.pole"));
    }

    private Material poleMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_POLE;
        }
        Material material = Material.matchMaterial(raw.trim());
        if (material == null || material.isLegacy() || material.isAir() || !material.isBlock()) {
            plugin.getLogger().warning("player-heads.head-pole.pole '" + raw + "' is not a placeable block; using "
                    + DEFAULT_POLE.getKey().getKey() + ".");
            return DEFAULT_POLE;
        }
        return material;
    }

    public boolean debug() {
        return debug;
    }

    public boolean dropOverflow() {
        return dropOverflow;
    }

    public boolean updateChecker() {
        return updateChecker;
    }

    public boolean dropHeads() {
        return dropHeads;
    }

    public boolean onlyPlayerKills() {
        return onlyPlayerKills;
    }

    public boolean unlockRecipes() {
        return unlockRecipes;
    }

    public boolean headPole() {
        return headPole;
    }

    public Material poleMaterial() {
        return poleMaterial;
    }
}
