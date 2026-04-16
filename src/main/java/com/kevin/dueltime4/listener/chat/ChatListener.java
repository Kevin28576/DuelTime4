package com.kevin.dueltime4.listener.chat;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    @EventHandler
    public void showTierTitleWhileChatting(AsyncPlayerChatEvent e) {
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        if (!cfgManager.isTierTitleShowedInChatBoxEnabled()) {
            return;
        }
        Player player = e.getPlayer();
        LevelManager levelManager = DuelTimePlugin.getInstance().getLevelManager();
        String title = levelManager.getTier(player.getName()).getTitle();
        e.setFormat(cfgManager.getTierTitleShowedInChatBoxFormat().replace("%v", title) + e.getFormat());
    }
}