package dev.zerorsen.rgoldenheads;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/** Drops a dead player's own head, wearing their skin, when player-heads.drop-on-death is on. */
public final class PlayerHeadDrops implements Listener {

    private final Settings settings;

    public PlayerHeadDrops(Settings settings) {
        this.settings = settings;
    }

    /** A player head showing this profile's skin; vanilla names it "<name>'s Head". */
    public static ItemStack headOf(PlayerProfile profile) {
        ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
        head.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile));
        return head;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!settings.dropHeads()) {
            return;
        }
        Player player = event.getPlayer();
        Player killer = player.getKiller();
        if (settings.onlyPlayerKills() && (killer == null || killer.equals(player))) {
            return;
        }

        ItemStack head = headOf(player.getPlayerProfile());
        if (event.getKeepInventory()) {
            // The drop list is ignored when the inventory is kept, so drop the head ourselves
            player.getWorld().dropItemNaturally(player.getLocation(), head);
        } else {
            event.getDrops().add(head);
        }
    }
}
