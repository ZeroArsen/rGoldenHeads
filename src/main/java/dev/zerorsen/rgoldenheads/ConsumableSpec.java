package dev.zerorsen.rgoldenheads;

import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.UUID;

/**
 * One fully-validated consumable, built by {@link ConsumableLoader} from a config entry.
 * Fields documented as optional may be null, which means the corresponding data component
 * (or builder call) is left untouched so the vanilla default applies.
 */
public record ConsumableSpec(
        String id,                      // lowercase key path: PDC tag and default cooldown group
        String commandName,
        List<String> aliases,
        String description,
        String permission,
        Component displayName,
        List<Component> lore,           // empty = no LORE component
        Material material,
        Key itemModel,                  // optional
        UUID skinUuid,                  // non-null whenever skinTexture is set
        String skinTexture,             // optional
        Boolean glint,                  // optional tri-state; null = vanilla default
        int amount,
        float consumeSeconds,
        ItemUseAnimation animation,     // optional
        Boolean consumeParticles,       // optional
        Key consumeSound,               // optional
        int nutrition,
        float saturation,
        boolean canAlwaysEat,
        Float cooldownSeconds,          // optional; null = no USE_COOLDOWN component at all
        Key cooldownGroup,
        float effectChance,
        List<PotionEffect> effects
) {

    public static final String NAMESPACE = "rgoldenheads";

    /** Per-item marker, kept because items handed out by 1.x carry only this key. */
    public NamespacedKey itemTag() {
        return new NamespacedKey(NAMESPACE, id);
    }
}
