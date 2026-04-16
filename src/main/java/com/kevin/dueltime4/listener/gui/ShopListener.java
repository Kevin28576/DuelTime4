package com.kevin.dueltime4.listener.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.cache.ShopCache;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.data.pojo.ShopRewardData;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import com.kevin.dueltime4.gui.MultiPageInventory;
import com.kevin.dueltime4.gui.ShopInventory;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ShopListener implements Listener {
    @EventHandler
    public void onRedeemInShop(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();
        ShopInventory shopInventory = DuelTimePlugin.getInstance().getCustomInventoryManager().getShop();
        ShopCache shopCache = DuelTimePlugin.getInstance().getCacheManager().getShopCache();
        int totalIndex = shopInventory.checkBeforeClickFunctionItem(event, shopCache.getList().size());
        if (totalIndex < MultiPageInventory.INDEX_THRESHOLD) {
            // 不是點選的本外掛面板裡的功能內容
            return;
        }
        ShopRewardData rewardData = shopCache.getList().get(totalIndex);
        PlayerDataCache playerDataCache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
        PlayerData playerData = playerDataCache.get(playerName);
        int levelNeed = rewardData.getLevelLimit();
        int levelNow = DuelTimePlugin.getInstance().getLevelManager().getLevel(playerName);
        if (levelNow < levelNeed) {
            MsgBuilder.send(Msg.GUI_TYPE_SHOP_REDEEM_UNSUCCESSFULLY_INSUFFICIENT_LEVEL, player,
                    "" + levelNeed, "" + levelNow);
            return;
        }
        double pointNeeded = rewardData.getPoint();
        double pointNow = playerData.getPoint();
        if (pointNow < pointNeeded) {
            MsgBuilder.send(Msg.GUI_TYPE_SHOP_REDEEM_UNSUCCESSFULLY_INSUFFICIENT_POINTS, player,
                    "" + pointNeeded, "" + pointNow);
            return;
        }
        // 扣除積分
        playerData.setPoint(pointNow - pointNeeded);
        playerDataCache.set(playerName, playerData);
        // 傳送獎勵。如果揹包已滿則獎勵物品會以掉落物的方式在玩家腳下呈現
        Map<Integer, ItemStack> itemUnfitMap = player.getInventory().addItem(rewardData.getItemStack());
        if (itemUnfitMap.isEmpty()) {
            MsgBuilder.send(Msg.GUI_TYPE_SHOP_REDEEM_SUCCESSFULLY, player);
        } else {
            MsgBuilder.send(Msg.GUI_TYPE_SHOP_REDEEM_SUCCESSFULLY_BUT_DROP, player,
                    "" + itemUnfitMap.size());
            World world = player.getWorld();
            Location location = player.getLocation();
            for (ItemStack itemUnfit : itemUnfitMap.values()) {
                world.dropItem(location, itemUnfit);
            }
        }
        // 執行兌換後指令
        List<String> commands = rewardData.getCommands();
        if (commands != null) {
            for (String commandData : commands) {
                String commandExecutor = commandData.split(":")[0];
                String commandContent = commandData.substring(commandExecutor.length()+1)
                        .replace("{player}", playerName)
                        .replace("{point}", "" + pointNeeded);
                if (commandExecutor.equals("player")) {
                    Bukkit.dispatchCommand(player, commandContent);
                }
                if (commandExecutor.equals("op")) {
                    if (player.isOp()) {
                        Bukkit.dispatchCommand(player, commandContent);
                    } else {
                        player.setOp(true);
                        Bukkit.dispatchCommand(player, commandContent);
                        player.setOp(false);
                    }
                }
                if (commandExecutor.equals("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandContent);
                }
            }
        }
        // 播放音效
        player.playSound(player.getLocation(), ViaVersion
                .getSound("ENTITY_PLAYER_LEVELUP", "LEVELUP"), 1.0f, 1.0f);
        // 更新銷量
        rewardData.updateTotalRedemptionVolume();
        int[] loc = ShopCache.getLocByIndex(totalIndex);
        shopCache.set(loc[0], loc[1], loc[2], rewardData);
        // 為所有瀏覽者實時重新整理頁面
        CustomInventoryManager customInventoryManager = DuelTimePlugin.getInstance().getCustomInventoryManager();
        customInventoryManager.updatePage(customInventoryManager.getShop());
    }
}
