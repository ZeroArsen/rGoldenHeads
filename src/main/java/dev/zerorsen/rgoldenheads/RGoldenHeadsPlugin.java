package dev.zerorsen.rgoldenheads;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RGoldenHeadsPlugin extends JavaPlugin {

    private static final String GIVE_PARENT = "rgoldenheads.give";

    private final ConsumableRegistry registry = new ConsumableRegistry();
    private final Settings settings = new Settings(this);
    private final Messages messages = new Messages(this);
    private final List<String> ownedPermissions = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!reloadAll()) {
            getLogger().severe("config.yml could not be read. Fix it and restart the server.");
        }

        // Taken once: the command tree is built from this list and cannot gain or lose
        // labels until the next server start, however often the config is reloaded.
        List<ConsumableSpec> startupSpecs = registry.all();
        if (startupSpecs.isEmpty()) {
            getLogger().warning("No usable items in config.yml, so only /rgoldenheads is registered.");
        }
        registerPermissions(startupSpecs);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("rgoldenheads", "rGoldenHeads admin command", List.of("rgh"),
                    new RGoldenHeadsCommand(this, messages));
            for (ConsumableSpec spec : startupSpecs) {
                commands.register(spec.commandName(), spec.description(), spec.aliases(),
                        new GiveConsumableCommand(spec.id(), spec.permission(), settings, messages, registry));
            }
        });

        getLogger().info("rGoldenHeads enabled with " + startupSpecs.size() + " item(s).");
    }

    @Override
    public void onDisable() {
        // Otherwise the dynamic nodes pile up when a plugin manager reloads us in place
        PluginManager pluginManager = getServer().getPluginManager();
        ownedPermissions.forEach(pluginManager::removePermission);
        ownedPermissions.clear();
    }

    /** Returns false and keeps the previously loaded items when the new config is unusable. */
    public boolean reloadAll() {
        reloadConfig();
        settings.load();
        FileConfiguration config = getConfig();

        ConfigurationSection items = config.getConfigurationSection("items");
        // reloadConfig() swallows a YAML syntax error and hands back an empty config, so
        // losing every item at once almost always means that rather than a deliberate edit.
        if (items == null && !registry.ids().isEmpty()) {
            getLogger().severe("config.yml has no 'items' section. Is the YAML malformed? "
                    + "Keeping the previously loaded items.");
            return false;
        }

        Set<String> before = Set.copyOf(registry.ids());
        messages.load();
        registry.replaceAll(ConsumableLoader.load(items, getLogger(), settings.debug()));

        registry.ids().stream()
                .filter(id -> !before.contains(id))
                .forEach(id -> getLogger().info("Item '" + id + "' was added, but its command will not "
                        + "exist until the server restarts."));

        return true;
    }

    private void registerPermissions(List<ConsumableSpec> specs) {
        PluginManager pluginManager = getServer().getPluginManager();
        for (ConsumableSpec spec : specs) {
            // Two items may deliberately share a node; only the first one creates it
            if (pluginManager.getPermission(spec.permission()) != null) {
                continue;
            }
            Permission permission = new Permission(spec.permission(),
                    "Allows using /" + spec.commandName(), PermissionDefault.OP);
            pluginManager.addPermission(permission);
            permission.addParent(GIVE_PARENT, true);
            ownedPermissions.add(spec.permission());
        }
    }
}
