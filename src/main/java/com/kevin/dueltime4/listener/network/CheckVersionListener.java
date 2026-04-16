package com.kevin.dueltime4.listener.network;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class CheckVersionListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().isOp())
            return;
        DuelTimePlugin.getInstance().getVersionChecker().checkForUpdates(event.getPlayer());
    }
}
