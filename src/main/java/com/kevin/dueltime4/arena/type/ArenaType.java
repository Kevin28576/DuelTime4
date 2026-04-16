package com.kevin.dueltime4.arena.type;

import com.kevin.dueltime4.progress.Step;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 競技場型別
 * 用於宣告某個型別的競技場的各種屬性
 * 如id、型別名、附加功能定義等
 */
public class ArenaType {
    // 競技場型別的唯一標識
    private final String id;
    // 競技場型別名稱
    private final Object name;
    private final String createProgressId;
    private final Object createProgressName;
    private final Step[] createTemplateSteps;
    // 附加功能(Function)
    private final Map<String, Function> functionDef;
    // 預設
    private final Map<PresetType, Object> presets;


    public ArenaType(Plugin plugin, String id, Object name, String createProgressId, Object createProgressName, Step[] createTemplateSteps, Map<String, Function> functionDef, Map<PresetType, Object> presets) {
        if (plugin == null) {
            throw new NullPointerException("The plugin cannot be null");
        }
        if (!id.contains(":") || !id.split(":")[0].equals(plugin.getDescription().getName().toLowerCase()) || !UtilFormat.isIDStyle(id.split(":")[1])) {
            throw new IllegalArgumentException("The format of the 2nd argument must be 'the lowercase of your plugin' + ':' + 'id',for example,'dueltime4:test',and the id can only consist of English and number");
        }
        this.id = id;
        if (!(name instanceof String) && !(name instanceof Msg)) {
            throw new IllegalArgumentException("The 3rd argument must be String or Msg");
        }
        this.createProgressId = createProgressId;
        if (!(createProgressName instanceof String) && !(createProgressName instanceof Msg)) {
            throw new IllegalArgumentException("The 5th argument must be String or Msg");
        }
        this.name = name;
        this.createProgressName = createProgressName;
        this.createTemplateSteps = createTemplateSteps;
        this.functionDef = functionDef;
        this.presets = presets == null ? new HashMap<>() : presets;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return (String) name;
    }

    public String getName(CommandSender sender) {
        return (name instanceof Msg) ?
                MsgBuilder.get((Msg) name, sender) :
                (String) name;
    }

    public String getCreateProgressId() {
        return createProgressId;
    }

    public String getCreateProgressName() {
        return (String) createProgressName;
    }

    public String getCreateProgressName(CommandSender sender) {
        return (createProgressName instanceof Msg) ?
                MsgBuilder.get((Msg) createProgressName, sender) :
                (String) createProgressName;
    }

    public Step[] getCreateTemplateSteps() {
        return createTemplateSteps;
    }

    public Map<String, Function> getFunctionDef() {
        return functionDef;
    }

    public Map<PresetType, Object> getPresets() {
        return presets;
    }

    public enum InternalType {
        CLASSIC("dueltime4:classic");
        private final String id;

        InternalType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * 附加功能定義類
     */
    public static class Function {
        // 附加功能的唯一標識
        private final String id;
        // 附加功能的名稱
        private final Object name;
        // 附加功能的描述
        private final Object description;
        // 附加功能資料上傳的過程ID
        private final String progressId;
        // 附加功能資料上傳需要的步驟模板
        private final Step[] templateSteps;

        public Function(String id, Object name, Object description, Plugin plugin, String progressId, Step... templateSteps) {
            if (!(name instanceof String) && !(name instanceof Msg)) {
                throw new IllegalArgumentException("The 2nd argument must be String or Msg");
            }
            if (!(description instanceof String) && !(description instanceof Msg)) {
                throw new IllegalArgumentException("The 3rd argument must be String or Msg");
            }
            this.id = id;
            this.name = name;
            this.description = description;
            if (progressId != null && (!progressId.contains(":") || !progressId.split(":")[0].equals(plugin.getDescription().getName().toLowerCase()) || !UtilFormat.isIDStyle(progressId.split(":")[1]))) {
                throw new IllegalArgumentException("The format of the 5th argument must be 'the lowercase of your plugin' + ':' + 'id',for example,'dueltime4:test',and the id can only consist of English and numbers");
            }
            this.progressId = progressId;
            this.templateSteps = templateSteps;
        }

        public Function(String id, Object name, Object description) {
            this(id, name, description, null, null);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return (String) name;
        }

        public String getName(CommandSender sender) {
            return MsgBuilder.get((Msg) name, sender);
        }

        public String getDescription() {
            return (String) description;
        }

        public String getDescription(CommandSender sender) {
            return MsgBuilder.get((Msg) description, sender);
        }

        public String getProgressId() {
            return progressId;
        }

        public Step[] getTemplateSteps() {
            return templateSteps;
        }
    }

    public enum PresetType {
        START_ICON(Material.class),// 開始介面中的圖示種類，資料型別為Material
        PROTECTION_BREAK(List.class),// 進行比賽時，阻止玩家破壞方塊，資料型別為List<Material>，為可破壞種類的白名單
        PROTECTION_PLACE(List.class),// 進行比賽時，阻止玩家放置方塊，資料型別為List<Material>，為可放置種類的白名單
        PROTECTION_INTERACT(List.class),// 進行比賽時，阻止玩家互動，資料型別為List<Material>，為可互動種類的白名單
        PROTECTION_POUR_LIQUID(List.class),// 進行比賽時，阻止玩家傾倒液體，資料型別為List<Material>，為可傾倒種類的白名單
        PROTECTION_GET_LIQUID(List.class),// 進行比賽時，阻止玩家撈取液體，資料型別為List<Material>，為可撈取種類的白名單
        PROTECTION_ENTITY_BREAK_DOOR(null),// 進行比賽時，阻止實體破壞門。無資料
        PROTECTION_ENTITY_SPAWN(List.class),// 進行比賽時，阻止實體生成，資料型別為List<EntityType>，為可生成種類的白名單
        PROTECTION_BLOCK_IGNITED(List.class),// 進行比賽時，阻止方塊被點燃，資料型別為List<Material>，為可點燃種類的白名單
        PROTECTION_BLOCK_BURNING(List.class);// 進行比賽時，阻止方塊持續燃燒，資料型別為List<Material>，為可持續燃燒種類的白名單

        private final Class<?> dataType;

        PresetType(Class<?> dataType) {
            this.dataType = dataType;
        }

        public Class<?> getDataType() {
            return dataType;
        }
    }

    public enum FunctionInternalType {
        CLASSIC_TIME_LIMIT("dueltime4:time_limit"),
        CLASSIC_COUNTDOWN("dueltime4:countdown"),
        CLASSIC_INVENTORY_CHECK_KEYWORD("dueltime4:inventory_check_keyword"),
        CLASSIC_INVENTORY_CHECK_TYPE("dueltime4:inventory_check_type"),
        CLASSIC_PRE_GAME_COMMAND("dueltime4:pre_game_command"),
        CLASSIC_SPECTATE("dueltime4:spectate"),
        CLASSIC_BAN_ENTITY_SPAWN("dueltime4:ban_entity_spawn");

        private final String id;

        FunctionInternalType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
