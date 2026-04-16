package com.kevin.dueltime4.event.arena;

import com.kevin.dueltime4.arena.base.BaseArena;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArenaStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final BaseArena arena;

    public ArenaStartEvent(BaseArena arena) {
        this.arena = arena;
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
