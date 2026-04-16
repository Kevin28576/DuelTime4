package com.kevin.dueltime4.event.arena;

import com.kevin.dueltime4.arena.base.BaseArena;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArenaTryToStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player[] players;
    private final BaseArena arena;
    private boolean cancel;

    public ArenaTryToStartEvent(BaseArena arena, Player... players) {
        this.arena = arena;
        this.players = players;
    }

    public Player[] getPlayers() {
        return players;
    }

    public BaseArena getArena() {
        return arena;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
