package com.kevin.dueltime4.cache;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.data.mapper.ClassicArenaRecordDataMapper;
import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;
import com.kevin.dueltime4.event.cache.CacheInitializedEvent;
import com.kevin.dueltime4.util.UtilSync;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordCache {
    private final Map<String, List<BaseRecordData>> playerRecordMap = new HashMap<>();

    public void reload() {
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            ClassicArenaRecordDataMapper mapper = sqlSession.getMapper(ClassicArenaRecordDataMapper.class);
            mapper.createTableIfNotExists();
            UtilSync.publishEvent(new CacheInitializedEvent(this.getClass()));
        }
    }

    /**
     * 載入某個玩家的記錄資料到快取中
     * 一般只有兩種情況會被呼叫
     * 1.開啟記錄面板時，由ArenaRecordInventory的openFor方法的最開始呼叫
     * 2.比賽結束後留下比賽記錄時，由本類的add方法呼叫
     * 意味著玩家想看記錄面板時才載入記錄快取，減少不必要的快取佔用
     */
    public void reload(Player player) {
        String playerName = player.getName();
        if (playerRecordMap.containsKey(playerName)) {
            return;
        }
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession()) {
            ClassicArenaRecordDataMapper mapper = sqlSession.getMapper(ClassicArenaRecordDataMapper.class);
            List<BaseRecordData> records = mapper.getAll(playerName);
            playerRecordMap.put(playerName, records);
            // 向記錄面板GUI管理器回傳資料
            DuelTimePlugin.getInstance().getCustomInventoryManager().getArenaRecord().updateContentTotalNumber(records.size(), player);
        }
    }

    public List<BaseRecordData> get(String playerName) {
        return playerRecordMap.getOrDefault(playerName, new ArrayList<>());
    }

    /**
     * 考慮到效能因素，在載入快取時，不會將所有玩家的比賽記錄都載入進來
     * 每個玩家的BaseRecordData物件集合只會在玩家上線時載入
     * 但如果一定要獲取某個玩家的BaseRecordData物件集合，那就需要呼叫這個方法
     *
     * @param playerName 玩家名
     * @return BaseRecordData物件集合，即所有比賽記錄
     */
    public List<BaseRecordData> getAnyway(String playerName) {
        List<BaseRecordData> records = playerRecordMap.get(playerName);
        if (records == null) {
            // 如果快取中找不到這個玩家，再從資料庫裡找
            try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession()) {
                List<BaseRecordData> recordsInDatabase = sqlSession.getMapper(ClassicArenaRecordDataMapper.class).getAll(playerName);
                if (recordsInDatabase == null) {
                    // 如果資料庫裡也不存在這個玩家的記錄資料，則確認不存在
                    return null;
                } else {
                    // 如果資料庫找到了這個玩家的記錄資料，則獲取
                    records = recordsInDatabase;
                }
            }
        }
        return records;
    }

    public void add(Player player, ClassicArenaRecordData recordData) {
        // 先判斷該玩家的比賽記錄是否已經載入快取
        reload(player);
        String playerName = player.getName();
        List<BaseRecordData> records = playerRecordMap.getOrDefault(playerName, new ArrayList<>());
        records.add(recordData);
        playerRecordMap.put(playerName, records);
        DuelTimePlugin.getInstance().getCustomInventoryManager().getArenaRecord().updateContentTotalNumber(records.size(), player);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(ClassicArenaRecordDataMapper.class).add(recordData);
        }
    }
}
