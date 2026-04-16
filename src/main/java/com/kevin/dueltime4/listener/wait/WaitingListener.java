package com.kevin.dueltime4.listener.wait;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class WaitingListener implements Listener {

    /*
    在玩家離開伺服器後，退出等待狀態
     */
    @EventHandler
    public void onPlayerLeaveServer(PlayerQuitEvent event) {
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        Player player = event.getPlayer();
        if (arenaManager.getWaitingFor(player) != null) arenaManager.removeWaitingPlayer(player);
    }
}
