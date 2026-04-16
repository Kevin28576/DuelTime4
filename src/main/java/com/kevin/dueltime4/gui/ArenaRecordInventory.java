package com.kevin.dueltime4.gui;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.itemstack.GUIItem;
import com.kevin.dueltime4.util.UtilMath;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ArenaRecordInventory extends MultiPageInventory {
    private final Map<String, Integer> contentTotalNumberMap = new HashMap<>();
    private final Map<String, Integer> maxPageMap = new HashMap<>();
    private final Map<String, Long> showCooldownMap = new HashMap<>();

    public ArenaRecordInventory() {
        super(Type.ARENA_RECORD, Msg.GUI_TYPE_RECORD_TITLE,
                new HashMap<ItemStack, int[]>() {{
                    put(GUIItem.blackGlassPane, new int[]{1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 49, 51, 52});
                    put(GUIItem.whiteGlassPane, new int[]{0, 8, 45, 53});
                }},
                new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42},
                20);
        updateContentTotalNumber(DuelTimePlugin.getInstance().getCacheManager().getShopCache().getList().size());
    }

    public void updateContentTotalNumber(int contentTotalNumber, Player player) {
        this.contentTotalNumberMap.put(player.getName(), contentTotalNumber);
        this.maxPageMap.put(player.getName(), (int) Math.ceil((double) contentTotalNumber / getContentPageSize()));
    }

    public int getMaxPage(Player player) {
        return maxPageMap.get(player.getName());
    }

    public int getContentTotalNumber(Player player) {
        return this.contentTotalNumberMap.get(player.getName());
    }

    public void loadContent(Player player, Inventory inventory, int page) {
        // 檢查當前頁碼是否超過最大值
        if (page > getMaxPage(player)) {
            /*
            如果頁碼超過最大值，則將頁碼設為最大值，即最後一頁
             */
            page = getMaxPage(player);
        }
        // 獲取當前頁碼的物品數量
        int contentNumberInThisPage = Math.min(20, getContentTotalNumber(player) - (page - 1) * 20);
        // 將當前頁碼的物品依次安置
        for (int i = 0; i < contentNumberInThisPage; i++) {
            // 根據迭代序號獲取slot序號
            int contentSlot = getContentSlots()[i];
            // 獲取要安置的物品
            ItemStack itemStack = GUIItem.getArenaRecord((page - 1) * 20 + i, player);
            inventory.setItem(contentSlot, itemStack);
        }
        // 如果當前瀏覽的是最後一頁，考慮到最後一頁可能不會填滿20個內容槽，所以要將無需安置物品的內容槽清空
        if (page == getMaxPage(player)) {
            for (int i = contentNumberInThisPage; i < 20; i++) {
                int contentSlot = getContentSlots()[i];
                inventory.setItem(contentSlot, null);
            }
        }
    }

    public void openFor(Player player) {
        // 先檢查是否為外掛載入後首次載入該玩家的記錄快取，如果是，則要請示ArenaRecordCache載入該玩家的快取，再呼叫updateContentTotalNumber()回傳記錄總數等資料
        DuelTimePlugin.getInstance().getCacheManager().getArenaRecordCache().reload(player);
        // 判斷是否有內容
        if (getMaxPage(player) == 0) {
            MsgBuilder.send(Msg.GUI_TYPE_RECORD_EMPTY, player);
            return;
        }
        // 建立Inventory容器
        Inventory inventory = Bukkit.createInventory(new CustomInventoryHolder(Type.ARENA_RECORD), 54,
                MsgBuilder.get(getTitleMsg(), player,
                        player.getName()));
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
    }

    public boolean isShowAvailable(String playerName) {
        return !showCooldownMap.containsKey(playerName) || showCooldownMap.get(playerName) < System.currentTimeMillis();
    }

    public void updateShowCooldown(String playerName) {
        showCooldownMap.put(playerName, System.currentTimeMillis() + DuelTimePlugin.getInstance().getCfgManager().getRecordShowCooldown() * 1000L);
    }

    public double getCooldownLeft(String playerName) {
        return UtilMath.round((showCooldownMap.get(playerName) - System.currentTimeMillis()) / 1000.0);
    }
}
