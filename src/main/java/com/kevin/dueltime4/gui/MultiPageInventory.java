package com.kevin.dueltime4.gui;

import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MultiPageInventory {
    private final Type type;
    private final Msg titleMsg;
    private final Map<ItemStack, int[]> decorateSlotMap;
    private final int[] contentSlots;
    private final Map<String, Integer> pageMap = new HashMap<>();
    private final int contentPageSize;
    private int contentTotalNumber;
    private int maxPage;
    private final List<String> viewers = new ArrayList<>();

    protected MultiPageInventory(Type type, Msg titleMsg, Map<ItemStack, int[]> decorateSlotMap, int[] contentSlots, int contentPageSize) {
        this.type = type;
        this.titleMsg = titleMsg;
        this.decorateSlotMap = decorateSlotMap;
        this.contentSlots = contentSlots;
        this.contentPageSize = contentPageSize;
    }

    /**
     * 接收來自快取系統的更新通知
     * 更新內容物品總數量，並根據內容物品總數量計算當前能劃分的最大頁碼
     *
     * @param contentTotalNumber 快取中內容物品的總數量
     */
    public void updateContentTotalNumber(int contentTotalNumber) {
        this.contentTotalNumber = contentTotalNumber;
        // 頁碼的最大值以兌換物總數為依據，透過向上取整的方式計算。這裡的contentPageSize即單頁的容量
        this.maxPage = (int) Math.ceil((double) contentTotalNumber / contentPageSize);
    }

    /**
     * 根據頁碼載入對應的內容物品
     *
     * @param page 頁碼（從1開始記，與生活常識一致）
     */
    public abstract void loadContent(Player player, Inventory inventory, int page);

    /**
     * 為某個玩家開啟商城面板
     */
    public abstract void openFor(Player player);

    public Msg getTitleMsg() {
        return titleMsg;
    }

    public Map<ItemStack, int[]> getDecorateSlotMap() {
        return decorateSlotMap;
    }

    public int[] getContentSlots() {
        return contentSlots;
    }

    public Map<String, Integer> getPageMap() {
        return pageMap;
    }

    public int getContentPageSize() {
        return contentPageSize;
    }

    public int getContentTotalNumber() {
        return contentTotalNumber;
    }

    public int getMaxPage() {
        return maxPage;
    }

    public int getPlayerPage(String playerName) {
        return pageMap.getOrDefault(playerName, 1);
    }

    public void updatePlayerPage(String playerName, int page) {
        pageMap.put(playerName, page);
    }

    public void addViewer(String playerName) {
        viewers.add(playerName);
    }

    public void removeViewer(String playerName) {
        viewers.remove(playerName);
    }

    public List<String> getViewers() {
        return viewers;
    }

    public static final int INDEX_THRESHOLD = -10; // 在checkBeforeClickContent方法中，用於區分返回值為負值時，具體情況是點選了非內容區功能物品([-10,-1])還是其餘情況((-∞,-11])

    public int checkBeforeClickFunctionItem(InventoryClickEvent event, int realSize) {
        Player player = (Player) event.getWhoClicked();
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof CustomInventoryHolder) || ((CustomInventoryHolder) inventoryHolder).getType() != type) {
            // 用holder判定代替title字串判定，有利於減少漏洞
            return -11;
        }
        // 識別為本外掛的特定面板後，取消點選事件
        event.setCancelled(true);
        int[] contentSlots = getContentSlots();
        int slotClicked = event.getSlot();
        int index = -1; // 當前點選的槽序號對應本頁內容區的第幾個物品
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slotClicked) {
                index = i; // 找到了，將索引值賦給index
                break; // 找到後跳出迴圈
            }
        }
        String playerName = player.getName();
        int nowPage = getPlayerPage(playerName);
        int maxPage = (int) Math.ceil(realSize / (double) getContentSlots().length);
        // 若未找到索引，說明點選的不是內容區
        if (index == -1) {
            Inventory inventory = event.getInventory();
            // 點選的是前往上一頁的按鈕
            if (slotClicked == 48) {
                if (nowPage == 1) {
                    MsgBuilder.send(Msg.GUI_ALREADY_THE_FIRST_PAGE, player);
                    return -12;
                }
                loadContent(player, inventory, nowPage - 1);
                updatePlayerPage(playerName, nowPage - 1);
                return -13;
            }
            // 點選的是前往下一頁的按鈕
            if (slotClicked == 50) {
                if (nowPage >= maxPage) {
                    MsgBuilder.send(Msg.GUI_ALREADY_THE_LAST_PAGE, player);
                    return -14;
                }
                loadContent(player, inventory, nowPage + 1);
                updatePlayerPage(playerName, nowPage + 1);
                return -15;
            }
            // 如果點選的既不是內容區的物品，又不是按鈕，則return
            return -16;
        } else if (index + 1 > realSize) {
            /*
            如果點選的槽位對應的總序號超出值域，則return
            這種情況一般發生在點選最後一頁內容區的空白槽中
             */
            return -17;
        }
        return index + (pageMap.get(player.getName())-1) * contentPageSize;
    }

    public enum Type {
        START, SHOP, ARENA_RECORD
    }
}
