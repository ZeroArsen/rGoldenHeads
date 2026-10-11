package dev.zerorsen.rgoldenheads;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Puts items into a player's inventory. Every give runs on the player's own entity
 * scheduler: on Folia a console command runs on the global region thread, where touching a
 * player's inventory is unsafe, and on Paper the same call simply runs on the next tick.
 */
public final class ItemGiver {

    // A full inventory's worth; anything beyond that can only end up on the floor.
    private static final int MAX_STACKS = 36;

    private final Plugin plugin;
    private final Settings settings;
    private final Messages messages;

    public ItemGiver(Plugin plugin, Settings settings, Messages messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
    }

    /** The most of {@code spec}'s item a single give may hand out. */
    public static int maxAmount(ConsumableSpec spec) {
        return spec.material().getMaxStackSize() * MAX_STACKS;
    }

    /**
     * Gives {@code amount} of the item, or the spec's configured amount when {@code amount}
     * is not positive. Returns false when the player left before the give could run.
     */
    public boolean give(Player player, ConsumableSpec spec, int amount) {
        int total = amount > 0 ? amount : spec.amount();
        // run() returns null when the player is already gone (logged out, retired entity)
        return player.getScheduler().run(plugin, task -> giveNow(player, spec, total), null) != null;
    }

    private void giveNow(Player player, ConsumableSpec spec, int total) {
        ItemStack template = ItemBuilder.build(spec);
        int maxStack = template.getMaxStackSize();

        boolean overflowed = false;
        for (int remaining = total; remaining > 0; remaining -= maxStack) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(remaining, maxStack));
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) {
                continue;
            }
            overflowed = true;
            if (settings.dropOverflow()) {
                for (ItemStack overflow : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                }
            }
        }

        if (!overflowed) {
            messages.send(player, messages.itemReceived(spec.displayName()));
        } else if (settings.dropOverflow()) {
            messages.send(player, messages.inventoryFullDropped());
        } else {
            messages.send(player, messages.inventoryFull());
        }
    }
}
