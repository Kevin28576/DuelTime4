package com.kevin.dueltime4.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CustomInventoryHolder implements InventoryHolder {
    private final MultiPageInventory.Type type;

    public CustomInventoryHolder(MultiPageInventory.Type type) {
        this.type = type;
    }

    public MultiPageInventory.Type getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
