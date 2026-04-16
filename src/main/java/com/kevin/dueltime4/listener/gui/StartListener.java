package com.kevin.dueltime4.listener.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.sub.CommandPermission;
import com.kevin.dueltime4.event.arena.ArenaTryToJoinEvent;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import com.kevin.dueltime4.gui.MultiPageInventory;
import com.kevin.dueltime4.gui.StartInventory;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

import static com.kevin.dueltime4.arena.base.BaseArena.State.*;

public class StartListener implements Listener {
    @EventHandler
    public void onChooseArena(InventoryClickEvent event) {
        CustomInventoryManager customInventoryManager = DuelTimePlugin.getInstance().getCustomInventoryManager();
        StartInventory startInventory = customInventoryManager.getStart();
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        int totalIndex = startInventory.checkBeforeClickFunctionItem(event, arenaManager.size());
        if (totalIndex < MultiPageInventory.INDEX_THRESHOLD) {
            // 不是點選的本外掛面板裡的功能內容
            return;
        }
        BaseArena arenaClicked = arenaManager.getList().get(totalIndex);
        Player player = (Player) event.getWhoClicked();
        if (arenaManager.getOf(player) != null) {
            MsgBuilder.send(Msg.GUI_TYPE_START_USE_WHILE_IN_GAME, player, arenaClicked.getName());
            return;
        }
        BaseArena.State state = arenaClicked.getState();
        if (state == WAITING) {
            BaseArena arenaWaited = arenaManager.getWaitingFor(player);
            if (arenaWaited != null && arenaWaited.getId().equals(arenaClicked.getId())) {
                // 如果點選的是當前所等待的競技場，則取消等待
                arenaManager.removeWaitingPlayer(player);
                MsgBuilder.send(Msg.ARENA_WAIT_STOP, player, arenaClicked.getName());
            } else {
                // 如果要開始等待，或者想切換等待。為了防止頻繁操作，這裡新增了一個短暫的時間間隔約束
                if (!player.hasPermission(CommandPermission.ADMIN)) {
                    if (waitingOperationCooldown.getOrDefault(player.getName(), 0L) > System.currentTimeMillis()) {
                        MsgBuilder.send(Msg.GUI_TYPE_START_WAITING_OPERATION_COOLDOWN, player);
                        return;
                    }
                    waitingOperationCooldown.put(player.getName(), System.currentTimeMillis() + 1000);
                }
                arenaManager.addWaitingPlayer(player, arenaClicked.getId());
                player.playSound(player.getLocation(), ViaVersion.getSound(
                        "BLOCK_ANVIL_PLACE", "ANVIL_PLACE"), 1, 0);
            }
        } else if (state == IN_PROGRESS_CLOSED) {
            MsgBuilder.send(Msg.GUI_TYPE_START_STATE_IN_PROGRESS_CLOSED, player, arenaClicked.getName());
        } else if (state == IN_PROGRESS_OPENED) {
            if (arenaClicked.isFull()) {
                MsgBuilder.send(Msg.GUI_TYPE_START_STATE_IN_PROGRESS_OPENED_FULL, player, arenaClicked.getName());
            } else {
                MsgBuilder.send(Msg.GUI_TYPE_START_STATE_IN_PROGRESS_OPENED, player, arenaClicked.getName());
                arenaManager.join(player, arenaClicked.getId(), ArenaTryToJoinEvent.Way.GUI);
            }
        } else {
            MsgBuilder.send(Msg.GUI_TYPE_START_STATE_DISABLED, player, arenaClicked.getName());
        }
    }

    Map<String, Long> waitingOperationCooldown = new HashMap<>();
}
