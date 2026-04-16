package com.kevin.dueltime4.gui.simple;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemDetailInventoryHolder implements InventoryHolder {
    public static HashMap<String, ItemStack> itemMap = new HashMap<>();
    @Override
    public Inventory getInventory() {
        return null;
    }
}
