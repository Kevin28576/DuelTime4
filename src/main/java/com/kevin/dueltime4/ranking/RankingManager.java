package com.kevin.dueltime4.ranking;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.message.Msg;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingManager {
    private final Map<String, Ranking> rankings = new HashMap<>();

    public RankingManager() {
        JavaPlugin plugin = DuelTimePlugin.getInstance();
        rankings.put(InternalType.CLASSIC_WIN_NUMBER.getId(), new Ranking(InternalType.CLASSIC_WIN_NUMBER.getId(), Msg.RANKING_TYPE_CLASSIC_WINS_NAME, Msg.RANKING_TYPE_CLASSIC_WINS_DESCRIPTION, ViaVersionItem.getGoldenSwordMaterial(), plugin));
        rankings.put(InternalType.CLASSIC_WIN_RATE.getId(), new Ranking(InternalType.CLASSIC_WIN_RATE.getId(), Msg.RANKING_TYPE_CLASSIC_WIN_RATE_NAME, Msg.RANKING_TYPE_CLASSIC_WIN_RATE_DESCRIPTION, ViaVersionItem.getGoldenAxeMaterial(), plugin));
        rankings.put(InternalType.EXP.getId(), new Ranking(InternalType.EXP.getId(), Msg.RANKING_TYPE_EXP_NAME, Msg.RANKING_TYPE_EXP_DESCRIPTION, ViaVersionItem.getExpBottleMaterial(), plugin));
        rankings.put(InternalType.TOTAL_GAME_NUMBER.getId(), new Ranking(InternalType.TOTAL_GAME_NUMBER.getId(), Msg.RANKING_TOTAL_GAME_NUMBER_TYPE_NAME, Msg.RANKING_TOTAL_GAME_NUMBER_TYPE_DESCRIPTION, Material.NETHER_STAR, plugin));
        rankings.put(InternalType.TOTAL_GAME_TIME.getId(), new Ranking(InternalType.TOTAL_GAME_TIME.getId(), Msg.RANKING_TOTAL_GAME_TIME_TYPE_NAME, Msg.RANKING_TOTAL_GAME_TIME_TYPE_DESCRIPTION, ViaVersionItem.getWatchMaterial(), plugin));
        rankings.put(InternalType.CLASSIC_GAME_NUMBER.getId(), new Ranking(InternalType.CLASSIC_GAME_NUMBER.getId(), Msg.RANKING_CLASSIC_GAME_NUMBER_TYPE_NAME, Msg.RANKING_CLASSIC_GAME_NUMBER_TYPE_DESCRIPTION, ViaVersionItem.getMapMaterial(), plugin));
        rankings.put(InternalType.CLASSIC_GAME_TIME.getId(), new Ranking(InternalType.CLASSIC_GAME_TIME.getId(), Msg.RANKING_CLASSIC_GAME_TIME_TYPE_NAME, Msg.RANKING_CLASSIC_GAME_TIME_TYPE_DESCRIPTION, ViaVersionItem.getWatchMaterial(), plugin));
    }

    public void registerRanking(String id, Object name, Object description, Material hologramItemType, JavaPlugin ownerPlugin) {
        id = ownerPlugin.getName() + ":" + id;
        for (Ranking ranking : rankings.values()) {
            if (ranking.getOwnerPlugin().equals(DuelTimePlugin.getInstance())) {
                throw new IllegalArgumentException("Suspected instance spoofing");
            }
            if (ranking.getId().equals(id)) {
                throw new IllegalArgumentException("ID " + id + " conflicts with other existing IDs");
            }
        }
        rankings.put(id, new Ranking(id, name, description, hologramItemType, ownerPlugin));
    }

    public void registerRanking(String id, Object name, Object description, JavaPlugin plugin) {
        registerRanking(id, name, description, null, plugin);
    }

    public void updateRanking(String id, List<RankingData> content, JavaPlugin plugin) {
        if (!rankings.get(id).getOwnerPlugin().equals(plugin)) {
            throw new IllegalArgumentException("Suspected instance spoofing");
        }
        if (content == null) content = new ArrayList<>();
        rankings.get(id).updateContent(content);
    }

    public Ranking getRanking(String id) {
        if (!id.contains(":")) id = "dueltime4:" + id;
        return rankings.get(id);
    }

    public Map<String, Ranking> getRankings() {
        return rankings;
    }

    public enum InternalType {
        CLASSIC_WIN_NUMBER("dueltime4:classic_win_number"),
        CLASSIC_WIN_RATE("dueltime4:classic_win_rate"),
        EXP("dueltime4:exp"),
        TOTAL_GAME_NUMBER("dueltime4:total_game_number"),
        TOTAL_GAME_TIME("dueltime4:total_game_time"),
        CLASSIC_GAME_NUMBER("dueltime4:classic_game_number"),
        CLASSIC_GAME_TIME("dueltime4:classic_game_time");

        private final String id;

        InternalType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
