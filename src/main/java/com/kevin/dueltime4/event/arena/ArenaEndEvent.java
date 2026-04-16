package com.kevin.dueltime4.event.arena;

import com.kevin.dueltime4.arena.base.BaseArena;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArenaEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final BaseArena arena;

    public ArenaEndEvent(BaseArena arena) {
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
