package com.kevin.dueltime4.arena.gamer;

import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ClassicGamerData extends BaseGamerData {
    private Location recentLocation;// 最後一次在場地內的位置，防止因玩家在臨界情況時因移動過快未能被移動事件記錄，使得無法拉回場地內
    private final Location originalLocation;
    private int hitTime;
    private double totalDamage;
    private double maxDamage;
    private ClassicArenaRecordData.Result result;

    public ClassicGamerData(Player player, Location originalLocation) {
        super(player);
        this.originalLocation = originalLocation;
    }

    public Location getRecentLocation() {
        return recentLocation;
    }

    public void updateRecentLocation(Location recentLocation) {
        this.recentLocation = recentLocation;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public void addDamage(double damage) {
        this.totalDamage += damage;
    }

    public void checkAndSetMaxDamage(double damage) {
        if (this.maxDamage == 0 || damage > this.maxDamage) {
            this.maxDamage = damage;
        }
    }

    public void addHitTime() {
        this.hitTime++;
    }

    public void confirmResult(ClassicArenaRecordData.Result result) {
        this.result = result;
    }

    public ClassicArenaRecordData.Result getResult() {
        return result;
    }

    public int getHitTime() {
        return hitTime;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public double getMaxDamage() {
        return maxDamage;
    }
}
