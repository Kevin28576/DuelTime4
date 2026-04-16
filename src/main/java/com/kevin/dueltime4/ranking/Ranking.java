package com.kevin.dueltime4.ranking;

import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ranking {
    private final String id;
    private final Object name;
    private final Object description;
    private final int singlePageSize;
    private final JavaPlugin ownerPlugin;
    private final Material hologramItemType;
    private List<RankingData> content = new ArrayList<>();

    private final Map<String, Integer> playerRankMap = new HashMap<>();

    public Ranking(String id, Object name, Object description, int singlePageSize, Material hologramItemType, JavaPlugin ownerPlugin) {
        this.id = id;
        if (!(name instanceof String) && !(name instanceof Msg)) {
            throw new IllegalArgumentException("The 2nd argument must be String or Msg");
        }
        this.name = name;
        if (!(description instanceof String) && !(description instanceof Msg)) {
            throw new IllegalArgumentException("The 3rd argument must be String or Msg");
        }
        this.description = description;
        this.singlePageSize = singlePageSize;
        this.hologramItemType = hologramItemType;
        this.ownerPlugin = ownerPlugin;
    }

    public Ranking(String id, Object name, Object description, Material hologramItemType, JavaPlugin ownerPlugin) {
        this(id, name, description, 10, hologramItemType, ownerPlugin);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return getName(null);
    }

    public String getName(CommandSender sender) {
        return name instanceof Msg ? MsgBuilder.get((Msg) name, sender) : (String) name;
    }

    public String getDescription() {
        return getDescription(null);
    }

    public String getDescription(CommandSender sender) {
        return description instanceof Msg ? MsgBuilder.get((Msg) description, sender) : (String) description;
    }

    public int getSinglePageSize() {
        return singlePageSize;
    }

    public JavaPlugin getOwnerPlugin() {
        return ownerPlugin;
    }

    public List<RankingData> getContent() {
        return content;
    }

    public void updateContent(List<RankingData> content) {
        this.content = content;
        playerRankMap.clear();
        int rank = 1;
        for (RankingData data : content) {
            playerRankMap.put(data.getPlayerName(), rank);
            rank++;
        }
    }

    public int getRank(String playerName) {
        return playerRankMap.getOrDefault(playerName, -1);
    }

    public Object getData(String playerName) {
        return content.get(getRank(playerName));
    }

    public Map<String, Integer> getPlayerRankMap() {
        return playerRankMap;
    }

    public String getRankString(Player player) {
        if (playerRankMap.containsKey(player.getName())) {
            return "" + playerRankMap.get(player.getName());
        }
        return MsgBuilder.get(Msg.STRING_NO_RANK, player);
    }

    public Material getHologramItemType() {
        return hologramItemType;
    }
}
