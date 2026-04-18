package com.kevin.dueltime4.network;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateManager {
    private final DuelTimePlugin plugin;
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private final AtomicBoolean downloading = new AtomicBoolean(false);

    private volatile long lastCheckedAt;
    private volatile String lastLatestVersion = "-";
    private volatile String lastSource = "-";
    private volatile String lastError = "";
    private volatile boolean updateAvailable;
    private volatile boolean updatePrepared;
    private volatile String preparedVersion = "";
    private volatile String preparedFile = "";

    public UpdateManager(DuelTimePlugin plugin) {
        this.plugin = plugin;
    }

    public void checkOnStartup() {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null || !cfg.isUpdaterEnabled() || !cfg.isUpdaterCheckOnStartup()) {
            return;
        }
        checkAsync(null, false, true);
    }

    public void checkNow(CommandSender sender) {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null || !cfg.isUpdaterEnabled()) {
            DynamicLang.send(sender, true,
                    "Dynamic.updater.disabled",
                    "&c自動更新功能目前已停用。");
            return;
        }
        checkAsync(sender, false, false);
    }

    public void downloadNow(CommandSender sender) {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null || !cfg.isUpdaterEnabled()) {
            DynamicLang.send(sender, true,
                    "Dynamic.updater.disabled",
                    "&c自動更新功能目前已停用。");
            return;
        }
        checkAsync(sender, true, false);
    }

    public void sendStatus(CommandSender sender) {
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.separator",
                "&8&m───────────────────────────────────");
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.title",
                "&a更新狀態面板");
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.current",
                "&7目前版本：&f{version}",
                "version", plugin.getDescription().getVersion());
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.latest",
                "&7最新版本：&f{version}",
                "version", lastLatestVersion == null || lastLatestVersion.isBlank() ? "-" : lastLatestVersion);
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.source",
                "&7檢查來源：&f{source}",
                "source", lastSource == null || lastSource.isBlank() ? "-" : lastSource);
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.last-check",
                "&7上次檢查：&f{time}",
                "time", formatLastCheckTime());
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.update-available",
                "&7可更新版本：&f{value}",
                "value", updateAvailable ? "true" : "false");
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.checking",
                "&7檢查中：&f{value}",
                "value", checking.get() ? "true" : "false");
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.downloading",
                "&7下載中：&f{value}",
                "value", downloading.get() ? "true" : "false");
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.prepared",
                "&7已準備更新：&f{value}",
                "value", updatePrepared ? "true" : "false");
        if (updatePrepared) {
            DynamicLang.send(sender, true,
                    "Dynamic.updater.status.prepared-version",
                    "&7已下載版本：&f{version}",
                    "version", preparedVersion);
            DynamicLang.send(sender, true,
                    "Dynamic.updater.status.prepared-file",
                    "&7更新檔案：&f{file}",
                    "file", preparedFile);
        }
        if (lastError != null && !lastError.isBlank()) {
            DynamicLang.send(sender, true,
                    "Dynamic.updater.status.last-error",
                    "&7上次錯誤：&c{error}",
                    "error", lastError);
        }
        DynamicLang.send(sender, true,
                "Dynamic.updater.status.separator",
                "&8&m───────────────────────────────────");
    }

    private void checkAsync(CommandSender sender, boolean forceDownload, boolean startupMode) {
        if (!checking.compareAndSet(false, true)) {
            sendSync(sender,
                    "Dynamic.updater.checking-busy",
                    "&e目前已有更新檢查正在進行中。");
            return;
        }

        sendSync(sender,
                "Dynamic.updater.checking",
                "&7正在檢查新版本，請稍候...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UpdateInfo latest = fetchLatestInfo();
                String currentVersion = plugin.getDescription().getVersion();
                boolean newer = isNewerVersion(currentVersion, latest.version);

                lastCheckedAt = System.currentTimeMillis();
                lastLatestVersion = latest.version;
                lastSource = latest.source;
                lastError = "";
                updateAvailable = newer;

                if (!newer) {
                    if (startupMode) {
                        plugin.getLogger().info("[Updater] You are already on latest version: " + currentVersion);
                    } else {
                        sendSync(sender,
                                "Dynamic.updater.up-to-date",
                                "&a目前已是最新版本：&f{version}",
                                "version", currentVersion);
                    }
                    return;
                }

                if (startupMode) {
                    plugin.getLogger().info("[Updater] New version found: current=" + currentVersion + ", latest=" + latest.version);
                } else {
                    sendSync(sender,
                            "Dynamic.updater.new-version",
                            "&6發現新版本：&f{current} &8-> &a{latest}",
                            "current", currentVersion,
                            "latest", latest.version);
                    if (!latest.notes.isEmpty()) {
                        sendSync(sender,
                                "Dynamic.updater.notes-title",
                                "&7更新重點：");
                        int max = Math.min(5, latest.notes.size());
                        for (int i = 0; i < max; i++) {
                            sendSync(sender,
                                    "Dynamic.updater.note-line",
                                    "&8  - &f{line}",
                                    "line", latest.notes.get(i));
                        }
                    }
                }

                boolean shouldDownload = forceDownload || (startupMode && plugin.getCfgManager().isUpdaterAutoDownload());
                if (shouldDownload) {
                    DownloadResult result = downloadUpdate(latest);
                    if (result.success) {
                        if (startupMode) {
                            plugin.getLogger().info("[Updater] Update downloaded: " + result.fileName + ". It will be applied after restart.");
                        } else {
                            sendSync(sender,
                                    "Dynamic.updater.download-success",
                                    "&a更新檔下載完成：&f{file}&a，重啟伺服器後會自動套用。",
                                    "file", result.fileName);
                        }
                    } else {
                        if (startupMode) {
                            plugin.getLogger().warning("[Updater] Auto download failed: " + result.errorMessage);
                        } else {
                            sendSync(sender,
                                    "Dynamic.updater.download-failed",
                                    "&c下載更新失敗：&f{reason}",
                                    "reason", result.errorMessage);
                        }
                    }
                } else if (!startupMode) {
                    sendSync(sender,
                            "Dynamic.updater.download-hint",
                            "&7可使用 &f/dueltime update download &7自動下載更新檔。");
                }
            } catch (Exception e) {
                lastCheckedAt = System.currentTimeMillis();
                lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (startupMode) {
                    plugin.getLogger().warning("[Updater] Check failed: " + lastError);
                } else {
                    sendSync(sender,
                            "Dynamic.updater.check-failed",
                            "&c檢查更新失敗：&f{reason}",
                            "reason", lastError);
                }
            } finally {
                checking.set(false);
            }
        });
    }

    private UpdateInfo fetchLatestInfo() throws Exception {
        CfgManager cfg = plugin.getCfgManager();
        Exception manifestException = null;

        String manifestUrl = safeTrim(cfg.getUpdaterManifestUrl());
        if (!manifestUrl.isEmpty()) {
            try {
                return fetchFromManifest(manifestUrl);
            } catch (Exception e) {
                manifestException = e;
            }
        }

        String fallbackUrl = safeTrim(cfg.getUpdaterFallbackVersionUrl());
        if (!fallbackUrl.isEmpty()) {
            try {
                return fetchFromLegacyVersionFile(fallbackUrl);
            } catch (Exception e) {
                if (manifestException != null) {
                    throw new Exception("manifest: " + manifestException.getMessage() + " | fallback: " + e.getMessage(), e);
                }
                throw e;
            }
        }

        if (manifestException != null) {
            throw manifestException;
        }
        throw new IllegalStateException("No updater URL configured.");
    }

    private UpdateInfo fetchFromManifest(String manifestUrl) throws Exception {
        String body = readRemoteText(manifestUrl);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(body);

        String version = safeTrim(yaml.getString("version"));
        if (version.isEmpty()) {
            throw new IllegalStateException("Manifest missing 'version'.");
        }

        String downloadUrl = safeTrim(yaml.getString("download-url"));
        if (downloadUrl.isEmpty()) {
            downloadUrl = buildDownloadUrlFromTemplate(version);
        }

        String sha256 = safeTrim(yaml.getString("sha256")).toLowerCase(Locale.ROOT);
        List<String> notes = yaml.getStringList("notes");
        if (notes == null) {
            notes = new ArrayList<>();
        }

        return new UpdateInfo(version, downloadUrl, sha256, notes, "manifest");
    }

    private UpdateInfo fetchFromLegacyVersionFile(String legacyUrl) throws Exception {
        List<String> lines = readRemoteLines(legacyUrl);
        if (lines.isEmpty()) {
            throw new IllegalStateException("Legacy version file is empty.");
        }

        String version = safeTrim(lines.get(0));
        if (version.isEmpty()) {
            throw new IllegalStateException("Legacy version file has empty version line.");
        }

        String downloadUrl = "";
        String sha256 = "";
        List<String> notes = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String raw = safeTrim(lines.get(i));
            if (raw.isEmpty()) {
                continue;
            }

            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.startsWith("download-url:")) {
                downloadUrl = safeTrim(raw.substring("download-url:".length()));
                continue;
            }
            if (lower.startsWith("sha256:")) {
                sha256 = safeTrim(raw.substring("sha256:".length())).toLowerCase(Locale.ROOT);
                continue;
            }
            if (lower.equals("notes:")) {
                continue;
            }

            if (raw.startsWith("- ")) {
                raw = safeTrim(raw.substring(2));
            }
            if (!raw.isEmpty()) {
                notes.add(raw);
            }
        }

        if (downloadUrl.isEmpty()) {
            downloadUrl = buildDownloadUrlFromTemplate(version);
        }
        return new UpdateInfo(version, downloadUrl, sha256, notes, "legacy-version");
    }

    private DownloadResult downloadUpdate(UpdateInfo info) {
        if (!downloading.compareAndSet(false, true)) {
            return DownloadResult.failed("Download is already in progress.");
        }

        Path tempFile = null;
        try {
            if (info.downloadUrl == null || info.downloadUrl.isBlank()) {
                return DownloadResult.failed("No download URL in manifest or template.");
            }

            Path updaterDataDir = plugin.getDataFolder().toPath().resolve("updater");
            Files.createDirectories(updaterDataDir);
            tempFile = updaterDataDir.resolve("download-" + info.version + ".jar.tmp");

            HttpURLConnection connection = openConnection(info.downloadUrl);
            connection.setInstanceFollowRedirects(true);

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            if (plugin.getCfgManager().isUpdaterVerifySha256() && info.sha256 != null && !info.sha256.isBlank()) {
                String actualSha = computeSha256(tempFile);
                if (!actualSha.equalsIgnoreCase(info.sha256)) {
                    return DownloadResult.failed("SHA-256 mismatch. expected=" + info.sha256 + ", actual=" + actualSha);
                }
            }

            Path updateFolder = resolveUpdateFolder();
            Files.createDirectories(updateFolder);

            String fileName = resolveTargetFileName(info);
            Path target = updateFolder.resolve(fileName);
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ignored) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }

            updatePrepared = true;
            preparedVersion = info.version;
            preparedFile = target.getFileName().toString();
            return DownloadResult.success(preparedFile);
        } catch (Exception e) {
            return DownloadResult.failed(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            downloading.set(false);
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Path resolveUpdateFolder() {
        try {
            if (plugin.getServer().getUpdateFolderFile() != null) {
                return plugin.getServer().getUpdateFolderFile().toPath();
            }
        } catch (Exception ignored) {
        }
        return plugin.getDataFolder().toPath().resolve("update");
    }

    private String resolveTargetFileName(UpdateInfo info) {
        try {
            String path = new URL(info.downloadUrl).getPath();
            int slashIndex = path.lastIndexOf('/');
            if (slashIndex >= 0 && slashIndex + 1 < path.length()) {
                String fromUrl = path.substring(slashIndex + 1).trim();
                if (!fromUrl.isEmpty() && fromUrl.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    return fromUrl;
                }
            }
        } catch (Exception ignored) {
        }
        return "Dueltime4-Bukkit-" + info.version + ".jar";
    }

    private String computeSha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String readRemoteText(String url) throws Exception {
        HttpURLConnection connection = openConnection(url);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private List<String> readRemoteLines(String url) throws Exception {
        HttpURLConnection connection = openConnection(url);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private HttpURLConnection openConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setConnectTimeout(plugin.getCfgManager().getUpdaterConnectTimeoutMs());
        connection.setReadTimeout(plugin.getCfgManager().getUpdaterReadTimeoutMs());
        return connection;
    }

    private String buildDownloadUrlFromTemplate(String version) {
        String template = safeTrim(plugin.getCfgManager().getUpdaterDownloadUrlTemplate());
        if (template.isEmpty()) {
            return "";
        }
        return template.replace("{version}", version);
    }

    private String formatLastCheckTime() {
        if (lastCheckedAt <= 0) {
            return "-";
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastCheckedAt), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void sendSync(CommandSender sender, String key, String fallback, String... replacements) {
        if (sender == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () ->
                DynamicLang.send(sender, true, key, fallback, replacements));
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

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private static class UpdateInfo {
        private final String version;
        private final String downloadUrl;
        private final String sha256;
        private final List<String> notes;
        private final String source;

        private UpdateInfo(String version, String downloadUrl, String sha256, List<String> notes, String source) {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
            this.notes = notes;
            this.source = source;
        }
    }

    private static class DownloadResult {
        private final boolean success;
        private final String fileName;
        private final String errorMessage;

        private DownloadResult(boolean success, String fileName, String errorMessage) {
            this.success = success;
            this.fileName = fileName;
            this.errorMessage = errorMessage;
        }

        private static DownloadResult success(String fileName) {
            return new DownloadResult(true, fileName, "");
        }

        private static DownloadResult failed(String errorMessage) {
            return new DownloadResult(false, "", errorMessage == null ? "unknown error" : errorMessage);
        }
    }
}
