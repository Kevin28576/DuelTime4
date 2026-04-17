package com.kevin.dueltime4;

import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.type.ArenaTypeManager;
import com.kevin.dueltime4.cache.CacheManager;
import com.kevin.dueltime4.command.CommandHandler;
import com.kevin.dueltime4.data.MyBatisManager;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import com.kevin.dueltime4.hook.DuelTimeExpansion;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.listener.ListenerManager;
import com.kevin.dueltime4.network.VersionChecker;
import com.kevin.dueltime4.progress.ProgressManager;
import com.kevin.dueltime4.ranking.RankingManager;
import com.kevin.dueltime4.ranking.hologram.HologramManager;
import com.kevin.dueltime4.request.RequestReceiverManager;
import com.kevin.dueltime4.stats.Metrics;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.MsgManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.regex.Pattern;

public final class DuelTimePlugin extends JavaPlugin {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private static DuelTimePlugin instance;
    public static String serverVersion;
    public static int serverVersionInt;
    private CfgManager cfgManager;
    private MsgManager msgManager;
    private MyBatisManager myBatisManager;
    private CacheManager cacheManager;
    private ArenaTypeManager arenaTypeManager;
    private ArenaManager arenaManager;
    private CommandHandler commandHandler;
    private ProgressManager progressManager;
    private CustomInventoryManager customInventoryManager;
    private RequestReceiverManager requestReceiverManager;
    private LevelManager levelManager;
    private RankingManager rankingManager;
    private HologramManager hologramManager;
    private Metrics metrics;
    private VersionChecker versionChecker;

    @Override
    public void onEnable() {
        long enableStart = System.currentTimeMillis();
        instance = this;

        ListenerManager.register();

        cfgManager = new CfgManager();
        levelManager = new LevelManager();
        msgManager = new MsgManager();
        logInfo("Config file successfully loaded.");
        logSuccess(buildLanguageSummary());

        myBatisManager = new MyBatisManager();
        rankingManager = new RankingManager();
        hologramManager = new HologramManager();
        logSuccess("Database and ranking services successfully initialized.");

        cacheManager = new CacheManager();
        cacheManager.load();
        arenaTypeManager = new ArenaTypeManager();
        arenaManager = new ArenaManager();
        logSuccess("Arena data successfully loaded. Arenas: " + arenaManager.size() + ", Types: " + arenaTypeManager.getList().size() + ".");

        commandHandler = new CommandHandler();
        progressManager = new ProgressManager();
        customInventoryManager = new CustomInventoryManager();
        requestReceiverManager = new RequestReceiverManager();
        logSuccess("Command and GUI systems successfully initialized.");

        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] packageParts = packageName.split("\\.");
        serverVersion = "";
        if (packageParts.length >= 4 && packageParts[3].startsWith("v")) {
            serverVersion = packageParts[3];
        }
        serverVersionInt = resolveServerVersionInt();
        if (serverVersionInt == 8 || serverVersionInt == 9) {
            ViaVersion.getClassesForTitleAndAction();
        }
        String detectedVersionName = serverVersion.isEmpty() ? "modern-package-layout" : serverVersion;
        logInfo("Server compatibility check completed. Version: " + resolveMinecraftVersionString() + " (nms=" + detectedVersionName + ", parsed=" + serverVersionInt + ").");

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DuelTimeExpansion(this).register();
            logSuccess("PlaceholderAPI hook was successfully registered!");
        } else {
            logWarn("PlaceholderAPI not found, placeholder hook skipped.");
        }

        metrics = new Metrics(this, 30767);
        versionChecker = new VersionChecker();
        logSuccess("DuelTime4 has been enabled! (" + (System.currentTimeMillis() - enableStart) + "ms)");
    }

    @Override
    public void onDisable() {
        long disableStart = System.currentTimeMillis();
        logInfo("Disabling DuelTime4...");

        if (arenaManager != null) {
            int stoppedArenaCount = 0;
            for (BaseArena arena : arenaManager.getList()) {
                if (arena.getState().equals(BaseArena.State.IN_PROGRESS_OPENED) || arena.getState().equals(BaseArena.State.IN_PROGRESS_CLOSED)) {
                    arenaManager.stop(arena.getId(), null);
                    stoppedArenaCount++;
                }
            }
            if (stoppedArenaCount > 0) {
                logInfo("Stopped " + stoppedArenaCount + " active arena(s).");
            }
        }
        if (hologramManager != null) {
            hologramManager.disable();
            logInfo("Hologram manager disabled.");
        }
        if (cacheManager != null && cacheManager.getPlayerDataCache() != null && cacheManager.getPlayerDataCache().getRefreshRankingTimer() != null) {
            cacheManager.getPlayerDataCache().getRefreshRankingTimer().cancel();
            logInfo("Ranking refresh task cancelled.");
        }
        if (myBatisManager != null) {
            myBatisManager.closeConnection();
            logInfo("Database connections closed.");
        }
        if (progressManager != null) {
            progressManager.exitAll();
            logInfo("All active progress sessions cleared.");
        }

        logSuccess("DuelTime4 has been disabled! (" + (System.currentTimeMillis() - disableStart) + "ms)");
    }

    public static DuelTimePlugin getInstance() {
        return instance;
    }

    public CfgManager getCfgManager() {
        return cfgManager;
    }

    public MsgManager getMsgManager() {
        return msgManager;
    }

    public MyBatisManager getMyBatisManager() {
        return myBatisManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public ArenaTypeManager getArenaTypeManager() {
        return arenaTypeManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public ProgressManager getProgressManager() {
        return progressManager;
    }

    public CustomInventoryManager getCustomInventoryManager() {
        return customInventoryManager;
    }

    public RequestReceiverManager getRequestReceiverManager() {
        return requestReceiverManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public RankingManager getRankingManager() {
        return rankingManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public VersionChecker getVersionChecker() {
        return versionChecker;
    }

    private int resolveServerVersionInt() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            bukkitVersion = Bukkit.getVersion();
        }
        String normalizedVersion = bukkitVersion.split("-")[0];
        String[] versionParts = normalizedVersion.split("\\.");
        try {
            if (versionParts.length >= 2 && "1".equals(versionParts[0])) {
                return Integer.parseInt(versionParts[1]);
            }
            return Integer.parseInt(versionParts[0]);
        } catch (NumberFormatException e) {
            getLogger().warning("Cannot parse server version from: " + bukkitVersion + ", fallback to 21.");
            return 21;
        }
    }

    private String resolveMinecraftVersionString() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            bukkitVersion = Bukkit.getVersion();
        }
        return bukkitVersion.split("-")[0];
    }

    private void logInfo(String message) {
        getLogger().info(ANSI_CYAN + message + ANSI_RESET);
    }

    private void logSuccess(String message) {
        getLogger().info(ANSI_GREEN + message + ANSI_RESET);
    }

    private void logWarn(String message) {
        getLogger().warning(ANSI_YELLOW + message + ANSI_RESET);
    }

    private String buildLanguageSummary() {
        Map<String, YamlConfiguration> languageMap = msgManager.getLanguageYamlFileMap();
        String configuredLanguage = cfgManager.getConfig().getString("Message.default-language", "zh_tw");
        String activeLanguage = configuredLanguage;
        if (!languageMap.containsKey(activeLanguage)) {
            activeLanguage = "zh_tw";
        }
        if (!languageMap.containsKey(activeLanguage) && !languageMap.isEmpty()) {
            activeLanguage = languageMap.keySet().stream().sorted().findFirst().orElse("unknown");
        }
        YamlConfiguration activeLanguageConfig = languageMap.get(activeLanguage);
        String languageName = activeLanguageConfig != null
                ? stripConsoleColors(activeLanguageConfig.getString("LanguageName", activeLanguage))
                : "unknown";
        String author = activeLanguageConfig != null
                ? stripConsoleColors(activeLanguageConfig.getString("Author", "unknown"))
                : "unknown";
        return "Language file successfully loaded. Language: " + languageName
                + " Author: " + author
                + " Installed: " + languageMap.size();
    }

    private String stripConsoleColors(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }
        String sectionSymbol = "\u00A7";
        String normalized = text.replace("&", sectionSymbol).replace("\u79AE", sectionSymbol);
        return Pattern.compile("(?i)\u00A7[0-9A-FK-ORX]").matcher(normalized).replaceAll("").trim();
    }
}
