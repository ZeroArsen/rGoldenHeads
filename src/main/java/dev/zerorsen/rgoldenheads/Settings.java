package dev.zerorsen.rgoldenheads;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * General (non per-item) config values. Fields are volatile so a reload on the main
 * thread is safely visible to anything that reads them later.
 */
public final class Settings {

    private static final int EXPECTED_CONFIG_VERSION = 1;

    private final JavaPlugin plugin;

    private volatile boolean debug = false;
    private volatile boolean dropOverflow = true;

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
        }

        this.debug = config.getBoolean("debug", false);
        this.dropOverflow = config.getBoolean("drop-overflow", true);
    }

    public boolean debug() {
        return debug;
    }

    public boolean dropOverflow() {
        return dropOverflow;
    }
}
