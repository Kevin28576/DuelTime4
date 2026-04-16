package com.kevin.dueltime4.progress;

import com.kevin.dueltime4.event.progress.ProgressStartEvent;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ProgressManager {
    private final Map<String, Progress> progressMap = new HashMap<>();

    public Progress getProgress(String playerName) {
        return progressMap.get(playerName);
    }

    public void enter(Player player, Progress progress) {
        Progress nowProgress = progressMap.get(player.getName());
        if (nowProgress != null) {
            if (nowProgress.getId().equals(progress.getId())) {
                MsgBuilder.send(Msg.PROGRESS_REPEATEDLY_JOIN, player,
                        progress.getName());
            } else {
                MsgBuilder.send(Msg.PROGRESS_JOIN_WHILE_HANDLING_OTHER, player,
                        nowProgress.getName());
            }
            return;
        }
        progressMap.put(player.getName(), progress);
        MsgBuilder.send(Msg.PROGRESS_JOIN_SUCCESSFULLY, player,
                progress.getName());
        // 發布事件
        Bukkit.getPluginManager().callEvent(new ProgressStartEvent(player, progress));
    }

    public void exit(String playerName) {
        progressMap.get(playerName).exit();
    }

    // 僅供Progress內部呼叫
    public void cancel(String playerName) {
        progressMap.remove(playerName);
    }

    public void exitAll() {
        for (Progress progress : progressMap.values()) {
            progress.exit();
        }
    }
}

