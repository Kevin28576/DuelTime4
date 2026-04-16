package com.kevin.dueltime4.util;

import com.kevin.dueltime4.arena.type.ArenaType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class UtilInventory {
    public static ItemStack check(Player player, String keywordCheckRange, List<String> keywords, List<String> types) {
        Inventory inventory = player.getInventory();
        for (int slot = -1; slot < 40; slot++) {
            // -1 代表遊標上的物品，防止玩家把物品藏在遊標上逃避檢測
            ItemStack itemStack = slot == -1 ? player.getItemOnCursor() : inventory.getItem(slot);
            if (keywords != null) {
                if (keywordCheckRange.equals("name") || keywordCheckRange.equals("all")) {
                    String displayName = itemStack.getItemMeta().getDisplayName();
                    if (displayName == null) continue;
                    for (String keyword : keywords) {
                        if (displayName.contains(keyword)) {
                            return itemStack;
                        }
                    }
                }
                if (keywordCheckRange.equals("lore") || keywordCheckRange.equals("all")) {
                    List<String> lores = itemStack.getItemMeta().getLore();
                    if (lores == null) continue;
                    for (String lore : lores) {
                        for (String keyword : keywords) {
                            if (lore.contains(keyword)) {
                                return itemStack;
                            }
                        }
                    }
                }
            }
            if (types != null) {
                for (String type : types) {
                    String[] clips = type.split(":");
                    String material = clips[0];
                    byte subId = clips.length == 1 ? (byte) 0 : Byte.parseByte(clips[1]);
                    if (itemStack.getType().name().equals(material) && itemStack.getData().getData() == subId) {
                        return itemStack;
                    }
                }
            }
        }
        return null;
    }
}
