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

    private FileConfiguration config;

    public FileConfiguration getConfig() {
        return config;
    }

    public void reload() {
        config = new YamlConfiguration();
        File file = new File(DuelTimePlugin.getInstance().getDataFolder(), "config.yml");
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
    private double arenaClassicRewardWinExp;
    private double arenaClassicRewardWinPoint;
    private double arenaClassicRewardLoseExpRate;
    private boolean arenaClassicAutoRespawnEnabled;
    private String arenaClassicAutoRespawnCode;
    private boolean arenaClassicDelayedBackEnabled;
    private int arenaClassicDelayedBackTime;
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
}
