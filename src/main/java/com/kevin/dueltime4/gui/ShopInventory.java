package com.kevin.dueltime4.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.itemstack.GUIItem;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShopInventory extends MultiPageInventory {

    public ShopInventory() {
        super(Type.SHOP, Msg.GUI_TYPE_SHOP_TITLE,
                new HashMap<ItemStack, int[]>() {{
                    put(GUIItem.blackGlassPane, new int[]{1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 49, 51, 52});
                    put(GUIItem.whiteGlassPane, new int[]{0, 8, 45, 53});
                }},
                new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42},
                20);
        // 初始化時，拉取商品總數
        updateContentTotalNumber(DuelTimePlugin.getInstance().getCacheManager().getShopCache().getList().size());
    }

    public void loadContent(Player player, Inventory inventory, int page) {
        // 檢查當前頁碼是否超過最大值
        if (page > getMaxPage()) {
            /*
            如果頁碼超過最大值，則將頁碼設為最大值，即最後一頁
            這種情況的一般觸發條件：
            玩家瀏覽最後一頁後關閉了這個GUI，隨後管理員刪除了一些內容物，使頁碼總數發生變化，最後一頁的頁碼數自然也發生變化
            當玩家再次開啟這個GUI時，會根據pageMap中儲存的瀏覽頁數而開啟原先的最後一頁，那麼這時候頁碼就會超過現在的最大頁碼
             */
            page = getMaxPage();
        }
        // 獲取當前頁碼的物品數量
        int contentNumberInThisPage = Math.min(20, getContentTotalNumber() - (page - 1) * 20);
        // 將當前頁碼的物品依次安置
        for (int i = 0; i < contentNumberInThisPage; i++) {
            // 根據迭代序號獲取slot序號
            int contentSlot = getContentSlots()[i];
            // 獲取要安置的物品
            ItemStack itemStack = GUIItem.getShopReward((page - 1) * 20 + i, player);
            inventory.setItem(contentSlot, itemStack);
        }
        // 如果當前瀏覽的是最後一頁，考慮到最後一頁可能不會填滿20個內容槽，所以要將無需安置物品的內容槽清空
        if (page == getMaxPage()) {
            for (int i = contentNumberInThisPage; i < 20; i++) {
                int contentSlot = getContentSlots()[i];
                inventory.setItem(contentSlot, null);
            }
        }
    }

    public void openFor(Player player) {
        if (getMaxPage() == 0) {
            MsgBuilder.send(Msg.GUI_TYPE_SHOP_EMPTY, player);
            return;
        }
        // 建立Inventory容器
        Inventory inventory = Bukkit.createInventory(new CustomInventoryHolder(Type.SHOP), 54, MsgBuilder.get(getTitleMsg(), player));
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
