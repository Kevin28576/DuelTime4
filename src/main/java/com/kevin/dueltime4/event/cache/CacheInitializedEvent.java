package com.kevin.dueltime4.event.cache;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CacheInitializedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Class<?> clazz;
    public CacheInitializedEvent( Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
