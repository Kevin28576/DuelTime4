package com.kevin.dueltime4.arena.spectator;

import com.kevin.dueltime4.arena.base.BaseSpectatorData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ClassicSpectatorData extends BaseSpectatorData {
    private Location recentLocation;
    private final Location originalLocation;
    private final GameMode originalGameMode;

    public ClassicSpectatorData(Player player, Location originalLocation, GameMode originalGameMode) {
        super(player);
        this.originalLocation = originalLocation;
        this.originalGameMode = originalGameMode;
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

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }
}
