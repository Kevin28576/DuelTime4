package com.kevin.dueltime4.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UtilItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public UtilItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public UtilItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    public UtilItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public UtilItemBuilder setDisplayName(String displayName) {
        if (displayName != null) {
            itemMeta.setDisplayName(displayName);
        }
        return this;
    }

    public UtilItemBuilder setLore(List<String> lore) {
        itemMeta.setLore(lore);
        return this;
    }

    public UtilItemBuilder setLight() {
        itemMeta.addEnchant(Enchantment.FORTUNE, 1, true);
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public UtilItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    public UtilItemBuilder setEnchants(Map<Enchantment,Integer> enchantMap) {
        for (Map.Entry<Enchantment, Integer> kv:enchantMap.entrySet()) {
            Enchantment enchantment = kv.getKey();
            int level = kv.getValue();
            itemMeta.addEnchant(enchantment,level,false);
        }
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
