package com.kevin.dueltime4.listener.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.QueueMatchConfirmManager;
import com.kevin.dueltime4.gui.simple.QueueMatchConfirmInventoryHolder;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class QueueMatchConfirmGUIListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof QueueMatchConfirmInventoryHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || clickedInventory != topInventory) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot == QueueMatchConfirmManager.ACCEPT_SLOT) {
            handleAccept(player);
            return;
        }
        if (rawSlot == QueueMatchConfirmManager.DECLINE_SLOT) {
            handleDecline(player);
        }
    }

    private void handleAccept(Player player) {
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        try {
            if (!arenaManager.acceptPendingMatch(player)) {
                DynamicLang.send(player, true,
                        "Dynamic.queue.confirm.no-pending",
                        "&c這場匹配確認已失效，請重新排隊。");
            }
            player.closeInventory();
        } catch (Throwable throwable) {
            onGuiError(player, throwable);
        }
    }

    private void handleDecline(Player player) {
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        try {
            if (!arenaManager.declinePendingMatch(player)) {
                DynamicLang.send(player, true,
                        "Dynamic.queue.confirm.no-pending",
                        "&c這場匹配確認已失效，請重新排隊。");
            }
            player.closeInventory();
        } catch (Throwable throwable) {
            onGuiError(player, throwable);
        }
    }

    private void onGuiError(Player player, Throwable throwable) {
        DynamicLang.send(player, true,
                "Dynamic.queue.confirm.gui-error",
                "&c確認介面發生錯誤，已切換為指令模式。");
        DynamicLang.send(player, true,
                "Dynamic.queue.confirm.command-fallback",
                "&7請輸入 &a{accept} &7接受，或輸入 &c{decline} &7拒絕。",
                "accept", QueueMatchConfirmManager.ACCEPT_COMMAND,
                "decline", QueueMatchConfirmManager.DECLINE_COMMAND);
        DuelTimePlugin.getInstance().getLogger().warning(
                "Queue confirm GUI click failed for " + player.getName()
                        + ", fallback to command mode. reason="
                        + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        player.closeInventory();
    }
}

