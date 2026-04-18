package com.kevin.dueltime4.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseArenaData;
import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.data.mapper.ClassicArenaDataMapper;
import com.kevin.dueltime4.data.pojo.ClassicArenaData;
import com.kevin.dueltime4.event.arena.ArenaEndEvent;
import com.kevin.dueltime4.event.arena.ArenaStartEvent;
import com.kevin.dueltime4.event.arena.ArenaStopEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToEndEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToJoinEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToQuitSpectateEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToSpectateEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToStartEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToStopEvent;
import com.kevin.dueltime4.event.arena.ArenaTryToWaitEvent;
import com.kevin.dueltime4.event.arena.ArenaWaitEvent;
import com.kevin.dueltime4.gui.CustomInventoryManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ArenaManager {
    private final Map<String, BaseArena> arenaMap = new HashMap<>();
    private final Map<String, String> gamerArenaMap = new HashMap<>();
    private final Map<String, String> spectatorArenaMap = new HashMap<>();
    private final Map<String, String> waitingPlayerToArenaMap = new HashMap<>();
    private final Map<String, List<String>> waitingArenaToPlayersMap = new HashMap<>();
    private final Map<String, Long> waitingPenaltyUntilMap = new HashMap<>();
    private final QueueMatchConfirmManager queueMatchConfirmManager;

    public ArenaManager() {
        reload();
        queueMatchConfirmManager = new QueueMatchConfirmManager(this);
    }

    public void reload() {
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
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

    public void start(String id, Object data, Player... players) {
        List<Player> startingPlayers = new ArrayList<>();
        for (Player player : players) {
            if (player != null) {
                startingPlayers.add(player);
            }
        }
        if (startingPlayers.isEmpty()) {
            return;
        }

        BaseArena arena = get(id);
        ArenaTryToStartEvent event = new ArenaTryToStartEvent(arena, startingPlayers.toArray(Player[]::new));
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            for (Player player : startingPlayers) {
                if (waitingPlayerToArenaMap.containsKey(player.getName())) {
                    removeWaitingPlayer(player.getName());
                }
            }
            return;
        }

        queueMatchConfirmManager.cancelForArenaSilently(id);

        List<String> waitingPlayers = waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>());
        for (Player player : startingPlayers) {
            addGamerToMap(player, id);
            player.closeInventory();
            waitingPlayerToArenaMap.remove(player.getName());
            waitingPlayers.remove(player.getName());
            if (player.isOnline()) {
                MsgBuilder.sendActionBar(" ", player, true);
            }
        }
        if (waitingPlayers.isEmpty()) {
            waitingArenaToPlayersMap.remove(id);
        } else {
            waitingArenaToPlayersMap.put(id, waitingPlayers);
        }

        arena.start(data, startingPlayers.toArray(Player[]::new));
        Bukkit.getServer().getPluginManager().callEvent(new ArenaStartEvent(arena));
        updateStartInventory();
    }

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
        queueMatchConfirmManager.cancelForArenaSilently(id);
        for (BaseGamerData gamerData : arena.getGamerDataList()) {
            removeGamerFromMap(gamerData.getPlayerName());
        }
        updateStartInventory();
        Bukkit.getServer().getPluginManager().callEvent(new ArenaStopEvent(arena, reason));
    }

    public void join(Player player, String id, ArenaTryToJoinEvent.Way way) {
        BaseArena arena = get(id);
        ArenaTryToJoinEvent event = new ArenaTryToJoinEvent(player, arena, way);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            addGamerToMap(player, id);
        }
        updateStartInventory();
    }

    public void addGamerToMap(Player player, String id) {
        gamerArenaMap.put(player.getName(), id);
    }

    public void removeGamerFromMap(String playerName) {
        gamerArenaMap.remove(playerName);
    }

    public void spectate(Player player, String id) {
        BaseArena arena = get(id);
        ArenaTryToSpectateEvent event = new ArenaTryToSpectateEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            spectatorArenaMap.put(player.getName(), id);
        }
    }

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
        long penaltyRemainingSeconds = getQueuePenaltyRemainingSeconds(playerName);
        if (penaltyRemainingSeconds > 0) {
            DynamicLang.send(player, true,
                    "Dynamic.queue.penalty-blocked",
                    "&cYou cannot join queue yet. Please wait &e{seconds} &cseconds.",
                    "seconds", String.valueOf(penaltyRemainingSeconds));
            return;
        }

        boolean isSwitch = !waitingPlayerToArenaMap.getOrDefault(playerName, id).equals(id);
        BaseArena arena = get(id);
        ArenaTryToWaitEvent event = new ArenaTryToWaitEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        if (isSwitch) {
            String oldId = waitingPlayerToArenaMap.get(playerName);
            List<String> oldWaitingPlayerList = waitingArenaToPlayersMap.getOrDefault(oldId, new ArrayList<>());
            oldWaitingPlayerList.remove(playerName);
            if (oldWaitingPlayerList.isEmpty()) {
                waitingArenaToPlayersMap.remove(oldId);
            } else {
                waitingArenaToPlayersMap.put(oldId, oldWaitingPlayerList);
            }
        }

        waitingPlayerToArenaMap.put(playerName, id);
        List<String> waitingPlayerList = waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>());
        if (!waitingPlayerList.contains(playerName)) {
            waitingPlayerList.add(playerName);
        }
        waitingArenaToPlayersMap.put(id, waitingPlayerList);

        Bukkit.getServer().getPluginManager().callEvent(new ArenaWaitEvent(player, arena));
        MsgBuilder.send(isSwitch ? Msg.ARENA_WAIT_SWITCH : Msg.ARENA_WAIT_START, player, arena.getName());
        tryCreatePendingMatch(id);
        updateStartInventory();
    }

    public void removeWaitingPlayer(Player player) {
        if (player == null) {
            return;
        }
        removeWaitingPlayer(player.getName());
    }

    public void removeWaitingPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }

        String waitingArenaId = waitingPlayerToArenaMap.remove(playerName);
        if (waitingArenaId != null) {
            List<String> waitingPlayerList = waitingArenaToPlayersMap.get(waitingArenaId);
            if (waitingPlayerList != null) {
                waitingPlayerList.remove(playerName);
                if (waitingPlayerList.isEmpty()) {
                    waitingArenaToPlayersMap.remove(waitingArenaId);
                } else {
                    waitingArenaToPlayersMap.put(waitingArenaId, waitingPlayerList);
                }
            }
        }

        queueMatchConfirmManager.onWaitingPlayerRemoved(playerName);

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null && player.isOnline()) {
            MsgBuilder.sendActionBar(" ", player, true);
        }

        if (waitingArenaId != null) {
            tryCreatePendingMatch(waitingArenaId);
        }
        updateStartInventory();
    }

    public BaseArena getWaitingFor(Player player) {
        return arenaMap.get(waitingPlayerToArenaMap.get(player.getName()));
    }

    public boolean isWaitingPlayerForArena(String playerName, String arenaId) {
        return Objects.equals(waitingPlayerToArenaMap.get(playerName), arenaId);
    }

    public List<String> getWaitingPlayers(String id) {
        return new ArrayList<>(waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>()));
    }

    public int getTotalWaitingCount() {
        return waitingPlayerToArenaMap.size();
    }

    public QueueMatchConfirmManager getQueueMatchConfirmManager() {
        return queueMatchConfirmManager;
    }

    public long getQueuePenaltyRemainingSeconds(String playerName) {
        Long penaltyEnd = waitingPenaltyUntilMap.get(playerName);
        if (penaltyEnd == null) {
            return 0;
        }
        long remaining = (penaltyEnd - System.currentTimeMillis() + 999L) / 1000L;
        if (remaining <= 0) {
            waitingPenaltyUntilMap.remove(playerName);
            return 0;
        }
        return remaining;
    }

    public boolean isQueuePenaltyActive(String playerName) {
        return getQueuePenaltyRemainingSeconds(playerName) > 0;
    }

    public void applyQueuePenalty(String playerName, int seconds) {
        if (seconds <= 0) {
            return;
        }
        waitingPenaltyUntilMap.put(playerName, System.currentTimeMillis() + seconds * 1000L);
    }

    public void tryCreatePendingMatch(String arenaId) {
        BaseArena arena = get(arenaId);
        if (arena == null || arena.getState() != BaseArena.State.WAITING) {
            return;
        }
        queueMatchConfirmManager.tryCreatePendingMatch(arena);
    }

    public boolean acceptPendingMatch(Player player) {
        return queueMatchConfirmManager.accept(player);
    }

    public boolean declinePendingMatch(Player player) {
        return queueMatchConfirmManager.decline(player);
    }

    public void cancelAllPendingMatches() {
        queueMatchConfirmManager.cancelAll();
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
