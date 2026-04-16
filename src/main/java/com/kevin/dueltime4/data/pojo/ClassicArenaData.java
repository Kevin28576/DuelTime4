package com.kevin.dueltime4.data.pojo;

import com.kevin.dueltime4.arena.base.BaseArenaData;
import com.kevin.dueltime4.arena.type.ArenaType;
import org.bukkit.Location;

import java.util.HashMap;

public class ClassicArenaData extends BaseArenaData {
    private final Location playerLocation1;
    private final Location playerLocation2;

    public ClassicArenaData(String id, String name, Location diagonalPointLocation1, Location diagonalPointLocation2, HashMap<String, Object[]> functions,
                            Location playerLocation1, Location playerLocation2) {
        super(id, name, ArenaType.InternalType.CLASSIC.getId(), diagonalPointLocation1, diagonalPointLocation2, 2, 2, functions);
        this.playerLocation1 = playerLocation1;
        this.playerLocation2 = playerLocation2;
    }

    public Location getPlayerLocation1() {
        return playerLocation1;
    }

    public Location getPlayerLocation2() {
        return playerLocation2;
    }
}
