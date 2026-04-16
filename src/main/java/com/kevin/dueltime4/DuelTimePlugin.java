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
        // 確認伺服器版本，方便一些版本差異的討論
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
        if (packageName.split("\\.").length >= 4) {
            // packageName 形如 org.bukkit.craftbukkit.v1_20_R1
            serverVersionInt = Integer.parseInt(serverVersion.contains("_") ? serverVersion.split("_")[1] : serverVersion.split("-")[0]);
            if (serverVersionInt == 8 || serverVersionInt == 9) {
                ViaVersion.getClassesForTitleAndAction();
            }
        } else {
            // packageName 形如 org.bukkit.craftbukkit
            try {
                serverVersionInt = Integer.parseInt(Bukkit.getVersion().split("\\.")[1]);
            } catch (NumberFormatException e) {
                // Bukkit.getVersion() 形如 1.21-66-99ae7bb
                serverVersionInt = Integer.parseInt(Bukkit.getVersion().split("-")[0].split("\\.")[1]);
            }
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
}
