package com.kevin.dueltime4.util;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerTeleportEvent;

public class UtilSync {
    public static void publishEvent(Event event) {
        Bukkit.getScheduler().runTask(DuelTimePlugin.getInstance(), () -> Bukkit.getServer().getPluginManager().callEvent(event));
    }

    public static void tp(Player player, Location location) {
        Bukkit.getScheduler().runTask(DuelTimePlugin.getInstance(), () -> player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN));
    }
}
