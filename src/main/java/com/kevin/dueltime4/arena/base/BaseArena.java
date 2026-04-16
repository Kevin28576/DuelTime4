package com.kevin.dueltime4.arena.base;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 活躍態競技場抽象類，負責根據場地資料(ArenaData)和記錄資料(RecordData)來組織玩家比賽
 */
public abstract class BaseArena {
    private BaseArenaData arenaData;
    private List<BaseGamerData> gamerDataList = new ArrayList<>();
    private List<BaseSpectatorData> spectatorDataList = new ArrayList<>();
    private final String arenaTypeId;
    private State state;

    public BaseArena(BaseArenaData arenaData) throws NullPointerException {
        this.arenaData = arenaData;
        if (DuelTimePlugin.getInstance().getArenaTypeManager().get(arenaData.getTypeId()) == null) {
            throw new NullPointerException(MsgBuilder.get(Msg.EXCEPTION_ARENA_INITIALIZE_FAIL_INVALID_ARENA_TYPE, Bukkit.getConsoleSender(),
                    arenaData.getId(),
                    arenaData.getName(),
                    arenaData.getTypeId()));
        } else {
            this.arenaTypeId = arenaData.getTypeId();
        }
    }

    public BaseArenaData getArenaData() {
        return arenaData;
    }

    public List<BaseGamerData> getGamerDataList() {
        return gamerDataList;
    }

    public List<BaseSpectatorData> getSpectatorDataList() {
        return spectatorDataList;
    }

    public String getArenaTypeId() {
        return arenaTypeId;
    }

    public ArenaType getArenaType() {
        return DuelTimePlugin.getInstance().getArenaTypeManager().get(arenaTypeId);
    }

    public String getId() {
        return arenaData.getId();
    }

    public String getName() {
        return arenaData.getName();
    }

    public boolean isFull() {
        return arenaData.getMaxPlayerNumber() == gamerDataList.size();
    }

    public void setArenaData(BaseArenaData arenaData) {
        this.arenaData = arenaData;
    }

    public void addGamerData(BaseGamerData gamerData) {
        gamerDataList.add(gamerData);
    }

    public void removeGamerData(Player player) {
        gamerDataList.removeIf(gamerData -> gamerData.getPlayer().getName().equals(player.getName()));
    }

    public void setGamerDataList(List<BaseGamerData> gamerDataList) {
        this.gamerDataList = gamerDataList == null ? new ArrayList<>() : gamerDataList;
    }

    public boolean hasPlayer(Player player) {
        String playerName = player.getName();
        for (BaseGamerData gamerData : gamerDataList) {
            if (gamerData.getPlayer().getName().equals(playerName)) {
                return true;
            }
        }
        return false;
    }

    public BaseGamerData getGamerData(String playerName) {
        for (BaseGamerData gamerData : gamerDataList) {
            if (gamerData.getPlayerName().equals(playerName)) {
                return gamerData;
            }
        }
        return null;
    }


    public void addSpectatorData(BaseSpectatorData spectatorData) {
        spectatorDataList.add(spectatorData);
    }

    public void removeSpectatorData(Player player) {
        spectatorDataList.removeIf(spectatorData -> spectatorData.getPlayer().getName().equals(player.getName()));
    }

    public void setSpectatorDataList(List<BaseSpectatorData> spectatorDataList) {
        this.spectatorDataList = spectatorDataList;
    }

    public boolean hasSpectator(Player player) {
        String playerName = player.getName();
        for (BaseSpectatorData spectatorData : spectatorDataList) {
            if (spectatorData.getPlayer().getName().equals(playerName)) {
                return true;
            }
        }
        return false;
    }

    public BaseSpectatorData getSpector(String playerName) {
        for (BaseSpectatorData spectatorData : spectatorDataList) {
            if (spectatorData.getPlayer().equals(playerName)) {
                return spectatorData;
            }
        }
        return null;
    }

    /**
     * 開始比賽時呼叫
     */
    public abstract void start(Object data,Player... gamers);

    /**
     * 結束比賽時呼叫
     */
    public abstract void end();

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public enum State {
        WAITING, IN_PROGRESS_CLOSED, IN_PROGRESS_OPENED, DISABLED
    }
}
