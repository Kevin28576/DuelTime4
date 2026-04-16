package com.kevin.dueltime4.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseArenaData;
import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.data.mapper.ClassicArenaDataMapper;
import com.kevin.dueltime4.data.pojo.ClassicArenaData;
import com.kevin.dueltime4.event.arena.*;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 活躍態競技場管理器，負責Arena競技場例項的快取工作
 * 但出於一些考慮，這個類沒有併入cache快取包中
 * 首先，BaseArena是一個活躍態的競技場物件，並非直接存入資料庫的物件（指BaseArenaData)
 * 其次，競技場資料沒有跨服共享的意義，與其他模組的資料共同點少
 */
public class ArenaManager {
    private final Map<String, BaseArena> arenaMap = new HashMap<>();
    private final Map<String, String> gamerArenaMap = new HashMap<>();
    private final Map<String, String> spectatorArenaMap = new HashMap<>();
    private final Map<String, String> waitingPlayerToArenaMap = new HashMap<>();
    private final Map<String, List<String>> waitingArenaToPlayersMap = new HashMap<>();

    public ArenaManager() {
        reload();
    }

    /**
     * 根據各個型別的場地資料(ArenaData)載入所有競技場
     */
    public void reload() {
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            // 載入經典型別競技場
            ClassicArenaDataMapper classicArenaDataMapper = sqlSession.getMapper(ClassicArenaDataMapper.class);
            classicArenaDataMapper.createTableIfNotExists();
            for (ClassicArenaData arenaData : classicArenaDataMapper.getAll()) {
                ClassicArena classicArena;
                try {
                    classicArena = new ClassicArena(arenaData);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    continue;
                }
                arenaMap.put(arenaData.getId(), classicArena);
            }
            // 載入其他型別競技場...
        }
    }

    public BaseArena get(String id) {
        return arenaMap.get(id);
    }


    public BaseArena getOf(Player player) {
        return arenaMap.get(gamerArenaMap.get(player.getName()));
    }

    public BaseArena getSpectate(Player player) {
        return arenaMap.get(spectatorArenaMap.get(player.getName()));
    }

    // 已棄用，現改用快取來處理玩家-競技場的對應關系
    @Deprecated
    public BaseArena getOfWithoutCache(Player player) {
        String playerName = player.getName();
        for (BaseArena arena : arenaMap.values()) {
            List<BaseGamerData> gamerDataList = arena.getGamerDataList();
            if (gamerDataList == null || gamerDataList.isEmpty()) {
                continue;
            }
            for (BaseGamerData gamerData : arena.getGamerDataList()) {
                if (gamerData.getPlayer().getName().equals(playerName)) {
                    return arena;
                }
            }
        }
        return null;
    }

    // 經由本管理器來呼叫比賽開始的方法，這麼做會事先發布比賽嘗試開始的事件，同時順帶清理等待者列表、載入玩家-競技場對映（這些工作可能會被不走這個方法的開發者疏漏），最後發布比賽開始事件（若未被取消）
    public void start(String id, Object data, Player... players) {
        BaseArena arena = get(id);
        ArenaTryToStartEvent event = new ArenaTryToStartEvent(arena, players);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            for (Player player : players) {
                if (waitingPlayerToArenaMap.containsKey(player.getName())) {
                    removeWaitingPlayer(player);
                }
            }
            return;
        }
        waitingArenaToPlayersMap.remove(id);
        for (Player player : players) {
            addGamerToMap(player, id);
            player.closeInventory();
            waitingPlayerToArenaMap.remove(player.getName());
        }
        arena.start(data, players);
        Bukkit.getServer().getPluginManager().callEvent(new ArenaStartEvent(arena));
        updateStartInventory();
    }

    // 經由本管理器來呼叫比賽結束的方法，這麼做會事先發布比賽結束的事件，同時順帶清除相關的玩家-競技場對映（這些工作可能會被不走這個方法的開發者疏漏）
    public void end(String id) {
        BaseArena arena = get(id);
        ArenaTryToEndEvent event = new ArenaTryToEndEvent(arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            for (BaseGamerData gamerData : arena.getGamerDataList()) {
                removeGamerFromMap(gamerData.getPlayerName());
            }
            arena.end();
            updateStartInventory();
            Bukkit.getServer().getPluginManager().callEvent(new ArenaEndEvent(arena));
        }
    }

    public void stop(String id, String reason) {
        BaseArena arena = get(id);
        Bukkit.getServer().getPluginManager().callEvent(new ArenaTryToStopEvent(arena, reason));
        for (BaseGamerData gamerData : arena.getGamerDataList()) {
            removeGamerFromMap(gamerData.getPlayerName());
        }
        updateStartInventory();
        Bukkit.getServer().getPluginManager().callEvent(new ArenaStopEvent(arena, reason));
    }

    // 經由本管理器來整合並呼叫玩家中途加入的方法，這麼做會事先發布玩家加入的的事件，同時順帶載入玩家-競技場對映（這些工作可能會被不走這個方法的開發者疏漏）
    public void join(Player player, String id, ArenaTryToJoinEvent.Way way) {
        BaseArena arena = get(id);
        ArenaTryToJoinEvent event = new ArenaTryToJoinEvent(player, arena, way);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            addGamerToMap(player, id);
        }
        updateStartInventory();
    }

    // 過後新增一個自定義報錯型別...............................................
    public void addGamerToMap(Player player, String id) {
        gamerArenaMap.put(player.getName(), id);
    }

    // 過後新增一個自定義報錯型別...............................................
    public void removeGamerFromMap(String playerName) {
        gamerArenaMap.remove(playerName);
    }

    // 經由本管理器來整合並呼叫玩家觀戰的方法，這麼做會事先發布玩家觀戰的的事件，同時順帶載入玩家-競技場對映（這些工作可能會被不走這個方法的開發者疏漏）
    public void spectate(Player player, String id) {
        BaseArena arena = get(id);
        ArenaTryToSpectateEvent event = new ArenaTryToSpectateEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            spectatorArenaMap.put(player.getName(), id);
        }
    }

    // 過後新增一個自定義報錯型別...............................................
    public void removeSpectator(Player player) {
        BaseArena arena = getSpectate(player);
        ArenaTryToQuitSpectateEvent event = new ArenaTryToQuitSpectateEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            arena.removeSpectatorData(player);
            spectatorArenaMap.remove(player.getName());
        }
    }

    public void addWaitingPlayer(Player player, String id) {
        String playerName = player.getName();
        boolean isSwitch = !waitingPlayerToArenaMap.getOrDefault(playerName, id).equals(id);
        BaseArena arena = get(id);
        ArenaTryToWaitEvent event = new ArenaTryToWaitEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        if (isSwitch) {
            String oldId = waitingPlayerToArenaMap.get(playerName);
            List<String> watingPlayerList = waitingArenaToPlayersMap.getOrDefault(oldId, new ArrayList<>());
            watingPlayerList.remove(player.getName());
            waitingArenaToPlayersMap.put(oldId, watingPlayerList);
        }
        waitingPlayerToArenaMap.put(playerName, id);
        List<String> watingPlayerList = waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>());
        watingPlayerList.add(playerName);
        waitingArenaToPlayersMap.put(id, watingPlayerList);
        if (watingPlayerList.size() >= arena.getArenaData().getMinPlayerNumber()) {
            player.closeInventory();
            start(arena.getId(), null, watingPlayerList.stream().map(Bukkit::getPlayerExact).filter(Objects::nonNull).toArray(Player[]::new));
            return;
        }
        Bukkit.getServer().getPluginManager().callEvent(new ArenaWaitEvent(player, arena));
        MsgBuilder.send(isSwitch ? Msg.ARENA_WAIT_SWITCH : Msg.ARENA_WAIT_START, player, arena.getName());
        updateStartInventory();
    }

    public void removeWaitingPlayer(Player player) {
        waitingArenaToPlayersMap.get(waitingPlayerToArenaMap.get(player.getName())).remove(player.getName());
        waitingPlayerToArenaMap.remove(player.getName());
        updateStartInventory();
    }

    public BaseArena getWaitingFor(Player player) {
        return arenaMap.get(waitingPlayerToArenaMap.get(player.getName()));
    }

    public List<String> getWaitingPlayers(String id) {
        return waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>());
    }

    public Map<String, BaseArena> getMap() {
        return arenaMap;
    }

    public List<BaseArena> getList() {
        return new ArrayList<>(arenaMap.values());
    }

    public int size() {
        return arenaMap.size();
    }

    public void add(BaseArena arena) {
        BaseArenaData arenaData = arena.getArenaData();
        arenaMap.put(arenaData.getId(), arena);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            if (arenaData.getTypeId().equals(ArenaType.InternalType.CLASSIC.getId())) {
                sqlSession.getMapper(ClassicArenaDataMapper.class).add((ClassicArenaData) arenaData);
            }
        }
    }

    public void update(BaseArenaData arenaData) {
        BaseArena arena = arenaMap.get(arenaData.getId());
        arena.setArenaData(arenaData);
        arenaMap.put(arenaData.getId(), arena);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            if (arenaData.getTypeId().equals(ArenaType.InternalType.CLASSIC.getId())) {
                sqlSession.getMapper(ClassicArenaDataMapper.class).update((ClassicArenaData) arenaData);
            }
        }
    }

    public void delete(String id) {
        arenaMap.remove(id);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(ClassicArenaDataMapper.class).delete(id);
        }
    }

    private void updateStartInventory() {
        CustomInventoryManager customInventoryManager = DuelTimePlugin.getInstance().getCustomInventoryManager();
        customInventoryManager.updatePage(customInventoryManager.getStart());
    }
}
