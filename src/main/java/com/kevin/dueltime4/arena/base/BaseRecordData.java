package com.kevin.dueltime4.arena.base;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class BaseRecordData {
    private final String playerName;
    private final String arenaId;
    private final String date;

    public BaseRecordData(String playerName, String arenaId,String date) {
        this.playerName = playerName;
        this.arenaId = arenaId;
        this.date = date;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerName);
    }

    public String getArenaId() {
        return arenaId;
    }

    public String getDate() {
        return date;
    }

    // 獲取在記錄面板中用於展示比賽記錄詳情的物品的名稱
    public abstract String getItemStackTitle();

    // 獲取在記錄面板中用於展示比賽記錄詳情的物品的lore
    public abstract List<String> getItemStackContent();
}
