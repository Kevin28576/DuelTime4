package com.kevin.dueltime4.level;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * 管理配置檔案中對升級公式的定義、對等級展示名的定義、玩家等級與對應展示名的快取
 */
public class LevelManager {
    private final Map<String, Integer> levelCacheMap = new HashMap<>();
    private final List<Tier> tiers = new ArrayList<>();
    private String defaultTitle;
    private final String DEFAULT_ID = "default";
    private final Map<String, Tier> tierCacheMap = new HashMap<>();

    public LevelManager() {
        reloadSettings();
    }

    public int getLevel(String playerName, double exp) {
        if (!tierCacheMap.containsKey(playerName)) {
            load(playerName, exp);
        }
        return levelCacheMap.get(playerName);
    }

    public int getLevel(String playerName) {
        return levelCacheMap.get(playerName);
    }

    /**
     * 根據某個玩家的經驗計算出對應的等級、段位並載入/更新到本類的快取，一般用於玩家進服、PlayerDataCache更新且觸發經驗變動時呼叫
     *
     * @param playerName 玩家名
     * @param exp        玩家的經驗
     */
    public void load(String playerName, double exp) {
        int originalLevel = levelCacheMap.getOrDefault(playerName, -1);
        int level = calculateLevel(exp);
        levelCacheMap.put(playerName, level);
        if (originalLevel == -1 || originalLevel != level) {
            // 如果玩家原先不在快取中、或者在快取中且檢測到了等級變化，則計算段位名
            Tier tier = calculateTier(level);
            tierCacheMap.put(playerName, tier);
        }
    }

    public int calculateLevel(double exp) {
        int currentLevel = 0;
        double totalExpNeeded = 0;
        for (int i = 0; i < tiers.size(); i++) {
            Tier currentTier = tiers.get(i);
            double expForThisTier = currentTier.getExpForLevelUp() * (i == tiers.size() - 1 ?
                    Double.MAX_VALUE :
                    tiers.get(i + 1).getLevel() - currentTier.getLevel());
            if (exp < totalExpNeeded + expForThisTier) {
                currentLevel += (int) ((exp - totalExpNeeded) / currentTier.getExpForLevelUp());
                break;
            }
            totalExpNeeded += expForThisTier;
            currentLevel = tiers.get(i + 1).getLevel();
        }
        return currentLevel;
    }

    public Tier calculateTier(int level) {
        Tier tier = tiers.get(0);
        for (Tier nowTier : tiers) {
            if (level < nowTier.getLevel()) {
                break;
            }
            tier = nowTier;
        }
        return tier;
    }

    public double calculateRemainingExpForLevelUp(double exp) {
        double totalExpNeeded = 0;
        for (int i = 0; i < tiers.size(); i++) {
            Tier currentTier = tiers.get(i);
            double expForThisTier = currentTier.getExpForLevelUp() * (i == tiers.size() - 1 ?
                    Double.MAX_VALUE :
                    tiers.get(i + 1).getLevel() - currentTier.getLevel());
            if (exp < totalExpNeeded + expForThisTier) {
                double expForLevelUp = currentTier.getExpForLevelUp();
                exp -= totalExpNeeded;
                exp %= expForLevelUp;
                return expForLevelUp - exp;
            }
            totalExpNeeded += expForThisTier;
        }
        return 0;
    }

    public double calculateLevelUpProgress(double exp) {
        double totalExpNeeded = 0;
        for (int i = 0; i < tiers.size(); i++) {
            Tier currentTier = tiers.get(i);
            double expForThisTier = currentTier.getExpForLevelUp() * (i == tiers.size() - 1 ?
                    Double.MAX_VALUE :
                    tiers.get(i + 1).getLevel() - currentTier.getLevel());
            if (exp < totalExpNeeded + expForThisTier) {
                double expForLevelUp = currentTier.getExpForLevelUp();
                exp -= totalExpNeeded;
                exp %= expForLevelUp;
                return exp / expForLevelUp;
            }
            totalExpNeeded += expForThisTier;
        }
        return 1;
    }

    /**
     * 從快取中提取段位
     *
     * @param exp        可能傳入的經驗值，用於將不在快取中的玩家載入快取
     * @param playerName 玩家名
     * @return 玩家對應的段位
     */
    public Tier getTier(String playerName, double exp) {
        if (!tierCacheMap.containsKey(playerName)) {
            load(playerName, exp);
        }
        return tierCacheMap.get(playerName);
    }


    /**
     * 從快取中直接提取段位，但呼叫時要保證玩家在本類的快取中
     */
    public Tier getTier(String playerName) {
        return tierCacheMap.get(playerName);
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    public Tier getDefaultTier() {
        return tiers.get(0);
    }

    /**
     * 從config.yml中載入對單挑等級展示名的設定
     */
    public void reloadSettings() {
        FileConfiguration config = DuelTimePlugin.getInstance().getCfgManager().getConfig();// 直接獲取instance裡的getConfig()在低版本可能會亂碼
        defaultTitle = config.getString("Level.tier.default.title").replace("&", "§");
        int defaultExpForLevelUp = config.getInt("Level.tier.default.exp-for-level-up");
        tiers.add(new Tier(DEFAULT_ID, 0, defaultTitle, defaultExpForLevelUp));
        String path = "Level.tier.custom";
        List<Integer> levelDefinedList = new ArrayList<>();
        for (String id : config.getConfigurationSection(path).getKeys(false)) {
            if (id.equals(DEFAULT_ID)) {
                DuelTimePlugin.getInstance().getLogger().warning("Invalid level id");
                // 對應的等級已有展示名定義，丟擲異常
                break;
            }
            int level = config.getInt(path + "." + id + ".level");
            if (level < 1) {
                DuelTimePlugin.getInstance().getLogger().warning("Invalid level value: " + level);
                // 等級為非正數，丟擲異常
                break;
            }
            if (levelDefinedList.contains(level)) {
                DuelTimePlugin.getInstance().getLogger().warning("This level is already defined: " + level);
                // 對應的等級已有展示名定義，丟擲異常
                break;
            }
            double expForLevelUp = config.getDouble(path + "." + id + ".exp-for-level-up");
            if (expForLevelUp <= 0) {
                DuelTimePlugin.getInstance().getLogger().warning("Experience required for level up must be greater than 0");
                // 升級所需為非正數，丟擲異常
                break;
            }
            String title = config.getString(path + "." + id + ".title").replace("&", "§");
            Tier tier = new Tier(id, level, title, expForLevelUp);
            int index = 0;
            while (index < tiers.size() && level > tiers.get(index).getLevel()) {
                /*
                index即為當前所比較物件對應的索引值
                由於customNameList按level屬性保持升序，現在則按索引值順序執行比較，遇到大於自己的就停下並在退出迴圈後插入
                */
                index++;
            }
            levelDefinedList.add(level);
            tiers.add(index, tier);
        }
    }
}
