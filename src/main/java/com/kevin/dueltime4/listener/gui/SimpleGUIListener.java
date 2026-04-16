package com.kevin.dueltime4.listener.gui;

import com.kevin.dueltime4.gui.simple.ItemDetailInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SimpleGUIListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ItemDetailInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
