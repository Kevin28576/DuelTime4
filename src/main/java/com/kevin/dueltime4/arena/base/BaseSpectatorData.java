package com.kevin.dueltime4.arena.base;

import org.bukkit.entity.Player;

public class BaseSpectatorData {
    private final Player player;
    private final String playerName;

    public BaseSpectatorData(Player player) {
        this.player = player;
        this.playerName = player.getName();
    }

    public Player getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }
}