package com.kevin.dueltime4.listener.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.cache.RecordCache;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.gui.ArenaRecordInventory;
import com.kevin.dueltime4.gui.MultiPageInventory;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArenaRecordListener implements Listener {
    @EventHandler
    public void onViewArenaRecord(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();
        ArenaRecordInventory recordInventory = DuelTimePlugin.getInstance().getCustomInventoryManager().getArenaRecord();
        RecordCache cache = DuelTimePlugin.getInstance().getCacheManager().getArenaRecordCache();
        int totalIndex = recordInventory.checkBeforeClickFunctionItem(event, cache.get(playerName).size());
        if (totalIndex < MultiPageInventory.INDEX_THRESHOLD) {
            // 不是點選的本外掛面板裡的功能內容
            return;
        }
        BaseRecordData recordData = cache.get(playerName).get(totalIndex);
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        if (event.getClick().equals(ClickType.LEFT) && cfgManager.isRecordShowEnabled()) {
            if (!recordInventory.isShowAvailable(playerName)) {
                MsgBuilder.send(Msg.GUI_TYPE_RECORD_SHOW_FREQUENTLY, player, "" + recordInventory.getCooldownLeft(playerName));
                return;
            }
            recordInventory.updateShowCooldown(playerName);
            for (TextComponent textComponent : MsgBuilder.getClickable(Msg.GUI_TYPE_RECORD_SHOW_CONTENT, player,false,
                    playerName,
                    String.join("||", recordData.getItemStackContent()))) {
                Bukkit.spigot().broadcast(textComponent);
            }
        }
        if (event.getClick().equals(ClickType.RIGHT) && cfgManager.isRecordPrintEnabled()) {
            ItemStack itemInHand = ViaVersionItem.getItemInMainHand(player);
            if (itemInHand == null || !itemInHand.getType().equals(Material.PAPER)) {
                MsgBuilder.send(Msg.GUI_TYPE_RECORD_PRINT_FAIL_NO_PAPER_IN_HAND, player);
                return;
            }
            if (itemInHand.hasItemMeta()) {
                MsgBuilder.send(Msg.GUI_TYPE_RECORD_PRINT_FAIL_PAPER_HAS_META, player);
                return;
            }
            PlayerDataCache playerDataCache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
            PlayerData playerData = playerDataCache.get(playerName);
            double pointNeed = cfgManager.getRecordPrintCost();
            double pointNow = playerDataCache.get(playerName).getPoint();
            if (pointNow < pointNeed) {
                MsgBuilder.send(Msg.GUI_TYPE_RECORD_PRINT_INSUFFICIENT_POINT, player,
                        "" + pointNeed, "" + pointNow);
                return;
            }
            // 執行積分消耗
            playerData.setPoint(pointNow - pointNeed);
            playerDataCache.set(playerName, playerData);
            // 實現記錄列印
            ItemMeta itemMetaInHand = itemInHand.getItemMeta();
            itemMetaInHand.setDisplayName(recordData.getItemStackTitle());
            itemMetaInHand.setLore(recordData.getItemStackContent());
            itemInHand.setItemMeta(itemMetaInHand);
            ViaVersionItem.setItemInMainHand(player, itemInHand);
            MsgBuilder.send(Msg.GUI_TYPE_RECORD_PRINT_SUCCESSFULLY, player);
        }
    }
}
