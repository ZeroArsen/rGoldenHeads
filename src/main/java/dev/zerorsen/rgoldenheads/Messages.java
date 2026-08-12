package dev.zerorsen.rgoldenheads;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Every user-facing chat string. Defaults are duplicated here so a config with the
 * whole messages: section deleted still produces sensible output.
 */
public final class Messages {

    private static final String DEFAULT_PREFIX = "";
    private static final String DEFAULT_PLAYERS_ONLY = "<red>Only players can receive items.";
    private static final String DEFAULT_NO_PERMISSION = "<red>You don't have permission to do that.";
    private static final String DEFAULT_ITEM_RECEIVED = "<green>You received <item><green>.";
    private static final String DEFAULT_INVENTORY_FULL = "<red>Your inventory is full.";
    private static final String DEFAULT_INVENTORY_FULL_DROPPED =
            "<yellow>Your inventory was full, so the item dropped at your feet.";
    private static final String DEFAULT_ITEM_UNAVAILABLE =
            "<red>That item is no longer available. Ask an admin to check the configuration.";
    private static final String DEFAULT_RELOAD_SUCCESS = "<green>Configuration reloaded.";
    private static final String DEFAULT_RELOAD_FAILED =
            "<red>Reload failed. See the console for details; the previous items are still active.";
    private static final String DEFAULT_USAGE = "<yellow>Usage: /<label> reload";

    private final JavaPlugin plugin;

    private volatile Component prefix = Component.empty();
    private volatile String playersOnly = DEFAULT_PLAYERS_ONLY;
    private volatile String noPermission = DEFAULT_NO_PERMISSION;
    private volatile String itemReceived = DEFAULT_ITEM_RECEIVED;
    private volatile String inventoryFull = DEFAULT_INVENTORY_FULL;
    private volatile String inventoryFullDropped = DEFAULT_INVENTORY_FULL_DROPPED;
    private volatile String itemUnavailable = DEFAULT_ITEM_UNAVAILABLE;
    private volatile String reloadSuccess = DEFAULT_RELOAD_SUCCESS;
    private volatile String reloadFailed = DEFAULT_RELOAD_FAILED;
    private volatile String usage = DEFAULT_USAGE;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        this.prefix = Text.parse(config.getString("messages.prefix", DEFAULT_PREFIX));
        this.playersOnly = config.getString("messages.players-only", DEFAULT_PLAYERS_ONLY);
        this.noPermission = config.getString("messages.no-permission", DEFAULT_NO_PERMISSION);
        this.itemReceived = config.getString("messages.item-received", DEFAULT_ITEM_RECEIVED);
        this.inventoryFull = config.getString("messages.inventory-full", DEFAULT_INVENTORY_FULL);
        this.inventoryFullDropped =
                config.getString("messages.inventory-full-dropped", DEFAULT_INVENTORY_FULL_DROPPED);
        this.itemUnavailable = config.getString("messages.item-unavailable", DEFAULT_ITEM_UNAVAILABLE);
        this.reloadSuccess = config.getString("messages.reload-success", DEFAULT_RELOAD_SUCCESS);
        this.reloadFailed = config.getString("messages.reload-failed", DEFAULT_RELOAD_FAILED);
        this.usage = config.getString("messages.usage", DEFAULT_USAGE);
    }

    /**
     * Sends a message with the configured prefix. Setting a message to "" in config
     * parses to an empty component, which is silenced here rather than sending a bare prefix.
     */
    public void send(CommandSender to, Component body) {
        if (Component.empty().equals(body)) {
            return;
        }
        to.sendMessage(prefix.append(body));
    }

    public Component playersOnly() {
        return Text.parse(playersOnly);
    }

    public Component noPermission() {
        return Text.parse(noPermission);
    }

    public Component itemReceived(Component itemName) {
        return Text.parse(itemReceived, itemName);
    }

    public Component inventoryFull() {
        return Text.parse(inventoryFull);
    }

    public Component inventoryFullDropped() {
        return Text.parse(inventoryFullDropped);
    }

    public Component itemUnavailable() {
        return Text.parse(itemUnavailable);
    }

    public Component reloadSuccess() {
        return Text.parse(reloadSuccess);
    }

    public Component reloadFailed() {
        return Text.parse(reloadFailed);
    }

    public Component usage(String label) {
        return Text.parse(usage.replace("<label>", label));
    }
}
