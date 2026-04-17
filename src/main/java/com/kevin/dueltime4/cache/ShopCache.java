package com.kevin.dueltime4.cache;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.data.mapper.ShopMapper;
import com.kevin.dueltime4.data.pojo.ShopRewardData;
import com.kevin.dueltime4.event.cache.CacheInitializedEvent;
import com.kevin.dueltime4.util.UtilSync;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopCache {
    private List<ShopRewardData> rewardDataList = new ArrayList<>();

    public void reload() {
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            ShopMapper mapper = sqlSession.getMapper(ShopMapper.class);
            mapper.createTable(DuelTimePlugin.getInstance().getMyBatisManager().getType(this.getClass()).name());
            rewardDataList = mapper.getList();
            UtilSync.publishEvent(new CacheInitializedEvent(this.getClass()));
        }
    }

    /**
     * 根據位置獲取索引值
     *
     * @param page   頁數
     * @param row    行數
     * @param column 列數
     * @return 索引值
     */
    public static int getIndexByLoc(int page, int row, int column) {
        return (page - 1) * 20 + (row - 1) * 5 + (column - 1);
    }

    /**
     * 根據索引值反向計算位置
     *
     * @return 一個三元陣列，內容依次為：頁數-行數-列數
     */
    public static int[] getLocByIndex(int index) {
        int page = index / 20 + 1;
        int row = (index % 20) / 5 + 1;
        int column = (index % 20) % 5 + 1;
        return new int[]{page, row, column};
    }

    private void updateContentTotalNumber() {
        DuelTimePlugin.getInstance().getCustomInventoryManager().getShop().updateContentTotalNumber(rewardDataList.size());
    }

    public List<ShopRewardData> getList() {
        return rewardDataList;
    }

    public ShopRewardData get(int page, int row, int column) {
        try {
            return rewardDataList.get(getIndexByLoc(page, row, column));
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }
    }

    public void add(ItemStack itemStack, double point, int levelLimit, String description, List<String> commands) {
        ShopRewardData rewardData = new ShopRewardData(-1, itemStack, point, levelLimit, description, commands, 0);
        rewardDataList.add(rewardData);
        DuelTimePlugin.getInstance().getCustomInventoryManager().getShop().updateContentTotalNumber(rewardDataList.size());
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(ShopMapper.class).add(rewardData);
        }
    }


    public boolean delete(int page, int row, int column) {
        int index = getIndexByLoc(page, row, column);
        if (index + 1 > rewardDataList.size()) {
            return false;
        }
        ShopRewardData rewardData = rewardDataList.get(index);
        rewardDataList.remove(getIndexByLoc(page, row, column));
        DuelTimePlugin.getInstance().getCustomInventoryManager().getShop().updateContentTotalNumber(rewardDataList.size());
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(ShopMapper.class).remove(rewardData.getId());
        }
        return true;
    }

    public void set(int page, int row, int column, ShopRewardData rewardData) {
        int index = getIndexByLoc(page, row, column);
        rewardDataList.set(index, rewardData);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(ShopMapper.class).set(rewardData);
        }
    }
}
