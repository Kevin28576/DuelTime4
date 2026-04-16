package com.kevin.dueltime4.listener.progress;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.progress.Progress;
import com.kevin.dueltime4.progress.ProgressType;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

public class ProgressUploadListener implements Listener {

    /**
     * 上傳字串、字串列表、數字
     */
    @EventHandler
    public void onSendMessage(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Progress progress = DuelTimePlugin.getInstance().getProgressManager().getProgress(player.getName());
        if (progress == null ||
                !progress.getId().equals(ProgressType.InternalType.ADD_FUNCTION_CLASSIC_INVENTORY_CHECK_KEYWORD.getId()) ||
                progress.getFinishedStep() != 0) {
            return;
        }
        if (progress.isPaused()) {
            return;
        }
        String enter = event.getMessage();
        if (!enter.equalsIgnoreCase("name") &&
                !enter.equalsIgnoreCase("lore") &&
                !enter.equalsIgnoreCase("all")) {
            MsgBuilder.send(Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_1_INCORRECT_RANGE,player,
                    enter);
        }
        progress.next(enter.toLowerCase());
    }
}