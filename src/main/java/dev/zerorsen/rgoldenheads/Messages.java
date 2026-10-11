package dev.zerorsen.rgoldenheads;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every user-facing chat string. Defaults are duplicated here so a config with the whole
 * messages: section deleted, or written by an older version, still produces sensible output.
 */
public final class Messages {

    private static final Map<String, String> DEFAULTS = defaults();

    private final JavaPlugin plugin;

    private volatile Component prefix = Component.empty();
    private volatile Map<String, String> raw = DEFAULTS;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("players-only", "<red>Only players can receive items.");
        m.put("no-permission", "<red>You don't have permission to do that.");
        m.put("item-received", "<green>You received <item><green>.");
        m.put("inventory-full", "<red>Your inventory is full.");
        m.put("inventory-full-dropped", "<yellow>Your inventory was full, so the item dropped at your feet.");
        m.put("item-unavailable", "<red>That item is no longer available. Ask an admin to check the configuration.");
        m.put("reload-success", "<green>Configuration reloaded.");
        m.put("reload-failed", "<red>Reload failed. See the console for details; the previous items are still active.");
        m.put("usage", "<yellow>Usage: /<label> reload | /<label> give {player|*} {item} [amount]");
        m.put("given-to-player", "<green>Gave <amount>x <item><green> to <player>.");
        m.put("given-to-all", "<green>Gave <amount>x <item><green> to <count> online player(s).");
        m.put("player-not-found", "<red>No online player is named <player>.");
        m.put("no-players-online", "<red>Nobody is online.");
        m.put("unknown-item", "<red>Unknown item '<id>'. Available: <items>");
        m.put("invalid-amount", "<red>The amount must be a whole number from 1 to <max>.");
        m.put("update-available", "<yellow>rGoldenHeads <latest> is available (you have <current>): <url>");
        return Map.copyOf(m);
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        Map<String, String> loaded = new LinkedHashMap<>();
        DEFAULTS.forEach((key, fallback) -> loaded.put(key, config.getString("messages." + key, fallback)));
        this.raw = Map.copyOf(loaded);
        this.prefix = Text.parse(config.getString("messages.prefix", ""));
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
        return render("players-only");
    }

    public Component noPermission() {
        return render("no-permission");
    }

    public Component itemReceived(Component item) {
        return render("item-received", Map.of("item", item));
    }

    public Component inventoryFull() {
        return render("inventory-full");
    }

    public Component inventoryFullDropped() {
        return render("inventory-full-dropped");
    }

    public Component itemUnavailable() {
        return render("item-unavailable");
    }

    public Component reloadSuccess() {
        return render("reload-success");
    }

    public Component reloadFailed() {
        return render("reload-failed");
    }

    public Component usage(String label) {
        return render("usage", Map.of("label", Component.text(label)));
    }

    public Component givenToPlayer(int amount, Component item, String player) {
        return render("given-to-player", Map.of(
                "amount", Component.text(amount), "item", item, "player", Component.text(player)));
    }

    public Component givenToAll(int amount, Component item, int count) {
        return render("given-to-all", Map.of(
                "amount", Component.text(amount), "item", item, "count", Component.text(count)));
    }

    public Component playerNotFound(String player) {
        return render("player-not-found", Map.of("player", Component.text(player)));
    }

    public Component noPlayersOnline() {
        return render("no-players-online");
    }

    public Component unknownItem(String id, String available) {
        return render("unknown-item", Map.of("id", Component.text(id), "items", Component.text(available)));
    }

    public Component invalidAmount(int max) {
        return render("invalid-amount", Map.of("max", Component.text(max)));
    }

    public Component updateAvailable(String current, String latest, String url) {
        return render("update-available", Map.of(
                "current", Component.text(current), "latest", Component.text(latest), "url", Component.text(url)));
    }

    private Component render(String key) {
        return Text.parse(raw.get(key));
    }

    private Component render(String key, Map<String, Component> placeholders) {
        return Text.parse(raw.get(key), placeholders);
    }
}
