package com.kevin.dueltime4.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.itemstack.GUIItem;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartInventory extends MultiPageInventory {

    public StartInventory() {
        super(Type.START, Msg.GUI_TYPE_START_TITLE,
                new HashMap<ItemStack, int[]>() {{
                    put(GUIItem.blackGlassPane, new int[]{1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 49, 51, 52});
                    put(GUIItem.whiteGlassPane, new int[]{0, 8, 45, 53});
                }},
                new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42},
                20);
        updateContentTotalNumber(DuelTimePlugin.getInstance().getArenaManager().getMap().size());
    }

    public void loadContent(Player player, Inventory inventory, int page) {
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        List<BaseArena> arenaList = arenaManager.getList();
        for (int i = 0; i < Math.min(20, arenaList.size() - (page - 1) * 20); i++) {
            ItemStack itemStack = GUIItem.getArenaInfo(arenaList.get((page - 1) * 20 + i).getArenaData().getId(), player);
            inventory.setItem(getContentSlots()[i], itemStack);
        }
        int maxPage = (int) Math.ceil((double) arenaList.size() / getContentPageSize());
        if (maxPage > 1 && page >= maxPage) {
            for (int i = arenaList.size() - (page - 1) * getContentPageSize(); i < getContentPageSize(); i++) {
                int contentSlot = getContentSlots()[i];
                inventory.setItem(contentSlot, null);
            }
        }
    }

    public void openFor(Player player) {
        if (DuelTimePlugin.getInstance().getArenaManager().size() == 0) {
            MsgBuilder.send(Msg.GUI_TYPE_START_EMPTY, player);
            return;
        }
        // 建立Inventory容器
        Inventory inventory = Bukkit.createInventory(new CustomInventoryHolder(Type.START), 54,
                MsgBuilder.get(getTitleMsg(), player));
        // 布設裝飾用的物品
        for (Map.Entry<ItemStack, int[]> kv : getDecorateSlotMap().entrySet()) {
            ItemStack decorateItemStack = kv.getKey();
            int[] slots = kv.getValue();
            for (int slot : slots) {
                inventory.setItem(slot, decorateItemStack);
            }
        }
        // 安置翻頁按鈕
        inventory.setItem(48, GUIItem.getButtonLast(player));
        inventory.setItem(50, GUIItem.getButtonNext(player));
        // 根據玩家在快取中的瀏覽頁數，載入內容物
        String playerName = player.getName();
        int nowPage;
        if (getPageMap().containsKey(playerName)) {
            nowPage = getPageMap().get(playerName);
        } else {
            // 如果用來儲存玩家當前瀏覽頁碼的pageMap快取中沒有該玩家，則新增快取
            nowPage = 1;
            getPageMap().put(playerName, 1);
        }
        loadContent(player, inventory, nowPage);
        player.openInventory(inventory);
        addViewer(playerName);
    }
}
