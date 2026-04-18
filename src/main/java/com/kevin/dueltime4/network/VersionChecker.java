package com.kevin.dueltime4.network;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.data.pojo.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VersionChecker {
    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/Kevin28576/DuelTime4/refs/heads/main/src/main/java/com/kevin/dueltime4/network/version";
    private static final String ENGLISH_LANGUAGE_KEY = "en_us";

    public void checkForUpdates(Player player) {
        if (player == null || VERSION_URL.isBlank()) {
            return;
        }

        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(VERSION_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                List<String> lines = new ArrayList<>();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (!line.isBlank()) {
                            lines.add(line);
                        }
                    }
                }

                if (lines.isEmpty()) {
                    return;
                }

                ParsedVersionFile parsed = parseVersionFile(lines);
                String latestVersion = parsed.latestVersion;
                String currentVersion = plugin.getDescription().getVersion();
                if (!isNewerVersion(currentVersion, latestVersion)) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> sendUpdateMessage(player, currentVersion, latestVersion, parsed.notes));
            } catch (Exception ignored) {
            }
        });
    }

    private void sendUpdateMessage(Player player, String currentVersion, String latestVersion, List<String> notes) {
        if (player == null || !player.isOnline()) {
            return;
        }

        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        YamlConfiguration languageConfig = resolveLanguageConfig(player);

        String prefix = colorize(plugin.getCfgManager().getPrefix());
        if (prefix.isBlank()) {
            prefix = "§8[§a§lDuel§2§l§oTime§2§l4§8] §r";
        }

        String separator = getLocalizedMessage(languageConfig, "VersionChecker.separator",
                "&8&m───────────────────────────────────");

        player.sendMessage(separator);
        player.sendMessage(prefix + getLocalizedMessage(languageConfig, "VersionChecker.new-version-title",
                "&6&lA new version is available!"));
        player.sendMessage(prefix + applyPlaceholders(
                getLocalizedMessage(languageConfig, "VersionChecker.current-latest",
                        "&7Current version: &f{current} &8-> &7Latest version: &a{latest}"),
                "current", currentVersion,
                "latest", latestVersion
        ));
        player.sendMessage(prefix + getLocalizedMessage(languageConfig, "VersionChecker.highlights",
                "&7Update highlights:"));

        if (!notes.isEmpty()) {
            int maxNotes = Math.min(notes.size(), 5);
            String noteTemplate = getLocalizedMessage(languageConfig, "VersionChecker.note",
                    "&8  - &f{line}");
            for (int i = 0; i < maxNotes; i++) {
                player.sendMessage(applyPlaceholders(noteTemplate, "line", notes.get(i)));
            }

            if (notes.size() > maxNotes) {
                player.sendMessage(applyPlaceholders(
                        getLocalizedMessage(languageConfig, "VersionChecker.more",
                                "&8  - &7...and {count} more update(s)"),
                        "count", String.valueOf(notes.size() - maxNotes)
                ));
            }
        } else {
            player.sendMessage(getLocalizedMessage(languageConfig, "VersionChecker.no-summary",
                    "&8  - &7No update summary is provided for this release."));
        }

        player.sendMessage(prefix + getLocalizedMessage(languageConfig, "VersionChecker.recommend",
                "&7Please update soon for better compatibility and latest fixes."));
        player.sendMessage(separator);
    }

    private YamlConfiguration resolveLanguageConfig(Player player) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        Map<String, YamlConfiguration> languageMap = plugin.getMsgManager().getLanguageYamlFileMap();
        if (languageMap.isEmpty()) {
            return null;
        }

        String language = plugin.getCfgManager().getDefaultLanguage();

        try {
            if (plugin.getCacheManager() != null && plugin.getCacheManager().getPlayerDataCache() != null) {
                PlayerData playerData = plugin.getCacheManager().getPlayerDataCache().getAnyway(player.getName());
                if (playerData != null && playerData.getLanguage() != null && languageMap.containsKey(playerData.getLanguage())) {
                    language = playerData.getLanguage();
                }
            }
        } catch (Exception ignored) {
        }

        if (language != null && languageMap.containsKey(language)) {
            return languageMap.get(language);
        }
        if (languageMap.containsKey(ENGLISH_LANGUAGE_KEY)) {
            return languageMap.get(ENGLISH_LANGUAGE_KEY);
        }
        return null;
    }

    private String getLocalizedMessage(YamlConfiguration languageConfig, String key, String fallbackEnglish) {
        String message = languageConfig != null ? languageConfig.getString(key) : null;
        if (message == null || message.isBlank()) {
            Map<String, YamlConfiguration> languageMap = DuelTimePlugin.getInstance().getMsgManager().getLanguageYamlFileMap();
            YamlConfiguration englishConfig = languageMap.get(ENGLISH_LANGUAGE_KEY);
            if (englishConfig != null) {
                message = englishConfig.getString(key);
            }
        }
        if (message == null || message.isBlank()) {
            message = fallbackEnglish;
        }
        return colorize(message);
    }

    private String applyPlaceholders(String message, String... replacements) {
        if (message == null || replacements == null || replacements.length == 0) {
            return message;
        }
        String resolved = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            resolved = resolved.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return resolved;
    }

    private String colorize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('&', '\u00A7').replace('\u79AE', '\u00A7');
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private int parseVersionPart(String part) {
        String digits = part.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private ParsedVersionFile parseVersionFile(List<String> lines) {
        String latestVersion = lines.get(0).trim();
        List<String> notes = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("download-url:") || lower.startsWith("sha256:") || lower.equals("notes:")) {
                continue;
            }
            if (line.startsWith("- ")) {
                line = line.substring(2).trim();
            }
            if (!line.isEmpty()) {
                notes.add(line);
            }
        }
        return new ParsedVersionFile(latestVersion, notes);
    }

    private static class ParsedVersionFile {
        private final String latestVersion;
        private final List<String> notes;

        private ParsedVersionFile(String latestVersion, List<String> notes) {
            this.latestVersion = latestVersion;
            this.notes = notes;
        }
    }
}
