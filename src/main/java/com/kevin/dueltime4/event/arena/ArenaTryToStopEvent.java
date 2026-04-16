package com.kevin.dueltime4.event.arena;

import com.kevin.dueltime4.arena.base.BaseArena;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArenaTryToStopEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final BaseArena arena;
    private final String reason;

    public ArenaTryToStopEvent(BaseArena arena,String reason) {
        this.arena = arena;
        this.reason = reason;
    }

    public BaseArena getArena() {
        return arena;
    }

    public String getReason() {
        return reason;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
