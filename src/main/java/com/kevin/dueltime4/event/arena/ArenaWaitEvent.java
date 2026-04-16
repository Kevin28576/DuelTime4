package com.kevin.dueltime4.event.arena;

import com.kevin.dueltime4.arena.base.BaseArena;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArenaWaitEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final BaseArena arena;

    public ArenaWaitEvent(Player player, BaseArena arena) {
        this.player = player;
        this.arena = arena;
    }

    public Player getPlayer() {
        return player;
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
}
