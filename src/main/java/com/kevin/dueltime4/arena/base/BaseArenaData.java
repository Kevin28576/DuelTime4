package com.kevin.dueltime4.arena.base;

import com.kevin.dueltime4.arena.type.ArenaType;
import org.bukkit.Location;

import java.util.HashMap;

/**
 * 競技場場地的基礎資料
 */
public class BaseArenaData {
    // ID，競技場的唯一標識
    private final String id;
    // 競技場名稱
    private String name;
    // 型別id
    private final String typeId;
    // 對角點位置1，用於確立三維空間
    private final Location diagonalPointLocation1;
    // 對角點位置2，用於確立三維空間
    private final Location diagonalPointLocation2;
    // 最小開始人數
    private final int minPlayerNumber;
    // 最大人數，為非正數代表無限制
    private final int maxPlayerNumber;
    // 拓展功能Map，功能名和相關資料一一對應
    private HashMap<String, Object[]> functions;

    public BaseArenaData(String id, String name, String typeId, Location diagonalPointLocation1, Location diagonalPointLocation2, int minPlayerNumber, int maxPlayerNumber, HashMap<String, Object[]> functions) {
        this.id = id;
        this.name = name;
        this.typeId = typeId;
        this.diagonalPointLocation1 = diagonalPointLocation1;
        this.diagonalPointLocation2 = diagonalPointLocation2;
        this.minPlayerNumber = minPlayerNumber;
        this.maxPlayerNumber = maxPlayerNumber;
        this.functions = functions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTypeId() {
        return typeId;
    }

    public Location getDiagonalPointLocation1() {
        return diagonalPointLocation1;
    }

    public Location getDiagonalPointLocation2() {
        return diagonalPointLocation2;
    }

    public int getMinPlayerNumber() {
        return minPlayerNumber;
    }

    public int getMaxPlayerNumber() {
        return maxPlayerNumber;
    }

    public HashMap<String, Object[]> getFunctions() {
        return functions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addFunction(String functionId, Object[] data) {
        if (functions == null) functions = new HashMap<>();
        functions.put(functionId, data);
    }

    public void removeFunction(String functionId) {
        if (this.functions != null) {
            functions.remove(functionId);
            if (functions.isEmpty()) {
                // 如果沒有元素了，設為null，避免在資料庫中以空列表形式佔用記憶體
                functions = null;
            }
        }
    }

    public void setFunctions(HashMap<String, Object[]> functions) {
        this.functions = functions;
    }

    public boolean hasFunction(String functionId) {
        if (this.functions == null) {
            return false;
        }
        return this.functions.containsKey(functionId);
    }

    public boolean hasFunction(ArenaType.FunctionInternalType function) {
        return hasFunction(function.getId());
    }

    public Object[] getFunctionData(ArenaType.FunctionInternalType function) {
        return getFunctionData(function.getId());
    }

    public Object[] getFunctionData(String functionId) {
        return functions.get(functionId);
    }
}
