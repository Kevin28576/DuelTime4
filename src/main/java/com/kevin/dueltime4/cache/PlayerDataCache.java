package com.kevin.dueltime4.cache;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.data.MyBatisManager;
import com.kevin.dueltime4.data.mapper.PlayerDataMapper;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.event.cache.CacheInitializedEvent;
import com.kevin.dueltime4.event.ranking.TryToRefreshRankingEvent;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.level.Tier;
import com.kevin.dueltime4.ranking.Ranking;
import com.kevin.dueltime4.util.UtilSync;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class PlayerDataCache {
    private final Map<String, PlayerData> playerDataMap = new HashMap<>();

    public void reloadRefreshRankingTimer() {
        int interval = DuelTimePlugin.getInstance().getCfgManager().getRankingAutoRefreshInterval();
        refreshRankingTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(DuelTimePlugin.getInstance(), () -> {
            for (Ranking ranking : DuelTimePlugin.getInstance().getRankingManager().getRankings().values()) {
                UtilSync.publishEvent(new TryToRefreshRankingEvent(null, ranking));
            }
        }, 3 * 20L, interval * 20L);
    }

    public void reload() {
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            PlayerDataMapper mapper = sqlSession.getMapper(PlayerDataMapper.class);
            mapper.createTableIfNotExists();
            for (Player player : ViaVersion.getOnlinePlayers()) {
                String playerName = player.getName();
                PlayerData playerDataInDatabase = mapper.get(playerName);
                PlayerData playerData =
                        playerDataInDatabase != null ?
                                playerDataInDatabase :
                                new PlayerData(playerName, 0, 0, null, 0, 0, 0, 0, 0, 0);
                playerDataMap.put(playerName, playerData);
                DuelTimePlugin.getInstance().getLevelManager().load(playerName, playerData.getExp());
            }
            Bukkit.getServer().getPluginManager().callEvent(new CacheInitializedEvent(this.getClass()));
        }
    }

    public void reload(Player player) {
        String playerName = player.getName();
        if (playerDataMap.containsKey(playerName)) {
            return;
        }
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession()) {
            PlayerData playerDataInDatabase = sqlSession.getMapper(PlayerDataMapper.class).get(playerName);
            PlayerData playerData =
                    playerDataInDatabase != null ?
                            playerDataInDatabase :
                            new PlayerData(playerName, 0, 0, null, 0, 0, 0, 0, 0, 0);
            playerDataMap.put(playerName, playerData);
            DuelTimePlugin.getInstance().getLevelManager().load(playerName, playerData.getExp());
        }
    }

    public PlayerData get(String playerName) {
        return playerDataMap.get(playerName).clone();
    }

    /**
     * 考慮到效能因素，在載入快取時，不會將所有玩家的資料都載入進來
     * 每個玩家對應的PlayerData物件只會在玩家上線時載入
     * 但如果一定要獲取某個玩家對應的PlayerData物件，那就需要呼叫這個方法
     *
     * @param playerName 玩家名
     * @return PlayerData物件的複製（獲取的是複製體，原因是如此能方便修改與確認，且最後可以作為整體傳入，便於分析資料差異）
     */
    public PlayerData getAnyway(String playerName) {
        PlayerData playerData = playerDataMap.get(playerName);
        if (playerData == null) {
            // 如果快取中找不到這個玩家，再從資料庫裡找
            try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession()) {
                PlayerData playerDataInDatabase = sqlSession.getMapper(PlayerDataMapper.class).get(playerName);
                if (playerDataInDatabase == null) {
                    // 如果資料庫裡也不存在這個玩家，則確認不存在
                    return null;
                } else {
                    // 如果資料庫找到了這個玩家，則獲取資訊
                    playerData = playerDataInDatabase;
                }
            }
        }
        return playerData.clone();
    }

    public void set(String playerName, PlayerData playerData) {
        PlayerData playerDataBefore = playerDataMap.get(playerName);
        LevelManager levelManager = DuelTimePlugin.getInstance().getLevelManager();
        Tier tierBefore = levelManager.getTier(playerName);
        int levelBefore = levelManager.getLevel(playerName);
        playerDataMap.put(playerName, playerData);
        if (playerDataBefore.getExp() != playerData.getExp()) {
            // 如果經驗值發生了變更，則需通知LevelManager重新計算等級和段位名
            DuelTimePlugin.getInstance().getLevelManager().load(playerName, playerData.getExp());
            // 告知與廣播變化
            Player player = Bukkit.getPlayerExact(playerName);
            int level = levelManager.getLevel(playerName);
            if (level > levelBefore && player != null) {
                MsgBuilder.send(Msg.LEVEL_LEVEL_UP_MESSAGE, player,
                        "" + level);
            }
            Tier tier = levelManager.getTier(playerName);
            if (tier.compare(tierBefore) > 0) {
                MsgBuilder.broadcast(Msg.LEVEL_TIER_UP_BROADCAST, false,
                        playerName, tier.getTitle());
                if (player != null) {
                    MsgBuilder.send(Msg.LEVEL_TIER_UP_MESSAGE, player,
                            tier.getTitle());
                }
            } else if (tier.compare(tierBefore) < 0 && player != null) {
                MsgBuilder.send(Msg.LEVEL_TIER_DOWN_MESSAGE, player,
                        tier.getTitle());
            }
        }
        MyBatisManager myBatisManager = DuelTimePlugin.getInstance().getMyBatisManager();
        try (SqlSession sqlSession = myBatisManager.getFactory(this.getClass()).openSession(true)) {
            if (myBatisManager.getType(this.getClass()).equals(MyBatisManager.DatabaseType.MYSQL)) {
                sqlSession.getMapper(PlayerDataMapper.class).insertOrUpdateMySQL(playerData);
            } else {
                sqlSession.getMapper(PlayerDataMapper.class).insertOrUpdateSQLite(playerData);
            }
        }
    }

    private BukkitTask refreshRankingTimer;

    public BukkitTask getRefreshRankingTimer() {
        return refreshRankingTimer;
    }
}
