package com.kevin.dueltime4.network;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class VersionChecker {
    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/Kevin28576/DuelTime4/main/src/main/java/com/kevin/dueltime4/network/version";

    public void checkForUpdates(Player player) {
        if (VERSION_URL.isBlank()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(DuelTimePlugin.getInstance(), () -> {
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

                String latestVersion = lines.get(0).trim();
                String currentVersion = DuelTimePlugin.getInstance().getDescription().getVersion();
                if (isNewerVersion(currentVersion, latestVersion)) {
                    String prefix = "§8[§a§lDuel§2§l§oTime§2§l4§8] §r";
                    player.sendMessage("§8§m────────────────────────────────────────");
                    player.sendMessage(prefix + "§6§l有新版本可更新！");
                    player.sendMessage(prefix + "§7目前版本: §f" + currentVersion + " §8→ §7最新版本: §a" + latestVersion);
                    player.sendMessage(prefix + "§7更新重點:");
                    if (lines.size() > 1) {
                        int maxNotes = Math.min(lines.size() - 1, 5);
                        for (int i = 1; i <= maxNotes; i++) {
                            player.sendMessage("§8  • §f" + lines.get(i));
                        }
                        if (lines.size() - 1 > maxNotes) {
                            player.sendMessage("§8  • §7...還有 " + ((lines.size() - 1) - maxNotes) + " 條更新");
                        }
                    } else {
                        player.sendMessage("§8  • §7本次未提供更新摘要");
                    }
                    player.sendMessage(prefix + "§7建議儘快更新以獲得最佳相容性與修正。");
                    player.sendMessage("§8§m────────────────────────────────────────");
                }
            } catch (Exception ignored) {
            }
        });
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
}
