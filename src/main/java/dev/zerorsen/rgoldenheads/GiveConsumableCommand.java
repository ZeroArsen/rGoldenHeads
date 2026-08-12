package dev.zerorsen.rgoldenheads;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class GiveConsumableCommand implements BasicCommand {

    private final String itemId;

    // Frozen at startup: Brigadier bakes the requirement predicate into the command tree
    // when the node is built, so changing this node in config needs a restart to affect
    // tab-completion visibility. The give itself re-checks the live spec below.
    private final String permission;

    private final Settings settings;
    private final Messages messages;
    private final ConsumableRegistry registry;

    public GiveConsumableCommand(String itemId, String permission, Settings settings,
                                 Messages messages, ConsumableRegistry registry) {
        this.itemId = itemId;
        this.permission = permission;
        this.settings = settings;
        this.messages = messages;
        this.registry = registry;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            messages.send(sender, messages.playersOnly());
            return;
        }

        ConsumableSpec spec = registry.byId(itemId);
        if (spec == null) {
            // Disabled or removed since startup; the command outlives the item until restart
            messages.send(player, messages.itemUnavailable());
            return;
        }

        // Checked against the live spec, so tightening a permission in config takes effect
        // on the next give even though tab visibility still uses the startup node.
        if (!player.hasPermission(spec.permission())) {
            messages.send(player, messages.noPermission());
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(ItemBuilder.build(spec));
        if (!leftover.isEmpty()) {
            if (!settings.dropOverflow()) {
                messages.send(player, messages.inventoryFull());
                return;
            }
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            messages.send(player, messages.inventoryFullDropped());
            return;
        }

        messages.send(player, messages.itemReceived(spec.displayName()));
    }

    @Override
    public String permission() {
        return permission;
    }
}
