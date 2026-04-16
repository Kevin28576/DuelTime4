package com.kevin.dueltime4.listener.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.gui.CustomInventoryHolder;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class CloseInventoryListener implements Listener {
    @EventHandler
    public void removeViewerForClosingInventory(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CustomInventoryHolder) {
            CustomInventoryManager manager = DuelTimePlugin.getInstance().getCustomInventoryManager();
            String playerName = event.getPlayer().getName();
            manager.getShop().removeViewer(playerName);
            manager.getStart().removeViewer(playerName);
        }
    }
}
