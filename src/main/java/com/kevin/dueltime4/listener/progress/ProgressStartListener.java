package com.kevin.dueltime4.listener.progress;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.event.progress.ProgressStartEvent;
import com.kevin.dueltime4.progress.Progress;
import com.kevin.dueltime4.util.UtilGeometry;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static com.kevin.dueltime4.progress.ProgressType.InternalType.ADD_FUNCTION_CLASSIC_SPECTATE;
import static com.kevin.dueltime4.progress.ProgressType.InternalType.CREATE_CLASSIC_ARENA;

public class ProgressStartListener implements Listener {
    @EventHandler
    public void onProgressStart(ProgressStartEvent event) {
        Progress progress = event.getProgress();
        Player player = progress.getPlayer();
        if (progress.isBossBarUsed()) {
            progress.initBossBar(BarColor.GREEN, BarStyle.SOLID);
        } else {
            MsgBuilder.sends(Msg.PROGRESS_BOSSBAR_FREE_TIP, player, false,
                    progress.getName(), "" + 0, "" + progress.getSteps().length,
                    "0 1",
                    progress.getSteps()[0].getTip());
        }
        if (progress.getId().equals(CREATE_CLASSIC_ARENA.getId()) || progress.getId().equals(ADD_FUNCTION_CLASSIC_SPECTATE.getId())) {
            progress.setTimer(Bukkit.getScheduler().runTaskTimer(DuelTimePlugin.getInstance(), () -> {
                // 顯示競技場邊界粒子預覽
                if (progress.getFinishedStep() >= 2) {
                    Location locationDiagonalA = (Location) progress.getSteps()[0].getData();
                    Location locationDiagonalB = (Location) progress.getSteps()[1].getData();
                    int[] colors = progress.isPaused() ? new int[]{255, 255, 0} : new int[]{72, 209, 204};
                    UtilGeometry.buildCubicLine(player, locationDiagonalA, locationDiagonalB, 0.3, colors[0], colors[1], colors[2]);
                }
            }, 20, 20));
        }
    }
}