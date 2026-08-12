package dev.zerorsen.rgoldenheads;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

/**
 * Turns config strings into components. MiniMessage by default, legacy '&' codes when
 * detected, so configs written in either style work without a format setting.
 */
public final class Text {

    // legacyAmpersand() ships with hex colours disabled, so build our own to accept &#rrggbb.
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    // '&' plus a colour/format code, or an &#rrggbb hex. A bare '&' (as in "Sword & Shield")
    // deliberately does not match, so that text stays on the MiniMessage path.
    private static final Pattern LEGACY_CODE = Pattern.compile("&(?:[0-9A-Fa-fK-Ok-oRr]|#[0-9A-Fa-f]{6})");

    private static final String ITEM_PLACEHOLDER = "<item>";

    private Text() {
    }

    public static boolean isLegacy(String raw) {
        return LEGACY_CODE.matcher(raw).find();
    }

    public static Component parse(String raw) {
        return isLegacy(raw) ? LEGACY.deserialize(raw) : MiniMessage.miniMessage().deserialize(raw);
    }

    /**
     * Parse with a single &lt;item&gt; placeholder. MiniMessage gets a real component
     * placeholder; the legacy path has no tag system, so the replacement is serialised
     * back to legacy text first to keep its colours.
     */
    public static Component parse(String raw, Component item) {
        if (isLegacy(raw)) {
            return LEGACY.deserialize(raw.replace(ITEM_PLACEHOLDER, LEGACY.serialize(item)));
        }
        return MiniMessage.miniMessage().deserialize(raw, Placeholder.component("item", item));
    }

    /** Lore renders italic by default in vanilla; only override when the author didn't say. */
    public static Component lore(String raw) {
        return parse(raw).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
