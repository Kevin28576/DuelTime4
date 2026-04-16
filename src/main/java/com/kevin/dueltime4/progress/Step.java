package com.kevin.dueltime4.progress;

import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.entity.Player;

public class Step {
    private final Player player;
    private final Object tip;
    private final Object finishTitle;
    private final Object finishSubTitle;
    private final Class<?> dataType;
    /**
     * 關於自動上傳
     * 在步驟中，對於部分型別的資料，可能不需要額外編寫邏輯，因為本外掛已預先寫好了一些常用的上傳邏輯
     * 當前已有的邏輯有：
     * String型別：透過聊天框輸入上傳，可自動替換顏色符號
     * Integer型別：透過聊天框輸入上傳，自動識別格式，可自動篩選出正值
     * Double/Float型別：透過聊天框輸入上傳，自動識別格式，可自動篩選出正值
     * Location型別：透過點選方塊/直接點選螢幕上傳
     * ItemStack型別：手持物品點選螢幕上傳
     * 如果上述邏輯符合你的需求，可以直接將autoUpload設定為true
     * 如果上述邏輯不符合你的需求，或不包含你需求的型別，請將autoUpdate設定為false，並自行額外編寫邏輯
     */
    private final boolean autoUpload;
    private final AutoUploadTag[] autoUploadTags;
    private Object data;

    public Step(Object tip, Object finishTitle, Object finishSubTitle, Player player, Class<?> dataType, boolean autoUpload, AutoUploadTag... autoUploadTags) {
        if (!(tip instanceof String) && !(tip instanceof Msg)) {
            throw new IllegalArgumentException("The 1st argument must be String or Msg");
        }
        if (!(finishTitle instanceof String) && !(finishTitle instanceof Msg)) {
            throw new IllegalArgumentException("The 2nd argument must be String or Msg");
        }
        if (!(finishSubTitle instanceof String) && !(finishSubTitle instanceof Msg)) {
            throw new IllegalArgumentException("The 3rd argument must be String or Msg");
        }
        this.tip = tip;
        this.finishTitle = finishTitle;
        this.finishSubTitle = finishSubTitle;
        this.player = player;
        this.autoUpload = autoUpload;
        this.autoUploadTags = autoUploadTags;
        this.dataType = dataType;
    }

    public String getTip() {
        return (tip instanceof String) ?
                (String) tip :
                MsgBuilder.get((Msg) tip, player);
    }

    public String getFinishTitle() {
        return (finishTitle instanceof String) ?
                (String) finishTitle :
                MsgBuilder.get((Msg) finishTitle, player);
    }

    public String getFinishSubTitle() {
        return (finishSubTitle instanceof String) ?
                (String) finishSubTitle :
                MsgBuilder.get((Msg) finishSubTitle, player);
    }

    public Player getPlayer() {
        return player;
    }

    public Class<?> getDataType() {
        return dataType;
    }

    public boolean isAutoUpload() {
        return autoUpload;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public AutoUploadTag[] getAutoUploadTags() {
        return autoUploadTags == null ? new AutoUploadTag[0] : autoUploadTags;
    }

    public boolean hasAutoUploadTags(AutoUploadTag autoUploadTag) {
        if (autoUploadTags == null) {
            return false;
        }
        for (AutoUploadTag autoUploadTagDefined : autoUploadTags) {
            if (autoUploadTagDefined.equals(autoUploadTag)) {
                return true;
            }
        }
        return false;
    }

    public enum AutoUploadTag {
        STRING_CONDITION_ID_STYLE, // 只容許英文字母的字串
        STRING_FUNCTION_REPLACE_BLANK, // 自動替換字串中的空格
        STRING_FUNCTION_REPLACE_COLOR_SYMBOL, // 自動替換顏色符號
        STRING_FUNCTION_TO_UPPERCASE, // 自動轉為大寫
        STRING_FUNCTION_TO_LOWERCASE, // 自動轉為小寫
        INTEGER_CONDITION_POSITIVE_VALUE, // 只容許正值的整數
        DOUBLE_CONDITION_POSITIVE_VALUE, // 只容許正值的小數
        LOCATION_CONDITION_CLICK_AIR, // 點選空氣記錄當前站立點
        LOCATION_CONDITION_CLICK_BLOCK, // 點選方塊記錄方塊點
        LOCATION_CONDITION_THE_SAME_WORLD, // 位置必須和上一個步驟的位置位於同一個世界
        LOCATION_CONDITION_DIFFERENT_BLOCK, // 位置必須和上一個步驟的位置不在同一個方塊
        LOCATION_CONDITION_CANNOT_OVERLAP_WITH_OTHER_ARENA, // 位置必須和上一個步驟的位置所構成的三維區域不能與其他競技場的三維區域有交疊
        LIST_CONDITION_STRING_INTEGER_PAIR, // 列表內容必須為字串-整數對，用英文冒號來分割，例如物品種類檢測情境下，"wool:3"表示子id為3的染色羊毛（淡藍色）
        LIST_CONDITION_STRING_INTEGER_PAIR_LOOSE, // 意義和LIST_CONDITION_STRING_INTEGER_PAIR類似，但容許只提供字串，如"wool"表示任何一種羊毛
        LIST_CONDITION_IDENTITY_COMMAND_PAIR, // 列表內容為字串對，前者為身份(player,op,console)，後者為指令內容，例如"console:fly {player}"表示透過後臺執行讓玩家飛行的指令（注意要自行將{player}等佔位符在功能邏輯中替換）
        LIST_CONDITION_NULLABLE, // 列表內容可以為空
    }
}
