package dev.zerorsen.rgoldenheads;

import org.bukkit.configuration.ConfigurationSection;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Named head skins. The built-ins ship in code rather than only in config.yml, so any config,
 * including one written by 2.0.x, can switch skins with a single {@code head-skin:} line.
 */
public final class HeadSkins {

    public record HeadSkin(String texture, UUID uuid) {
    }

    public static final String MODERN = "modern";
    public static final String CLASSIC = "classic";

    private static final Map<String, HeadSkin> BUILT_IN = Map.of(
            // The default from 2.1 on. Source: https://namemc.com/skin/7e3c84ef1342fb4d
            MODERN, new HeadSkin(
                    "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmUxNTUxYjk0"
                            + "MzRmYWUxZThlOTMyN2Y3YzFjZWFjN2JjYWUzY2Y3MWZiZDY3ZTc5NDJmMGMyY2U2MjgwNWMxOCJ9fX0=",
                    UUID.fromString("38903a1f-09fb-5757-966e-c748605e243a")),
            // The 1.x/2.0 look. Kept byte-identical so newly given classic heads still stack
            // with the ones those versions handed out.
            CLASSIC, new HeadSkin(
                    "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmZkNTA2MDI2MTFl"
                            + "OGM0ZjkwY2UxYWRhNDhlNjMyY2UyYTFkNTVlZDg0YTRiNWNkYTBhZGJiY2Q5MGY1NTU1OSJ9fX0=",
                    UUID.fromString("befd0365-bdc7-5023-9013-a57583aef725")));

    private HeadSkins() {
    }

    /** The built-in skins, overlaid with any head-skins: entries from config. Never throws. */
    public static Map<String, HeadSkin> load(ConfigurationSection section, Logger log) {
        Map<String, HeadSkin> skins = new LinkedHashMap<>(BUILT_IN);
        if (section == null) {
            return Collections.unmodifiableMap(skins);
        }

        for (String rawName : section.getKeys(false)) {
            String path = "head-skins." + rawName;
            ConfigurationSection entry = section.getConfigurationSection(rawName);
            if (entry == null) {
                log.warning(path + " is not a configuration block, so it was skipped.");
                continue;
            }
            String texture = entry.getString("texture");
            if (texture == null || texture.isBlank()) {
                log.warning(path + " is missing the required key 'texture', so it was skipped.");
                continue;
            }
            if (!isUsableTexture(texture, path + ".texture", log)) {
                continue;
            }
            skins.put(rawName.toLowerCase(Locale.ROOT),
                    new HeadSkin(texture, uuid(entry.getString("uuid"), texture, path + ".uuid", log)));
        }

        return Collections.unmodifiableMap(skins);
    }

    /** False, with the reason logged, when the texture cannot be used at all. */
    static boolean isUsableTexture(String texture, String path, Logger log) {
        try {
            byte[] decoded = Base64.getDecoder().decode(texture);
            if (!new String(decoded, StandardCharsets.UTF_8).contains("textures")) {
                log.warning(path + " decodes but does not look like a skin blob. "
                        + "Applying it anyway; the head may render as a default skin.");
            }
            return true;
        } catch (IllegalArgumentException e) {
            log.warning(path + " is not valid base64, so this skin was skipped. "
                    + "Copy the 'value' field from a head database, not the whole JSON.");
            return false;
        }
    }

    /** The configured UUID, or one derived from the texture so identical items stack across restarts. */
    static UUID uuid(String raw, String texture, String path, Logger log) {
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                log.warning(path + " '" + raw + "' is not a valid UUID; deriving one from the texture instead.");
            }
        }
        return UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
    }
}
