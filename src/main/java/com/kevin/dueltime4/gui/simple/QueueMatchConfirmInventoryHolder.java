package com.kevin.dueltime4.gui.simple;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class QueueMatchConfirmInventoryHolder implements InventoryHolder {
    private final String arenaId;

    public QueueMatchConfirmInventoryHolder(String arenaId) {
        this.arenaId = arenaId;
    }

    public String getArenaId() {
        return arenaId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
