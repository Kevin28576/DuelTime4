package com.kevin.dueltime4.arena.type;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.progress.ProgressType;
import com.kevin.dueltime4.progress.Step;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.message.Msg;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kevin.dueltime4.arena.type.ArenaType.FunctionInternalType.*;
import static com.kevin.dueltime4.progress.Step.AutoUploadTag.*;

public class ArenaTypeManager {
    private final List<ArenaType> arenaTypeList = new ArrayList<>();

    public ArenaTypeManager() {
        reload();
    }

    /**
     * （重新）載入所有種類定義
     */
    public void reload() {
        Step[] steps = new Step[]{
                // 點選方塊確定空間對角點1
                new Step(
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_1_TIP,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_1_TITLE,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_1_SUBTITLE,
                        null,
                        Location.class,
                        true,
                        LOCATION_CONDITION_CLICK_BLOCK),
                // 點選方塊確定空間對角點2
                new Step(
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_2_TIP,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_2_TITLE,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_2_SUBTITLE,
                        null,
                        Location.class,
                        true,
                        LOCATION_CONDITION_CLICK_BLOCK,
                        LOCATION_CONDITION_THE_SAME_WORLD,
                        LOCATION_CONDITION_DIFFERENT_BLOCK,
                        LOCATION_CONDITION_CANNOT_OVERLAP_WITH_OTHER_ARENA),
                // 點選空氣確定傳送點1
                new Step(
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_3_TIP,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_3_TITLE,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_3_SUBTITLE,
                        null,
                        Location.class,
                        true,
                        LOCATION_CONDITION_CLICK_AIR),
                // 點選空氣確定傳送點2
                new Step(
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_4_TIP,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_4_TITLE,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_4_SUBTITLE,
                        null,
                        Location.class,
                        true,
                        LOCATION_CONDITION_CLICK_AIR,
                        LOCATION_CONDITION_THE_SAME_WORLD),
                // 輸入競技場ID
                new Step(
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_5_TIP,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_5_TITLE,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_5_SUBTITLE,
                        null,
                        String.class,
                        true,
                        STRING_CONDITION_ID_STYLE),
                // 輸入競技場展示名
                new Step(
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_6_TIP,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_6_TITLE,
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_STEP_6_SUBTITLE,
                        null,
                        String.class,
                        true,
                        STRING_FUNCTION_REPLACE_BLANK, STRING_FUNCTION_REPLACE_COLOR_SYMBOL)
        };
        Map<String, ArenaType.Function> functionDef =
                new HashMap<String, ArenaType.Function>() {{
                    // 時間限制
                    put(CLASSIC_TIME_LIMIT.getId(), new ArenaType.Function(CLASSIC_TIME_LIMIT.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_TIME_LIMIT_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_TIME_LIMIT_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_TIME_LIMIT.getId(),
                            // 輸入秒數
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_TIME_LIMIT_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_TIME_LIMIT_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_TIME_LIMIT_STEP_1_SUBTITLE,
                                    null,
                                    Integer.class,
                                    true,
                                    INTEGER_CONDITION_POSITIVE_VALUE)));
                    // 賽前倒計時
                    put(CLASSIC_COUNTDOWN.getId(), new ArenaType.Function(CLASSIC_COUNTDOWN.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_COUNTDOWN_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_COUNTDOWN_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_COUNTDOWN.getId(),
                            // 輸入秒數
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_COUNTDOWN_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_COUNTDOWN_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_COUNTDOWN_STEP_1_SUBTITLE,
                                    null,
                                    Integer.class,
                                    true,
                                    INTEGER_CONDITION_POSITIVE_VALUE),
                            // 輸入T/F表示倒計時期間是否可以移動
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_COUNTDOWN_STEP_2_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_COUNTDOWN_STEP_2_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_COUNTDOWN_STEP_2_SUBTITLE,
                                    null,
                                    Boolean.class,
                                    true)));
                    // 賽前揹包檢測（依關鍵詞)
                    put(CLASSIC_INVENTORY_CHECK_KEYWORD.getId(), new ArenaType.Function(CLASSIC_INVENTORY_CHECK_KEYWORD.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_KEYWORD_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_KEYWORD_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_INVENTORY_CHECK_KEYWORD.getId(),
                            // 輸入檢查的範圍，name代表展示名，lore代表描述，all代表都檢查
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_1_SUBTITLE,
                                    null,
                                    String.class,
                                    false),
                            // 輸入列表
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_2_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_2_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_KEYWORD_STEP_2_SUBTITLE,
                                    null,
                                    List.class,
                                    true)));
                    // 賽前揹包檢測（依物品種類)
                    put(CLASSIC_INVENTORY_CHECK_TYPE.getId(), new ArenaType.Function(CLASSIC_INVENTORY_CHECK_TYPE.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_TYPE_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_TYPE_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_INVENTORY_CHECK_TYPE.getId(),
                            // 輸入列表，元素為字串-整數對
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_TYPE_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_TYPE_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_INVENTORY_CHECK_TYPE_STEP_1_SUBTITLE,
                                    null,
                                    List.class,
                                    true,
                                    LIST_CONDITION_STRING_INTEGER_PAIR_LOOSE,
                                    STRING_FUNCTION_TO_UPPERCASE)));
                    // 賽前入場後指令
                    put(CLASSIC_PRE_GAME_COMMAND.getId(), new ArenaType.Function(CLASSIC_PRE_GAME_COMMAND.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_PRE_GAME_COMMAND_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_PRE_GAME_COMMAND_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_PRE_GAME_COMMAND.getId(),
                            // 輸入列表，元素為字串對，前者為身份名，後者為指令
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_PRE_GAME_COMMAND_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_PRE_GAME_COMMAND_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_PRE_GAME_COMMAND_STEP_1_SUBTITLE,
                                    null,
                                    List.class,
                                    true,
                                    LIST_CONDITION_IDENTITY_COMMAND_PAIR)));
                    // 觀戰
                    put(CLASSIC_SPECTATE.getId(), new ArenaType.Function(CLASSIC_SPECTATE.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_SPECTATE.getId(),
                            // 傳入觀戰席區域A點
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_1_SUBTITLE,
                                    null,
                                    Location.class,
                                    true,
                                    LOCATION_CONDITION_CLICK_BLOCK),
                            // 傳入觀戰席區域B點
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_2_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_2_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_2_SUBTITLE,
                                    null,
                                    Location.class,
                                    true,
                                    LOCATION_CONDITION_CLICK_BLOCK,
                                    LOCATION_CONDITION_DIFFERENT_BLOCK,
                                    LOCATION_CONDITION_THE_SAME_WORLD),
                            // 傳入觀眾傳送點
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_3_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_3_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_3_SUBTITLE,
                                    null,
                                    Location.class,
                                    true,
                                    LOCATION_CONDITION_CLICK_AIR),
                            // 傳入是否向觀眾實時展示對戰雙方血量
                            new Step(
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_4_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_4_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_SPECTATOR_STEP_4_SUBTITLE,
                                    null,
                                    Boolean.class,
                                    true)
                    ));
                    put(CLASSIC_BAN_ENTITY_SPAWN.getId(), new ArenaType.Function(CLASSIC_BAN_ENTITY_SPAWN.getId(), Msg.ARENA_TYPE_CLASSIC_FUNCTION_BAN_ENTITY_SPAWN_NAME, Msg.ARENA_TYPE_CLASSIC_FUNCTION_BAN_ENTITY_SPAWN_DESCRIPTION,
                            DuelTimePlugin.getInstance(),
                            ProgressType.InternalType.ADD_FUNCTION_CLASSIC_BAN_ENTITY_SPAWN.getId(),
                            new Step(Msg.PROGRESS_TYPE_SET_FUNCTION_BAN_ENTITY_SPAWN_STEP_1_TIP,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_BAN_ENTITY_SPAWN_STEP_1_TITLE,
                                    Msg.PROGRESS_TYPE_SET_FUNCTION_BAN_ENTITY_SPAWN_STEP_1_SUBTITLE,
                                    null,
                                    List.class,
                                    true,
                                    LIST_CONDITION_NULLABLE)));
                }};
        Map<ArenaType.PresetType, Object> presets =
                new HashMap<ArenaType.PresetType, Object>() {{
                    put(ArenaType.PresetType.START_ICON, ViaVersionItem.getMapMaterial());
                    put(ArenaType.PresetType.PROTECTION_BREAK, null);
                    put(ArenaType.PresetType.PROTECTION_PLACE, null);
                    put(ArenaType.PresetType.PROTECTION_INTERACT, null);
                    put(ArenaType.PresetType.PROTECTION_POUR_LIQUID, null);
                    put(ArenaType.PresetType.PROTECTION_GET_LIQUID, null);
                    put(ArenaType.PresetType.PROTECTION_ENTITY_BREAK_DOOR, null);
                    // put(ArenaType.PresetType.PROTECTION_ENTITY_SPAWN, null);改成自定義
                    put(ArenaType.PresetType.PROTECTION_BLOCK_IGNITED, null);
                    put(ArenaType.PresetType.PROTECTION_BLOCK_BURNING, null);
                }};
        arenaTypeList.add(
                new ArenaType(
                        // 本外掛例項
                        DuelTimePlugin.getInstance(),
                        // 經典競技場ID
                        ArenaType.InternalType.CLASSIC.getId(),
                        // 經典競技場展示名
                        Msg.ARENA_TYPE_CLASSIC_NAME,
                        // 建立經典競技場的過程ID
                        ProgressType.InternalType.CREATE_CLASSIC_ARENA.getId(),
                        // 建立經典競技場的過程名稱
                        Msg.PROGRESS_TYPE_CREATE_CLASSIC_ARENA_NAME,
                        // 建立經典競技場過程的模板步驟
                        steps,
                        // 經典競技場所定義的附加功能
                        functionDef,
                        // 經典競技場所採用的預設
                        presets
                ));
    }

    public ArenaType get(String id) {
        if (!id.contains(":")) id = "dueltime4:" + id;
        for (ArenaType type : arenaTypeList) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    public List<ArenaType> getList() {
        return arenaTypeList;
    }
}
