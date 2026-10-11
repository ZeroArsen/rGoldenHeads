package dev.zerorsen.rgoldenheads;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private Text() {
    }

    public static boolean isLegacy(String raw) {
        return LEGACY_CODE.matcher(raw).find();
    }

    public static Component parse(String raw) {
        return isLegacy(raw) ? LEGACY.deserialize(raw) : MiniMessage.miniMessage().deserialize(raw);
    }

    /** Parse with a single &lt;item&gt; placeholder. */
    public static Component parse(String raw, Component item) {
        return parse(raw, Map.of("item", item));
    }

    /**
     * Parse with named &lt;placeholders&gt;. MiniMessage gets real component placeholders; the
     * legacy path has no tag system, so each value is serialised back to legacy text first to
     * keep its colours. Values are components, so plain text passed in as
     * {@code Component.text(...)} is never itself parsed as markup.
     */
    public static Component parse(String raw, Map<String, Component> placeholders) {
        if (isLegacy(raw)) {
            String replaced = raw;
            for (Map.Entry<String, Component> entry : placeholders.entrySet()) {
                replaced = replaced.replace("<" + entry.getKey() + ">", LEGACY.serialize(entry.getValue()));
            }
            return LEGACY.deserialize(replaced);
        }
        List<TagResolver> resolvers = new ArrayList<>();
        placeholders.forEach((name, value) -> resolvers.add(Placeholder.component(name, value)));
        return MiniMessage.miniMessage().deserialize(raw, TagResolver.resolver(resolvers));
    }

    /** Lore renders italic by default in vanilla; only override when the author didn't say. */
    public static Component lore(String raw) {
        return parse(raw).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
