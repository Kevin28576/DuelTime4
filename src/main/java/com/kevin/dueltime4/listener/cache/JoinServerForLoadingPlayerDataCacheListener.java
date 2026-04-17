package com.kevin.dueltime4.listener.cache;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinServerForLoadingPlayerDataCacheListener implements Listener {
    @EventHandler
    public void onJoinServer(PlayerJoinEvent event) {
        DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache().reload(event.getPlayer());
    }
}
