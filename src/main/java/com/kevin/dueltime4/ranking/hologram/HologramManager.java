package com.kevin.dueltime4.ranking.hologram;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.LocationCache;
import com.kevin.dueltime4.ranking.Ranking;
import com.kevin.dueltime4.ranking.RankingData;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class HologramManager {
    private boolean hologramPluginExist = false;
    private HologramPluginType hologramPluginTypeUsed;
    private boolean isEnabled = false;
    private final Map<String, HologramInstance> hologramInstanceMap = new HashMap<>();

    public void enable() {
        // 檢查是否有全息圖外掛
        checkHologramPlugin();
        if (!hologramPluginExist) {
            return;
        }
        // 載入所有全息圖
        for (Ranking ranking : DuelTimePlugin.getInstance().getRankingManager().getRankings().values()) {
            create(ranking);
        }
        // 確認初始化完畢
        isEnabled = true;
    }

    public void disable() {
        hologramInstanceMap.values().forEach(HologramInstance::destroy);
        hologramInstanceMap.clear();
    }

    public Map<String, HologramInstance> getHologramInstanceMap() {
        return hologramInstanceMap;
    }

    public void create(Ranking ranking) {
        LocationCache cache = DuelTimePlugin.getInstance().getCacheManager().getLocationCache();
        String id = ranking.getId();
        Location location = cache.get(id);
        if (location == null) {
            return;
        }
        hologramInstanceMap.put(id, new HologramInstance(hologramPluginTypeUsed, location, id, getContent(ranking), ranking.getHologramItemType()));
    }

    public void destroy(String rankingId) {
        hologramInstanceMap.get(rankingId).destroy();
        hologramInstanceMap.remove(rankingId);
    }

    public void refresh(Ranking ranking) {
        hologramInstanceMap.get(ranking.getId()).refresh(getContent(ranking), ranking.getHologramItemType());
    }

    private List<String> getContent(Ranking ranking) {
        List<String> content = new ArrayList<>(MsgBuilder.gets(Msg.RANKING_HOLOGRAM_HEADING, null,
                ranking.getName(), ranking.getDescription()));
        int size = DuelTimePlugin.getInstance().getCfgManager().getConfig().getInt("Ranking.hologram.size");
        List<RankingData> dataList = ranking.getContent();
        for (int i = 0; i < Math.min(size, dataList.size()); i++) {
            RankingData data = ranking.getContent().get(i);
            content.add(MsgBuilder.get(Msg.RANKING_HOLOGRAM_BODY, null, false,
                    "" + (i + 1),
                    data.getPlayerName(),
                    UtilFormat.toString(data.getData()),
                    UtilFormat.toString(data.getExtraStr())));
        }
        content.addAll(MsgBuilder.gets(Msg.RANKING_HOLOGRAM_ENDING, null,
                ranking.getName(), ranking.getDescription()));
        return content;
    }

    public void move(String rankingId, Location location) {
        hologramInstanceMap.get(rankingId).move(location);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    // 檢查全息外掛
    private void checkHologramPlugin() {
        FileConfiguration config = DuelTimePlugin.getInstance().getCfgManager().getConfig();
        boolean hologramEnabled = config.getBoolean("Ranking.hologram.enabled");
        if (!hologramEnabled) return;
        String pluginNameInConfig = config.getString("Ranking.hologram.plugin");
        List<HologramPluginType> hologramPluginTypeInstalledList = new ArrayList<>();
        outer:
        for (HologramPluginType hologramPluginType : HologramPluginType.values()) {
            String[] pluginName = hologramPluginType.getPluginNames();
            for (String realName : pluginName) {
                if (Bukkit.getPluginManager().getPlugin(realName) != null) {
                    hologramPluginTypeInstalledList.add(hologramPluginType);
                    if (realName.equals(pluginNameInConfig)) {
                        hologramPluginTypeUsed = hologramPluginType;
                        hologramPluginExist = true;
                        break outer;
                    }
                }
            }
        }
        // 如果遍歷過後沒有找到配置檔案中所填寫的全息外掛，且裝載有其他受支援的全息外掛，則使用第一個檢測到的
        if (!hologramPluginExist && !hologramPluginTypeInstalledList.isEmpty()) {
            hologramPluginTypeUsed = hologramPluginTypeInstalledList.get(0);
            hologramPluginExist = true;
        }
    }
}
