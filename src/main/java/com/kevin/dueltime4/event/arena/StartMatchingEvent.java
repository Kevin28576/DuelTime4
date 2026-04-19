package com.kevin.dueltime4.event.arena;

import com.kevin.dueltime4.arena.base.BaseArena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class StartMatchingEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final BaseArena arena;
    private final boolean switchedArena;

    public StartMatchingEvent(Player player, BaseArena arena, boolean switchedArena) {
        this.player = player;
        this.arena = arena;
        this.switchedArena = switchedArena;
    }

    public Player getPlayer() {
        return player;
    }

    public BaseArena getArena() {
        return arena;
    }

    public boolean isSwitchedArena() {
        return switchedArena;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
