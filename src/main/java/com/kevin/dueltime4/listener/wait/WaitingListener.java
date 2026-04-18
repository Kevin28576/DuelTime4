package com.kevin.dueltime4.listener.wait;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.event.arena.ArenaWaitEvent;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaitingListener implements Listener {
    private final Map<UUID, BukkitTask> waitingActionBarTaskMap = new HashMap<>();
    private final Map<UUID, Long> waitingStartTimeMap = new HashMap<>();

    @EventHandler
    public void onArenaWait(ArenaWaitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        stopTracking(playerId);
        waitingStartTimeMap.put(playerId, System.currentTimeMillis());

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player onlinePlayer = Bukkit.getPlayer(playerId);
                if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                    stopTracking(playerId);
                    return;
                }

                ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
                BaseArena waitingArena = arenaManager.getWaitingFor(onlinePlayer);
                if (waitingArena == null) {
                    stopTracking(playerId);
                    return;
                }

                long startedAt = waitingStartTimeMap.getOrDefault(playerId, System.currentTimeMillis());
                long seconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
                int waitingCount = arenaManager.getWaitingPlayers(waitingArena.getId()).size();
                MsgBuilder.sendActionBar(
                        Msg.ARENA_WAIT_ACTION_BAR_SEARCHING,
                        onlinePlayer,
                        true,
                        String.valueOf(seconds),
                        String.valueOf(waitingCount));
            }
        }.runTaskTimer(DuelTimePlugin.getInstance(), 0L, 20L);

        waitingActionBarTaskMap.put(playerId, task);
    }

    /*
    在玩家離開伺服器後，退出等待狀態
     */
    @EventHandler
    public void onPlayerLeaveServer(PlayerQuitEvent event) {
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        Player player = event.getPlayer();
        stopTracking(player.getUniqueId());
        if (arenaManager.getWaitingFor(player) != null) arenaManager.removeWaitingPlayer(player);
    }

    private void stopTracking(UUID playerId) {
        BukkitTask task = waitingActionBarTaskMap.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        waitingStartTimeMap.remove(playerId);
    }
}
