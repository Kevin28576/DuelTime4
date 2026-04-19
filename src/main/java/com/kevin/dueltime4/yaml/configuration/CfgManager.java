package com.kevin.dueltime4.yaml.configuration;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ClassicArena;
import com.kevin.dueltime4.yaml.message.MsgManager;
import com.google.common.base.Charsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class CfgManager {
    public CfgManager() {
        reload();
    }

    private File file;
    private FileConfiguration config;

    public FileConfiguration getConfig() {
        return config;
    }

    public void reload() {
        config = new YamlConfiguration();
        file = new File(DuelTimePlugin.getInstance().getDataFolder(), "config.yml");
        if (!file.exists()) {
            DuelTimePlugin.getInstance().saveResource("config.yml", false);
        }
        try {
            config.load(new BufferedReader(new InputStreamReader(
                    Files.newInputStream(file.toPath()), Charsets.UTF_8)));
        } catch (Exception e) {
            try {
                config.load(new BufferedReader(new InputStreamReader(
                        Files.newInputStream(file.toPath()))));
            } catch (Exception ex) {
                ex.printStackTrace();
                // 提示後臺無法載入
            }
        }
        prefix = config.getString("Message.prefix").replace("&", "§");
        MsgManager msgManager = DuelTimePlugin.getInstance().getMsgManager();
        if (msgManager != null) {
            msgManager.updatePrefix(prefix);
        }
        defaultLanguage = config.getString("Message.default-language");
        boolean hasDefaultLanguageEntered = false;
        if (new File(DuelTimePlugin.getInstance().getDataFolder(), "languages").exists()) {
            for (File languageFile : new File(DuelTimePlugin.getInstance().getDataFolder(), "languages").listFiles()) {
                if (languageFile.getName().equals(defaultLanguage + ".yml") || languageFile.getName().equals(defaultLanguage + ".yaml")) {
                    hasDefaultLanguageEntered = true;
                    break;
                }
            }
        }
        if (!hasDefaultLanguageEntered) defaultLanguage = null;
        updaterEnabled = config.getBoolean("Updater.enabled", true);
        updaterCheckOnStartup = config.getBoolean("Updater.check-on-startup", true);
        updaterAutoDownload = config.getBoolean("Updater.auto-download", false);
        updaterVerifySha256 = config.getBoolean("Updater.verify-sha256", true);
        updaterManifestUrl = config.getString(
                "Updater.manifest-url",
                "https://raw.githubusercontent.com/Kevin28576/DuelTime4/refs/heads/main/src/main/java/com/kevin/dueltime4/network/update.yml");
        updaterFallbackVersionUrl = config.getString(
                "Updater.fallback-version-url",
                "https://raw.githubusercontent.com/Kevin28576/DuelTime4/refs/heads/main/src/main/java/com/kevin/dueltime4/network/version");
        updaterDownloadUrlTemplate = config.getString(
                "Updater.download-url-template",
                "https://github.com/Kevin28576/DuelTime4/releases/download/v{version}/Dueltime4-Bukkit-{version}.jar");
        updaterConnectTimeoutMs = config.getInt("Updater.connect-timeout-ms", 5000);
        updaterReadTimeoutMs = config.getInt("Updater.read-timeout-ms", 5000);
        if (updaterConnectTimeoutMs < 1000) updaterConnectTimeoutMs = 1000;
        if (updaterReadTimeoutMs < 1000) updaterReadTimeoutMs = 1000;
        discordWebhookEnabled = config.getBoolean("Network.discord-webhook.enabled", false);
        discordWebhookUrl = config.getString("Network.discord-webhook.url", "");
        discordWebhookUsername = config.getString("Network.discord-webhook.username", "DuelTime4");
        discordWebhookAvatarUrl = config.getString("Network.discord-webhook.avatar-url", "");
        discordWebhookConnectTimeoutMs = config.getInt("Network.discord-webhook.connect-timeout-ms", 5000);
        discordWebhookReadTimeoutMs = config.getInt("Network.discord-webhook.read-timeout-ms", 5000);
        if (discordWebhookConnectTimeoutMs < 1000) discordWebhookConnectTimeoutMs = 1000;
        if (discordWebhookReadTimeoutMs < 1000) discordWebhookReadTimeoutMs = 1000;
        discordWebhookBattleReportEnabled = config.getBoolean("Network.discord-webhook.battle-report.enabled", true);
        discordWebhookBattleReportIncludeDraw = config.getBoolean("Network.discord-webhook.battle-report.include-draw", true);
        discordWebhookLeavePenaltyEnabled = config.getBoolean("Network.discord-webhook.leave-penalty.enabled", true);
        arenaClassicRewardWinExp = config.getDouble("Arena.classic.reward.win-exp");
        arenaClassicRewardWinPoint = config.getDouble("Arena.classic.reward.win-point");
        arenaClassicRewardLoseExpRate = config.getDouble("Arena.classic.reward.lose-exp-rate");
        if (arenaClassicRewardLoseExpRate < 0) arenaClassicRewardLoseExpRate = 0;
        arenaClassicAutoRespawnEnabled = config.getBoolean("Arena.classic.auto-respawn.enabled");
        arenaClassicAutoRespawnCode = config.getString("Arena.classic.auto-respawn.code");
        if (!arenaClassicAutoRespawnCode.equalsIgnoreCase(ClassicArena.RespawnCode.SPIGOT.name()) && !arenaClassicAutoRespawnCode.equalsIgnoreCase(ClassicArena.RespawnCode.SETHEALTH.name())) {
            arenaClassicAutoRespawnCode = ClassicArena.RespawnCode.SPIGOT.name();
        }
        arenaClassicDelayedBackEnabled = config.getBoolean("Arena.classic.delayed-back.enabled");
        arenaClassicDelayedBackTime = config.getInt("Arena.classic.delayed-back.time");
        if (arenaClassicDelayedBackTime < 2) arenaClassicDelayedBackTime = 2;
        arenaClassicStreakEnabled = config.getBoolean("Arena.classic.streak.enabled", true);
        arenaClassicStreakShowMessage = config.getBoolean("Arena.classic.streak.show-message", true);
        arenaClassicStreakResetOnDraw = config.getBoolean("Arena.classic.streak.reset-on-draw", true);
        arenaClassicMatchConfirmTimeout = config.getInt("Arena.classic.matchmaking.confirm-timeout", 15);
        if (arenaClassicMatchConfirmTimeout < 5) arenaClassicMatchConfirmTimeout = 5;
        arenaClassicQueueSoundEnabled = config.getBoolean("Arena.classic.matchmaking.queue-sound.enabled", true);
        arenaClassicQueueSoundAllowPlayerToggle = config.getBoolean("Arena.classic.matchmaking.queue-sound.allow-player-toggle", true);
        arenaClassicQueueSoundIntervalSeconds = config.getInt("Arena.classic.matchmaking.queue-sound.interval-seconds", 5);
        if (arenaClassicQueueSoundIntervalSeconds < 1) arenaClassicQueueSoundIntervalSeconds = 1;
        arenaClassicQueueSoundName = config.getString("Arena.classic.matchmaking.queue-sound.sound", "BLOCK_NOTE_BLOCK_PLING");
        arenaClassicQueueSoundVolume = config.getDouble("Arena.classic.matchmaking.queue-sound.volume", 1.0D);
        if (arenaClassicQueueSoundVolume < 0D) arenaClassicQueueSoundVolume = 0D;
        if (arenaClassicQueueSoundVolume > 5D) arenaClassicQueueSoundVolume = 5D;
        arenaClassicQueueSoundPitch = config.getDouble("Arena.classic.matchmaking.queue-sound.pitch", 1.2D);
        if (arenaClassicQueueSoundPitch < 0.1D) arenaClassicQueueSoundPitch = 0.1D;
        if (arenaClassicQueueSoundPitch > 2D) arenaClassicQueueSoundPitch = 2D;
        arenaClassicMatchWatchdogEnabled = config.getBoolean("Arena.classic.matchmaking.watchdog.enabled", true);
        arenaClassicMatchWatchdogIntervalSeconds = config.getInt("Arena.classic.matchmaking.watchdog.interval-seconds", 3);
        if (arenaClassicMatchWatchdogIntervalSeconds < 1) arenaClassicMatchWatchdogIntervalSeconds = 1;
        arenaClassicMatchWatchdogCleanupOfflinePlayers = config.getBoolean(
                "Arena.classic.matchmaking.watchdog.cleanup-offline-players", true);
        arenaClassicMatchWatchdogCleanupInvalidArena = config.getBoolean(
                "Arena.classic.matchmaking.watchdog.cleanup-invalid-arena", true);
        arenaClassicMatchWatchdogTriggerMatchCheck = config.getBoolean(
                "Arena.classic.matchmaking.watchdog.trigger-match-check", true);
        arenaClassicLeavePenaltyEnabled = config.getBoolean("Arena.classic.matchmaking.leave-penalty.enabled",
                config.getDouble("Arena.classic.matchmaking.leave-penalty.point",
                        config.getDouble("Arena.classic.matchmaking.leave-penalty-point", 0)) > 0
                        || config.getInt("Arena.classic.matchmaking.leave-penalty.cooldown",
                        config.getInt("Arena.classic.matchmaking.leave-penalty-cooldown", 0)) > 0);
        arenaClassicLeavePenaltyApplyOnQuitCommand = config.getBoolean(
                "Arena.classic.matchmaking.leave-penalty.apply-on-quit-command", true);
        arenaClassicLeavePenaltyApplyOnDisconnect = config.getBoolean(
                "Arena.classic.matchmaking.leave-penalty.apply-on-disconnect", true);
        arenaClassicLeavePenaltyApplyPointDeduction = config.getBoolean(
                "Arena.classic.matchmaking.leave-penalty.apply-point-deduction", true);
        arenaClassicLeavePenaltyApplyQueueCooldown = config.getBoolean(
                "Arena.classic.matchmaking.leave-penalty.apply-queue-cooldown", true);
        arenaClassicLeavePenaltyPoint = config.getDouble("Arena.classic.matchmaking.leave-penalty.point",
                config.getDouble("Arena.classic.matchmaking.leave-penalty-point", 0));
        if (arenaClassicLeavePenaltyPoint < 0) arenaClassicLeavePenaltyPoint = 0;
        arenaClassicLeavePenaltyCooldown = config.getInt("Arena.classic.matchmaking.leave-penalty.cooldown",
                config.getInt("Arena.classic.matchmaking.leave-penalty-cooldown", 0));
        if (arenaClassicLeavePenaltyCooldown < 0) arenaClassicLeavePenaltyCooldown = 0;
        restartProtectionEnabled = config.getBoolean("System.restart-protection.enabled", true);
        restartProtectionBroadcastMessage = config.getBoolean("System.restart-protection.broadcast-message", true);
        recordShowEnabled = config.getBoolean("Record.show.enabled");
        recordShowCooldown = config.getInt("Record.show.cooldown");
        if (recordShowCooldown < 0) recordShowCooldown = 0;
        recordPrintEnabled = config.getBoolean("Record.print.enabled");
        recordPrintCost = config.getDouble("Record.print.cost");
        if (recordPrintCost < 0) recordPrintCost = 0;
        rankingAutoRefreshInterval = config.getInt("Ranking.auto-refresh-interval");
        if (rankingAutoRefreshInterval < 5) rankingAutoRefreshInterval = 5;
        tierTitleShowedInChatBoxEnabled = config.getBoolean("Level.tier.showed-in-chat-box.enabled");
        tierTitleShowedInChatBoxFormat = config.getString("Level.tier.showed-in-chat-box.format").replace("&", "§");
    }

    private String prefix;
    private String defaultLanguage;
    private boolean updaterEnabled;
    private boolean updaterCheckOnStartup;
    private boolean updaterAutoDownload;
    private boolean updaterVerifySha256;
    private String updaterManifestUrl;
    private String updaterFallbackVersionUrl;
    private String updaterDownloadUrlTemplate;
    private int updaterConnectTimeoutMs;
    private int updaterReadTimeoutMs;
    private boolean discordWebhookEnabled;
    private String discordWebhookUrl;
    private String discordWebhookUsername;
    private String discordWebhookAvatarUrl;
    private int discordWebhookConnectTimeoutMs;
    private int discordWebhookReadTimeoutMs;
    private boolean discordWebhookBattleReportEnabled;
    private boolean discordWebhookBattleReportIncludeDraw;
    private boolean discordWebhookLeavePenaltyEnabled;
    private double arenaClassicRewardWinExp;
    private double arenaClassicRewardWinPoint;
    private double arenaClassicRewardLoseExpRate;
    private boolean arenaClassicAutoRespawnEnabled;
    private String arenaClassicAutoRespawnCode;
    private boolean arenaClassicDelayedBackEnabled;
    private int arenaClassicDelayedBackTime;
    private boolean arenaClassicStreakEnabled;
    private boolean arenaClassicStreakShowMessage;
    private boolean arenaClassicStreakResetOnDraw;
    private int arenaClassicMatchConfirmTimeout;
    private boolean arenaClassicQueueSoundEnabled;
    private boolean arenaClassicQueueSoundAllowPlayerToggle;
    private int arenaClassicQueueSoundIntervalSeconds;
    private String arenaClassicQueueSoundName;
    private double arenaClassicQueueSoundVolume;
    private double arenaClassicQueueSoundPitch;
    private boolean arenaClassicMatchWatchdogEnabled;
    private int arenaClassicMatchWatchdogIntervalSeconds;
    private boolean arenaClassicMatchWatchdogCleanupOfflinePlayers;
    private boolean arenaClassicMatchWatchdogCleanupInvalidArena;
    private boolean arenaClassicMatchWatchdogTriggerMatchCheck;
    private boolean arenaClassicLeavePenaltyEnabled;
    private boolean arenaClassicLeavePenaltyApplyOnQuitCommand;
    private boolean arenaClassicLeavePenaltyApplyOnDisconnect;
    private boolean arenaClassicLeavePenaltyApplyPointDeduction;
    private boolean arenaClassicLeavePenaltyApplyQueueCooldown;
    private double arenaClassicLeavePenaltyPoint;
    private int arenaClassicLeavePenaltyCooldown;
    private boolean restartProtectionEnabled;
    private boolean restartProtectionBroadcastMessage;
    private boolean recordShowEnabled;
    private int recordShowCooldown;
    private boolean recordPrintEnabled;
    private double recordPrintCost;
    private int rankingAutoRefreshInterval;
    private boolean tierTitleShowedInChatBoxEnabled;
    private String tierTitleShowedInChatBoxFormat;

    public String getPrefix() {
        return prefix;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isUpdaterEnabled() {
        return updaterEnabled;
    }

    public boolean isUpdaterCheckOnStartup() {
        return updaterCheckOnStartup;
    }

    public boolean isUpdaterAutoDownload() {
        return updaterAutoDownload;
    }

    public boolean isUpdaterVerifySha256() {
        return updaterVerifySha256;
    }

    public String getUpdaterManifestUrl() {
        return updaterManifestUrl;
    }

    public String getUpdaterFallbackVersionUrl() {
        return updaterFallbackVersionUrl;
    }

    public String getUpdaterDownloadUrlTemplate() {
        return updaterDownloadUrlTemplate;
    }

    public int getUpdaterConnectTimeoutMs() {
        return updaterConnectTimeoutMs;
    }

    public int getUpdaterReadTimeoutMs() {
        return updaterReadTimeoutMs;
    }

    public boolean isDiscordWebhookEnabled() {
        return discordWebhookEnabled;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public String getDiscordWebhookUsername() {
        return discordWebhookUsername;
    }

    public String getDiscordWebhookAvatarUrl() {
        return discordWebhookAvatarUrl;
    }

    public int getDiscordWebhookConnectTimeoutMs() {
        return discordWebhookConnectTimeoutMs;
    }

    public int getDiscordWebhookReadTimeoutMs() {
        return discordWebhookReadTimeoutMs;
    }

    public boolean isDiscordWebhookBattleReportEnabled() {
        return discordWebhookBattleReportEnabled;
    }

    public boolean isDiscordWebhookBattleReportIncludeDraw() {
        return discordWebhookBattleReportIncludeDraw;
    }

    public boolean isDiscordWebhookLeavePenaltyEnabled() {
        return discordWebhookLeavePenaltyEnabled;
    }

    public double getArenaClassicRewardWinExp() {
        return arenaClassicRewardWinExp;
    }

    public double getArenaClassicRewardWinPoint() {
        return arenaClassicRewardWinPoint;
    }

    public double getArenaClassicRewardLoseExpRate() {
        return arenaClassicRewardLoseExpRate;
    }

    public boolean isArenaClassicAutoRespawnEnabled() {
        return arenaClassicAutoRespawnEnabled;
    }

    public String getArenaClassicAutoRespawnCode() {
        return arenaClassicAutoRespawnCode;
    }

    public boolean isArenaClassicDelayedBackEnabled() {
        return arenaClassicDelayedBackEnabled;
    }

    public int getArenaClassicDelayedBackTime() {
        return arenaClassicDelayedBackTime;
    }

    public boolean isArenaClassicStreakEnabled() {
        return arenaClassicStreakEnabled;
    }

    public boolean isArenaClassicStreakShowMessage() {
        return arenaClassicStreakShowMessage;
    }

    public boolean isArenaClassicStreakResetOnDraw() {
        return arenaClassicStreakResetOnDraw;
    }

    public int getArenaClassicMatchConfirmTimeout() {
        return arenaClassicMatchConfirmTimeout;
    }

    public boolean isArenaClassicQueueSoundEnabled() {
        return arenaClassicQueueSoundEnabled;
    }

    public boolean isArenaClassicQueueSoundAllowPlayerToggle() {
        return arenaClassicQueueSoundAllowPlayerToggle;
    }

    public int getArenaClassicQueueSoundIntervalSeconds() {
        return arenaClassicQueueSoundIntervalSeconds;
    }

    public String getArenaClassicQueueSoundName() {
        return arenaClassicQueueSoundName;
    }

    public double getArenaClassicQueueSoundVolume() {
        return arenaClassicQueueSoundVolume;
    }

    public double getArenaClassicQueueSoundPitch() {
        return arenaClassicQueueSoundPitch;
    }

    public boolean isArenaClassicMatchWatchdogEnabled() {
        return arenaClassicMatchWatchdogEnabled;
    }

    public int getArenaClassicMatchWatchdogIntervalSeconds() {
        return arenaClassicMatchWatchdogIntervalSeconds;
    }

    public boolean isArenaClassicMatchWatchdogCleanupOfflinePlayers() {
        return arenaClassicMatchWatchdogCleanupOfflinePlayers;
    }

    public boolean isArenaClassicMatchWatchdogCleanupInvalidArena() {
        return arenaClassicMatchWatchdogCleanupInvalidArena;
    }

    public boolean isArenaClassicMatchWatchdogTriggerMatchCheck() {
        return arenaClassicMatchWatchdogTriggerMatchCheck;
    }

    public boolean isArenaClassicLeavePenaltyEnabled() {
        return arenaClassicLeavePenaltyEnabled;
    }

    public boolean isArenaClassicLeavePenaltyApplyOnQuitCommand() {
        return arenaClassicLeavePenaltyApplyOnQuitCommand;
    }

    public boolean isArenaClassicLeavePenaltyApplyOnDisconnect() {
        return arenaClassicLeavePenaltyApplyOnDisconnect;
    }

    public boolean isArenaClassicLeavePenaltyApplyPointDeduction() {
        return arenaClassicLeavePenaltyApplyPointDeduction;
    }

    public boolean isArenaClassicLeavePenaltyApplyQueueCooldown() {
        return arenaClassicLeavePenaltyApplyQueueCooldown;
    }

    public double getArenaClassicLeavePenaltyPoint() {
        return arenaClassicLeavePenaltyPoint;
    }

    public int getArenaClassicLeavePenaltyCooldown() {
        return arenaClassicLeavePenaltyCooldown;
    }

    public boolean isRestartProtectionEnabled() {
        return restartProtectionEnabled;
    }

    public boolean isRestartProtectionBroadcastMessage() {
        return restartProtectionBroadcastMessage;
    }

    public boolean isRecordShowEnabled() {
        return recordShowEnabled;
    }

    public int getRecordShowCooldown() {
        return recordShowCooldown;
    }

    public boolean isRecordPrintEnabled() {
        return recordPrintEnabled;
    }

    public double getRecordPrintCost() {
        return recordPrintCost;
    }

    public int getRankingAutoRefreshInterval() {
        return rankingAutoRefreshInterval;
    }

    public boolean isTierTitleShowedInChatBoxEnabled() {
        return tierTitleShowedInChatBoxEnabled;
    }

    public String getTierTitleShowedInChatBoxFormat() {
        return tierTitleShowedInChatBoxFormat;
    }

    public void save() {
        if (file == null) {
            return;
        }
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
