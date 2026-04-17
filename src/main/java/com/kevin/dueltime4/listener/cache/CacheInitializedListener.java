package com.kevin.dueltime4.listener.cache;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.LocationCache;
import com.kevin.dueltime4.event.cache.CacheInitializedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CacheInitializedListener implements Listener {
    @EventHandler
    public void onLocationCacheInit(CacheInitializedEvent e) {
        if (e.getClazz().equals(LocationCache.class)) {
            DuelTimePlugin.getInstance().getHologramManager().enable();
        }
    }
}
