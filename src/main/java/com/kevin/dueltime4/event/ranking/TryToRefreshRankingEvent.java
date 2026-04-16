package com.kevin.dueltime4.event.ranking;

import com.kevin.dueltime4.ranking.Ranking;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TryToRefreshRankingEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final CommandSender sender;
    private final Ranking ranking;

    public TryToRefreshRankingEvent(CommandSender player, Ranking ranking) {
        this.sender = player;
        this.ranking = ranking;
    }

    public CommandSender getSender() {
        return sender;
    }

    public Ranking getRanking() {
        return ranking;
    }


    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
