package dev.zerorsen.rgoldenheads;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class ItemBuilder {

    /** Written to every item so any item made by this plugin can be identified in one lookup. */
    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(ConsumableSpec.NAMESPACE, "item");

    private ItemBuilder() {
    }

    public static ItemStack build(ConsumableSpec spec) {
        ItemStack item = ItemStack.of(spec.material(), spec.amount());

        item.editPersistentDataContainer(pdc -> {
            pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, spec.id());
            // Legacy marker: items handed out by 1.x carry only this, so keep writing it
            pdc.set(spec.itemTag(), PersistentDataType.BOOLEAN, true);
        });

        item.setData(DataComponentTypes.ITEM_NAME, spec.displayName());
        if (!spec.lore().isEmpty()) {
            item.setData(DataComponentTypes.LORE, ItemLore.lore(spec.lore()));
        }
        if (spec.itemModel() != null) {
            item.setData(DataComponentTypes.ITEM_MODEL, spec.itemModel());
        }
        if (spec.skinTexture() != null) {
            item.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile()
                    .uuid(spec.skinUuid())
                    .addProperty(new ProfileProperty("textures", spec.skinTexture()))
                    .build());
        }
        if (spec.maxStackSize() != null) {
            item.setData(DataComponentTypes.MAX_STACK_SIZE, spec.maxStackSize());
        }
        if (spec.glint() != null) {
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, spec.glint());
        }
        item.setData(DataComponentTypes.FOOD, FoodProperties.food()
                .canAlwaysEat(spec.canAlwaysEat())
                .nutrition(spec.nutrition())
                .saturation(spec.saturation())
                .build());

        Consumable.Builder consumable = Consumable.consumable()
                .consumeSeconds(spec.consumeSeconds())
                .addEffect(ConsumeEffect.applyStatusEffects(spec.effects(), spec.effectChance()));
        // Only override these when the config asked to; otherwise the vanilla defaults must stand
        if (spec.animation() != null) {
            consumable.animation(spec.animation());
        }
        if (spec.consumeParticles() != null) {
            consumable.hasConsumeParticles(spec.consumeParticles());
        }
        if (spec.consumeSound() != null) {
            consumable.sound(spec.consumeSound());
        }
        item.setData(DataComponentTypes.CONSUMABLE, consumable.build());

        if (spec.cooldownSeconds() != null) {
            item.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(spec.cooldownSeconds())
                    .cooldownGroup(spec.cooldownGroup())
                    .build());
        }

        return item;
    }

    /** The item's configured ID, or null when it was not made by this plugin. */
    public static String idOf(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return item.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
    }
}
