package com.kevin.dueltime4;

import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.QueueWatchdogService;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.arena.type.ArenaTypeManager;
import com.kevin.dueltime4.cache.CacheManager;
import com.kevin.dueltime4.command.CommandHandler;
import com.kevin.dueltime4.data.MyBatisManager;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import com.kevin.dueltime4.hook.DuelTimeExpansion;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.listener.ListenerManager;
import com.kevin.dueltime4.network.DiscordWebhookManager;
import com.kevin.dueltime4.network.UpdateManager;
import com.kevin.dueltime4.network.VersionChecker;
import com.kevin.dueltime4.progress.ProgressManager;
import com.kevin.dueltime4.ranking.RankingManager;
import com.kevin.dueltime4.ranking.hologram.HologramManager;
import com.kevin.dueltime4.request.RequestReceiverManager;
import com.kevin.dueltime4.stats.MatchStreakManager;
import com.kevin.dueltime4.stats.Metrics;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import com.kevin.dueltime4.yaml.message.MsgManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DuelTimePlugin extends JavaPlugin {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private static DuelTimePlugin instance;
    private static volatile boolean serverShuttingDown;
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
    private MatchStreakManager matchStreakManager;
    private LevelManager levelManager;
    private RankingManager rankingManager;
    private HologramManager hologramManager;
    private Metrics metrics;
    private VersionChecker versionChecker;
    private UpdateManager updateManager;
    private DiscordWebhookManager discordWebhookManager;
    private QueueWatchdogService queueWatchdogService;

    @Override
    public void onEnable() {
        long enableStart = System.currentTimeMillis();
        instance = this;
        serverShuttingDown = false;

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
        queueWatchdogService = new QueueWatchdogService(this);
        queueWatchdogService.start();
        logSuccess("Arena data successfully loaded. Arenas: " + arenaManager.size() + ", Types: " + arenaTypeManager.getList().size() + ".");

        commandHandler = new CommandHandler();
        progressManager = new ProgressManager();
        customInventoryManager = new CustomInventoryManager();
        requestReceiverManager = new RequestReceiverManager();
        matchStreakManager = new MatchStreakManager();
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
        updateManager = new UpdateManager(this);
        discordWebhookManager = new DiscordWebhookManager(this);
        updateManager.checkOnStartup();
        logSuccess("DuelTime4 has been enabled! (" + (System.currentTimeMillis() - enableStart) + "ms)");
    }

    @Override
    public void onDisable() {
        long disableStart = System.currentTimeMillis();
        serverShuttingDown = true;
        logInfo("Disabling DuelTime4...");

        if (cfgManager != null && cfgManager.isRestartProtectionEnabled()) {
            int activeArenaCount = 0;
            int waitingCount = 0;
            int pendingCount = 0;
            List<BaseArena> activeArenas = new ArrayList<>();
            if (arenaManager != null) {
                for (BaseArena arena : arenaManager.getList()) {
                    if (arena.getState().equals(BaseArena.State.IN_PROGRESS_OPENED) || arena.getState().equals(BaseArena.State.IN_PROGRESS_CLOSED)) {
                        activeArenaCount++;
                        activeArenas.add(arena);
                    }
                }
                waitingCount = arenaManager.getTotalWaitingCount();
                if (arenaManager.getQueueMatchConfirmManager() != null) {
                    pendingCount = arenaManager.getQueueMatchConfirmManager().getPendingArenaCount();
                }
            }
            logWarn("Restart protection enabled: leave penalties are temporarily disabled during shutdown.");
            if (cfgManager.isRestartProtectionBroadcastMessage()) {
                notifyActiveBattlePlayers(activeArenas);
                logInfo("Restart snapshot - active arenas: " + activeArenaCount
                        + ", waiting queue players: " + waitingCount
                        + ", pending confirmations: " + pendingCount + ".");
            }
        }

        if (arenaManager != null) {
            arenaManager.cancelAllPendingMatches();
            int stoppedArenaCount = arenaManager.stopAllInProgress("SERVER_SHUTDOWN");
            if (stoppedArenaCount > 0) {
                logInfo("Stopped " + stoppedArenaCount + " active arena(s).");
            }
        }
        if (queueWatchdogService != null) {
            queueWatchdogService.stop();
            logInfo("Queue watchdog stopped.");
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
        if (matchStreakManager != null) {
            matchStreakManager.clear();
            logInfo("Match streak cache cleared.");
        }

        logSuccess("DuelTime4 has been disabled! (" + (System.currentTimeMillis() - disableStart) + "ms)");
    }

    private void notifyActiveBattlePlayers(List<BaseArena> activeArenas) {
        if (activeArenas == null || activeArenas.isEmpty()) {
            return;
        }
        Set<String> notified = new HashSet<>();
        for (BaseArena arena : activeArenas) {
            for (BaseGamerData gamerData : arena.getGamerDataList()) {
                Player player = gamerData.getPlayer();
                if (player == null || !player.isOnline()) {
                    continue;
                }
                if (!notified.add(player.getName())) {
                    continue;
                }
                String title = DynamicLang.get(player,
                        "Dynamic.restart-protection.pause-title",
                        "&c&l戰鬥已暫停");
                String subtitle = DynamicLang.get(player,
                        "Dynamic.restart-protection.pause-subtitle",
                        "&7伺服器正在重啟，請稍後重新連線");
                MsgBuilder.sendTitle(title, subtitle, 0, 45, 10, player);
                DynamicLang.send(player, true,
                        "Dynamic.restart-protection.shutdown-warning",
                        "&e伺服器正在重啟，本場對戰已暫停。這次不算中離，不會扣分也不會進入匹配冷卻。");
            }
        }
    }

    public static DuelTimePlugin getInstance() {
        return instance;
    }

    public static boolean isServerShuttingDown() {
        return serverShuttingDown;
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

    public MatchStreakManager getMatchStreakManager() {
        return matchStreakManager;
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

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    public DiscordWebhookManager getDiscordWebhookManager() {
        return discordWebhookManager;
    }

    public QueueWatchdogService getQueueWatchdogService() {
        return queueWatchdogService;
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
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
        if (stripped == null || stripped.trim().isEmpty()) {
            return "unknown";
        }
        return stripped.trim();
    }
}
