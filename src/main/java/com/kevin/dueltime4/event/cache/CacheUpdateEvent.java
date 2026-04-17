package com.kevin.dueltime4.event.cache;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class CacheUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID uuid;
    private final Class<?> clazz;
    private final Object data;

    public CacheUpdateEvent(UUID uuid, Class<?> clazz, Object data) {
        this.uuid = uuid;
        this.clazz = clazz;
        this.data = data;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Object getData() {
        return data;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
