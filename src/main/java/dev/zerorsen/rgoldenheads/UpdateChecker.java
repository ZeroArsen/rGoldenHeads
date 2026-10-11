package dev.zerorsen.rgoldenheads;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Asks Modrinth once per startup whether a newer release exists. The query is filtered to
 * this server's Minecraft version and loader, so it only ever announces a build that would
 * actually run here. Any failure is silent apart from a debug line: an update check must
 * never be the reason a server logs errors.
 */
public final class UpdateChecker implements Listener {

    private static final String PROJECT_ID = "n4TUPak0";
    private static final String PROJECT_URL = "https://modrinth.com/project/" + PROJECT_ID;
    private static final String NOTIFY_PERMISSION = "rgoldenheads.admin.notify";

    private final Plugin plugin;
    private final Settings settings;
    private final Messages messages;
    private final String currentVersion;

    private volatile String latestVersion;

    public UpdateChecker(Plugin plugin, Settings settings, Messages messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    public void checkAsync() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> check());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String latest = latestVersion;
        if (latest != null && event.getPlayer().hasPermission(NOTIFY_PERMISSION)) {
            messages.send(event.getPlayer(), messages.updateAvailable(currentVersion, latest, PROJECT_URL));
        }
    }

    private void check() {
        String loader = isFolia() ? "folia" : "paper";
        String minecraft = plugin.getServer().getMinecraftVersion();
        URI uri = URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"
                + "?loaders=" + jsonArrayParam(loader)
                + "&game_versions=" + jsonArrayParam(minecraft));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                // Modrinth asks for a User-Agent that identifies the project and how to reach it
                .header("User-Agent", "ZeroArsen/rGoldenHeads/" + currentVersion
                        + " (https://github.com/ZeroArsen/rGoldenHeads)")
                .GET()
                .build();

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                debug("Update check: Modrinth answered HTTP " + response.statusCode() + ".");
                return;
            }

            String latest = newestRelease(response.body());
            if (latest != null && isNewer(latest, currentVersion)) {
                latestVersion = latest;
                plugin.getLogger().info("A newer version is available: " + latest
                        + " (you have " + currentVersion + "). " + PROJECT_URL);
            } else {
                debug("Update check: " + currentVersion + " is the newest release for " + minecraft + ".");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            debug("Update check failed: " + e);
        }
    }

    /** Modrinth lists versions newest first; take the first stable release. */
    private static String newestRelease(String body) {
        for (JsonElement element : JsonParser.parseString(body).getAsJsonArray()) {
            JsonObject version = element.getAsJsonObject();
            if ("release".equals(version.get("version_type").getAsString())) {
                return version.get("version_number").getAsString();
            }
        }
        return null;
    }

    /** Numeric comparison of the dotted part only, so "2.10.0" beats "2.9.1". */
    static boolean isNewer(String candidate, String current) {
        int[] a = numericParts(candidate);
        int[] b = numericParts(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int[] numericParts(String version) {
        // Drop any pre-release or build suffix ("2.1.0-beta", "2.1.0+5") before splitting
        String core = version.split("[-+]", 2)[0];
        String[] parts = core.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private static String jsonArrayParam(String value) {
        return URLEncoder.encode("[\"" + value + "\"]", StandardCharsets.UTF_8);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void debug(String message) {
        if (settings.debug()) {
            plugin.getLogger().info(message);
        }
    }
}
