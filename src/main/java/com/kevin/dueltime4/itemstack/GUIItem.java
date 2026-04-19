package com.kevin.dueltime4.itemstack;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseArenaData;
import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.data.pojo.ShopRewardData;
import com.kevin.dueltime4.util.UtilItemBuilder;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIItem {
    public static ItemStack blackGlassPane = new UtilItemBuilder(ViaVersionItem.getGlassPaneType(15)).setDisplayName(" ").build();
    public static ItemStack whiteGlassPane = new UtilItemBuilder(ViaVersionItem.getGlassPaneType(0)).setDisplayName(" ").build();

    public static ItemStack getButtonLast(Player player) {
        return new UtilItemBuilder(Material.PAPER).setDisplayName(MsgBuilder.get(Msg.ITEM_GUI_SHOP_BUTTON_PREVIOUS_PAGE_NAME, player)).build();
    }

    public static ItemStack getButtonNext(Player player) {
        return new UtilItemBuilder(Material.PAPER).setDisplayName(MsgBuilder.get(Msg.ITEM_GUI_SHOP_BUTTON_NEXT_PAGE_NAME, player)).build();
    }

    public static ItemStack getShopReward(int index, Player player) {
        ShopRewardData rewardData = DuelTimePlugin.getInstance().getCacheManager().getShopCache().getList().get(index);
        ItemStack rewardItemStack = rewardData.getItemStack();
        int amount = rewardItemStack.getAmount();
        String displayName = null;
        List<String> lore = new ArrayList<>();
        ItemMeta rewardItemStackMeta = rewardItemStack.getItemMeta();
        Map<Enchantment, Integer> enchantmentMap = new HashMap<>();
        if (rewardItemStackMeta != null) {
            if (rewardItemStackMeta.hasDisplayName()) {
                displayName = rewardItemStackMeta.getDisplayName();
            }
            if (rewardItemStackMeta.hasLore()) {
                lore = rewardItemStackMeta.getLore();
            }
            if (rewardItemStackMeta.hasEnchants()) {
                enchantmentMap = rewardItemStackMeta.getEnchants();
            }
        }

        lore.addAll(MsgBuilder.gets(
                Msg.ITEM_GUI_SHOP_REWARD_INFORMATION,
                player,
                String.valueOf(rewardData.getPoint()),
                String.valueOf(rewardData.getTotalRedemptionVolume()),
                rewardData.getDescription() != null ? rewardData.getDescription() : "-"));

        int levelNow = DuelTimePlugin.getInstance().getLevelManager().getLevel(player.getName());
        int levelNeeded = rewardData.getLevelLimit();
        double pointNow = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache().get(player.getName()).getPoint();
        double pointNeeded = rewardData.getPoint();
        String tip;
        if (levelNow < levelNeeded) {
            tip = MsgBuilder.get(Msg.ITEM_GUI_SHOP_REWARD_REDEEM_TIP_NO_ENOUGH_LEVEL, player, String.valueOf(levelNow), String.valueOf(levelNeeded));
        } else if (pointNow < pointNeeded) {
            tip = MsgBuilder.get(Msg.ITEM_GUI_SHOP_REWARD_REDEEM_TIP_NO_ENOUGH_POINT, player, String.valueOf(pointNow), String.valueOf(pointNeeded));
        } else {
            tip = MsgBuilder.get(Msg.ITEM_GUI_SHOP_REWARD_REDEEM_TIP_YES, player, String.valueOf(pointNow), String.valueOf(pointNeeded));
        }
        lore.add(tip);
        if (levelNeeded != 0 && levelNow > levelNeeded) {
            lore.add(MsgBuilder.get(Msg.ITEM_GUI_SHOP_REWARD_LEVEL_LIMIT_TIP_YES, player, String.valueOf(levelNeeded)));
        }

        return new UtilItemBuilder(rewardItemStack.getType())
                .setAmount(amount)
                .setDisplayName(displayName)
                .setLore(lore)
                .setEnchants(enchantmentMap)
                .build();
    }

    public static ItemStack getArenaRecord(int index, Player player) {
        BaseRecordData recordData = DuelTimePlugin.getInstance().getCacheManager().getArenaRecordCache().get(player.getName()).get(index);
        ItemStack itemStack = new ItemStack(ViaVersionItem.getMapMaterial(), 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(recordData.getItemStackTitle());
        List<String> lore = recordData.getItemStackContent();
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        boolean isArenaRecordShowEnabled = cfgManager.isRecordShowEnabled();
        boolean isArenaRecordPrintEnabled = cfgManager.isRecordPrintEnabled();
        if (isArenaRecordShowEnabled || isArenaRecordPrintEnabled) {
            lore.add("");
            if (isArenaRecordShowEnabled) {
                lore.add(MsgBuilder.get(Msg.ITEM_GUI_RECORD_SHOW_TIP, player));
            }
            if (isArenaRecordPrintEnabled) {
                lore.add(MsgBuilder.get(Msg.ITEM_GUI_RECORD_PRINT_TIP, player, String.valueOf(cfgManager.getRecordPrintCost())));
            }
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack getArenaInfo(String id, Player player) {
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena = arenaManager.get(id);
        BaseArenaData arenaData = arena.getArenaData();
        Object iconData = arena.getArenaType().getPresets().get(ArenaType.PresetType.START_ICON);
        ItemStack itemStack = new ItemStack(iconData != null ? (Material) iconData : Material.PAPER, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(arenaData.getName());

        Msg stateMsg;
        Msg buttonMsg;
        String stateText;
        String leftPlayerNumber = String.valueOf(arena.getGamerDataList().size());
        String rightPlayerNumber = arenaData.getMaxPlayerNumber() > 0 ? String.valueOf(arenaData.getMaxPlayerNumber()) : "-";
        String queueSoundLine = null;

        switch (arena.getState()) {
            case WAITING:
                stateMsg = Msg.ITEM_GUI_START_ARENA_STATE_WAITING;
                int waitingCount = arenaManager.getWaitingPlayers(id).size();
                if (arenaManager.getWaitingPlayers(id).contains(player.getName())) {
                    buttonMsg = Msg.ITEM_GUI_START_ARENA_BUTTON_MESSAGE_WAITING_STOP;
                    itemMeta.addEnchant(Enchantment.LURE, 1, true);
                    if (ViaVersionItem.isHasItemFlagMethod()) {
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    }
                } else {
                    buttonMsg = Msg.ITEM_GUI_START_ARENA_BUTTON_MESSAGE_WAITING_START;
                }
                leftPlayerNumber = String.valueOf(waitingCount);
                rightPlayerNumber = String.valueOf(arenaData.getMinPlayerNumber());
                long etaSeconds = arenaManager.isWaitingPlayerForArena(player.getName(), id)
                        ? arenaManager.getEstimatedQueueRemainingSeconds(player.getName(), id)
                        : arenaManager.getEstimatedQueueWaitSeconds(id);
                String etaSuffix = DynamicLang.get(
                        player,
                        "Dynamic.queue.gui-waiting-eta-suffix",
                        " &8| &7ETA: &f{eta}&7s",
                        "eta", String.valueOf(etaSeconds))
                        .replace('§', '&');
                stateText = MsgBuilder.get(stateMsg, player, String.valueOf(waitingCount)) + etaSuffix;
                PlayerData playerData = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache().getAnyway(player.getName());
                boolean queueSoundEnabled = playerData == null || playerData.isQueueSoundEnabled();
                queueSoundLine = DynamicLang.get(player,
                        "Dynamic.queue.sound.gui-line",
                        "&7排隊提醒音效：{state}",
                        "state", DynamicLang.get(player,
                                queueSoundEnabled ? "Dynamic.queue.sound.state-on" : "Dynamic.queue.sound.state-off",
                                queueSoundEnabled ? "&a開啟" : "&c關閉"));
                break;
            case IN_PROGRESS_CLOSED:
                stateMsg = Msg.ITEM_GUI_START_ARENA_STATE_IN_PROGRESS_CLOSED;
                buttonMsg = Msg.ITEM_GUI_START_ARENA_BUTTON_MESSAGE_IN_PROGRESS_CLOSED;
                stateText = MsgBuilder.get(stateMsg, player);
                break;
            case IN_PROGRESS_OPENED:
                stateMsg = Msg.ITEM_GUI_START_ARENA_STATE_IN_PROGRESS_OPENED;
                int maxPlayerNumber = arenaData.getMaxPlayerNumber();
                buttonMsg = maxPlayerNumber > 0 && arena.getGamerDataList().size() < maxPlayerNumber
                        ? Msg.ITEM_GUI_START_ARENA_BUTTON_MESSAGE_IN_PROGRESS_OPENED
                        : Msg.ITEM_GUI_START_ARENA_BUTTON_MESSAGE_IN_PROGRESS_OPENED_FULL;
                stateText = MsgBuilder.get(stateMsg, player);
                break;
            default:
                stateMsg = Msg.ITEM_GUI_START_ARENA_STATE_DISABLED;
                buttonMsg = Msg.ITEM_GUI_START_ARENA_BUTTON_MESSAGE_DISABLED;
                stateText = MsgBuilder.get(stateMsg, player);
                break;
        }

        List<String> lore = MsgBuilder.gets(
                Msg.ITEM_GUI_START_ARENA_INFORMATION,
                player,
                stateText,
                leftPlayerNumber,
                rightPlayerNumber,
                MsgBuilder.get(buttonMsg, player));
        if (queueSoundLine != null) {
            lore.add("");
            lore.add(queueSoundLine);
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
