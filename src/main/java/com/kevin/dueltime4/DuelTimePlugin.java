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
import org.bukkit.plugin.java.JavaPlugin;

public final class DuelTimePlugin extends JavaPlugin {

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
        instance = this;
        ListenerManager.register();
        cfgManager = new CfgManager();
        levelManager = new LevelManager();
        msgManager = new MsgManager();
        myBatisManager = new MyBatisManager();
        rankingManager = new RankingManager();
        hologramManager = new HologramManager();
        cacheManager = new CacheManager();
        cacheManager.load();
        arenaTypeManager = new ArenaTypeManager();
        arenaManager = new ArenaManager();
        commandHandler = new CommandHandler();
        progressManager = new ProgressManager();
        customInventoryManager = new CustomInventoryManager();
        requestReceiverManager = new RequestReceiverManager();
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
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DuelTimeExpansion(this).register();
        }
        metrics = new Metrics(this, 30767);
        versionChecker = new VersionChecker();
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            for (BaseArena arena : arenaManager.getList()) {
                if (arena.getState().equals(BaseArena.State.IN_PROGRESS_OPENED) || arena.getState().equals(BaseArena.State.IN_PROGRESS_CLOSED)) {
                    arenaManager.stop(arena.getId(), null);
                }
            }
        }
        if (hologramManager != null) {
            hologramManager.disable();
        }
        if (cacheManager != null && cacheManager.getPlayerDataCache() != null && cacheManager.getPlayerDataCache().getRefreshRankingTimer() != null) {
            cacheManager.getPlayerDataCache().getRefreshRankingTimer().cancel();
        }
        if (myBatisManager != null) {
            myBatisManager.closeConnection();
        }
        if (progressManager != null) {
            progressManager.exitAll();
        }
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
}
