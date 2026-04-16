package com.kevin.dueltime4.data.pojo;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopRewardData {
    private int id;
    private final ItemStack itemStack;
    private double point;
    private int levelLimit;
    private String description;
    private List<String> commands;
    private int totalRedemptionVolume;
    @Override
    public String toString() {
        return "ShopRewardData{" +
                "id=" + id +
                ", itemStack=" + itemStack +
                ", point=" + point +
                ", levelLimit=" + levelLimit +
                ", description='" + description + '\'' +
                ", commands=" + commands +
                ", totalRedemptionVolume=" + totalRedemptionVolume +
                '}';
    }
    public ShopRewardData(int id,ItemStack itemStack, double point, int levelLimit, String description, List<String> commands, int totalRedemptionVolume) {
        this.id = id;
        this.itemStack = itemStack;
        this.point = point;
        this.levelLimit = levelLimit;
        this.description = description;
        this.commands = commands;
        this.totalRedemptionVolume = totalRedemptionVolume;
    }

    public int getId() {
        return id;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public double getPoint() {
        return point;
    }

    public int getTotalRedemptionVolume() {
        return totalRedemptionVolume;
    }

    public int getLevelLimit() {
        return levelLimit;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCommands() {
        return commands;
    }

    // 用於主鍵返回時呼叫
    public void setId(int id) {
        this.id = id;
    }

    public void setPoint(double point) {
        this.point = point;
    }

    public void setLevelLimit(int levelLimit) {
        this.levelLimit = levelLimit;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public void setTotalRedemptionVolume(int totalRedemptionVolume) {
        this.totalRedemptionVolume = totalRedemptionVolume;
    }

    public void updateTotalRedemptionVolume() {
        this.totalRedemptionVolume++;
    }
}
