package com.kevin.dueltime4.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.arena.base.BaseSpectatorData;
import com.kevin.dueltime4.arena.gamer.ClassicGamerData;
import com.kevin.dueltime4.arena.spectator.ClassicSpectatorData;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.cache.RecordCache;
import com.kevin.dueltime4.cache.LocationCache;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.data.pojo.ClassicArenaData;
import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.level.Tier;
import com.kevin.dueltime4.listener.arena.BaseArenaListener;
import com.kevin.dueltime4.listener.arena.ClassicArenaListener;
import com.kevin.dueltime4.network.DiscordWebhookManager;
import com.kevin.dueltime4.stats.MatchStreakManager;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilMath;
import com.kevin.dueltime4.util.UtilSync;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kevin.dueltime4.arena.type.ArenaType.FunctionInternalType.*;

public class ClassicArena extends BaseArena {

    private int time = 0;
    private BukkitTask timer;
    private Stage stage;
    // 附加功能-倒計時：玩家-初始位置對映，用於倒計時期間禁止移動情形下快速拉回各自原點
    private final HashMap<String, Location> playerStartLocationMap = new HashMap<>();
    // 附加功能-限時：BossBar進度條
    private BossBar timeLimitBossBar;
    // 附加功能-觀戰：顯示雙方選手血量的BossBar
    private final HashMap<String, BossBar> healthBossBars = new HashMap<>();
    private Result result;
    private Player winner;


    public ClassicArena(ClassicArenaData arenaData) {
        super(arenaData);
        setState(State.WAITING);
    }

    public Player getOpponent(Player player) {
        String playerName = player.getName();
        if (!hasPlayer(player)) {
            return null;
        }
        for (BaseGamerData gamerData : getGamerDataList()) {
            if (!gamerData.getPlayer().getName().equals(playerName)) {
                return gamerData.getPlayer();
            }
        }
        return null;
    }

    // 在不確定雙方的Player物件是否存在的情況下（如因下線輸掉比賽時為null），就依靠玩家名獲取對手名
    public String getOpponent(String playerName) {
        for (BaseGamerData gamerData : getGamerDataList()) {
            if (!gamerData.getPlayerName().equals(playerName)) {
                return gamerData.getPlayerName();
            }
        }
        return null;
    }

    @Override
    public void start(Object data, Player... gamers) {
        if (gamers.length != 2) return;
        if (gamers[0].getName().equals(gamers[1].getName())) return;
        // 宣告狀態，並把選手們都傳送進來
        setState(State.IN_PROGRESS_CLOSED);
        Player gamer1 = gamers[0];
        Player gamer2 = gamers[1];
        ClassicGamerData gamerData1 = new ClassicGamerData(gamer1, gamer1.getLocation());
        ClassicGamerData gamerData2 = new ClassicGamerData(gamer2, gamer2.getLocation());
        addGamerData(gamerData1);
        addGamerData(gamerData2);
        ClassicArenaData arenaData = (ClassicArenaData) getArenaData();
        gamer1.teleport(arenaData.getPlayerLocation1());
        playerStartLocationMap.put(gamer1.getName(), arenaData.getPlayerLocation1());
        gamerData1.updateRecentLocation(arenaData.getPlayerLocation1());// 初始化最近位置，下同
        gamer2.teleport(arenaData.getPlayerLocation2());
        playerStartLocationMap.put(gamer2.getName(), arenaData.getPlayerLocation2());
        gamerData2.updateRecentLocation(arenaData.getPlayerLocation2());
        // 加滿血量，並根據是否有觀戰的附加功能來初始化雙方的血條BossBar
        for (BaseGamerData gamerData : getGamerDataList()) {
            Player player = gamerData.getPlayer();
            player.setHealth(player.getMaxHealth());
            if (getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_SPECTATE) &&
                    (boolean) (getArenaData().getFunctionData(CLASSIC_SPECTATE)[3]) &&
                    DuelTimePlugin.serverVersionInt >= 9) {
                BossBar bossBar = Bukkit.createBossBar(MsgBuilder.get(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_GAMER_HEALTH_BOSSBAR, player,
                        player.getName(), "" + UtilFormat.round(player.getHealth(), 1), "" + UtilFormat.round(player.getMaxHealth(), 1)), BarColor.BLUE, BarStyle.SOLID);
                bossBar.setProgress(1.0);
                healthBossBars.put(player.getName(), bossBar);
            }
        }
        // 初始化定時器
        stage = (getArenaData().hasFunction(CLASSIC_COUNTDOWN)) ? Stage.COUNTDOWN : Stage.GAME;
        time = -1;
        timer = Bukkit.getScheduler().runTaskTimerAsynchronously(DuelTimePlugin.getInstance(),
                () -> {
                    time++;
                    // 檢測是否為倒計時階段
                    if (getArenaData().hasFunction(CLASSIC_COUNTDOWN)) {
                        if (stage == Stage.COUNTDOWN) {
                            int nowCountdown = (int) (getArenaData().getFunctionData(CLASSIC_COUNTDOWN)[0]) - time;
                            if (nowCountdown > 0) {
                                // 提示倒計時並播放音效
                                for (BaseGamerData gamerData : getGamerDataList()) {
                                    Player gamer = gamerData.getPlayer();
                                    MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_FUNCTION_COUNTDOWN_GAME_TITLE,
                                            Msg.ARENA_TYPE_CLASSIC_FUNCTION_COUNTDOWN_GAME_SUBTITLE,
                                            0, 25, 0, gamer, ViaVersion.TitleType.LINE,
                                            "" + nowCountdown);
                                    gamer.playSound(gamer.getLocation(), ViaVersion.getSound("BLOCK_NOTE_BASS", "NOTE_BASS"), 1, 0);
                                }
                                return;
                            } else {
                                time = 0;// 計時歸零，正式開始
                                stage = Stage.GAME;
                            }
                        }
                    }
                    if (time == 0) {
                        // 比賽正式開始，提示Title，播放音效
                        for (BaseGamerData gamerData : getGamerDataList()) {
                            Player gamer = gamerData.getPlayer();
                            MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_START_TITLE, Msg.ARENA_TYPE_CLASSIC_START_SUBTITLE, 0, 25, 0, gamer, ViaVersion.TitleType.TITLE);
                            gamer.playSound(gamer.getLocation(), ViaVersion
                                    .getSound("ENTITY_PLAYER_LEVELUP", "LEVELUP"), 1.0f, 1.0f);
                        }
                        // 如果有倒計時功能，且設定了期間不禁止移動，則要把選手拉回原傳送點
                        if (getArenaData().hasFunction(CLASSIC_COUNTDOWN) && !(boolean) getArenaData().getFunctionData(CLASSIC_COUNTDOWN)[1]) {
                            UtilSync.tp(gamer1, arenaData.getPlayerLocation1());
                            UtilSync.tp(gamer2, arenaData.getPlayerLocation2());
                        }
                    }
                    // 檢測開賽後是否達到時間限制
                    if (getArenaData().hasFunction(CLASSIC_TIME_LIMIT)) {
                        // 如果還沒有BossBar則建立一個
                        if (timeLimitBossBar == null && DuelTimePlugin.serverVersionInt >= 9) {
                            timeLimitBossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
                        }
                        int timeLimit = (int) getArenaData().getFunctionData(CLASSIC_TIME_LIMIT)[0];
                        int timeLeft = timeLimit - time;
                        // 更新BossBar資訊（標題、數值）
                        if (timeLimitBossBar != null) {
                            for (BaseGamerData gamerData : getGamerDataList()) {
                                Player player = gamerData.getPlayer();
                                timeLimitBossBar.setProgress(timeLeft / (double) timeLimit);
                                timeLimitBossBar.setTitle(MsgBuilder.get(Msg.ARENA_TYPE_CLASSIC_FUNCTION_TIME_LIMIT_BOSSBAR_TITLE, player,
                                        "" + timeLeft));
                                if (!timeLimitBossBar.getPlayers().contains(player)) {
                                    timeLimitBossBar.addPlayer(player);
                                }
                            }
                        }
                        // 若剩餘時間為0，則結束比賽
                        if (timeLeft == 0) {
                            confirmResult(Result.DRAW, null);
                            for (BaseGamerData gamerData : getGamerDataList()) {
                                ((ClassicGamerData) gamerData).confirmResult(ClassicArenaRecordData.Result.DRAW);
                            }
                            Bukkit.getScheduler().runTask(DuelTimePlugin.getInstance(), () ->
                                    DuelTimePlugin.getInstance().getArenaManager().end(getId()));
                        }
                    }
                },
                0, 20);
    }

    @Override
    public void end() {
        // 關閉計時器
        timer.cancel();
        // 關閉比賽限時附加功能的BossBar
        if (getArenaData().hasFunction(CLASSIC_TIME_LIMIT) && timeLimitBossBar != null) {
            timeLimitBossBar.removeAll();
        }
        // 清空玩家-起始位置對映
        playerStartLocationMap.clear();
        // 先判斷比賽整體結果，如果不是被強制停止，則根據雙方的結果傳送提示語並記錄比賽
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        if (result != Result.STOPPED) {
            PlayerDataCache playerDataCache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
            RecordCache recordCache = DuelTimePlugin.getInstance().getCacheManager().getArenaRecordCache();
            MatchStreakManager matchStreakManager = DuelTimePlugin.getInstance().getMatchStreakManager();
            boolean streakEnabled = cfgManager.isArenaClassicStreakEnabled();
            boolean streakShowMessage = cfgManager.isArenaClassicStreakShowMessage();
            boolean streakResetOnDraw = cfgManager.isArenaClassicStreakResetOnDraw();
            for (BaseGamerData gamerData : new ArrayList<>(getGamerDataList())) {
                ClassicGamerData classicGamerData = (ClassicGamerData) gamerData;
                ClassicArenaRecordData.Result result = classicGamerData.getResult();
                Player player = classicGamerData.getPlayer();
                String playerName = classicGamerData.getPlayerName();
                String streakDisplay = "-";
                Msg resultMsg;
                double expChange;
                PlayerData playerData = playerDataCache.get(playerName);
                Location lobby = DuelTimePlugin.getInstance().getCacheManager().getLocationCache().get(LocationCache.InternalType.LOBBY.getId());
                Location back = lobby != null ? lobby : ((ClassicGamerData) gamerData).getOriginalLocation();
                if (result == ClassicArenaRecordData.Result.DRAW) {
                    resultMsg = Msg.ARENA_TYPE_CLASSIC_END_RESULT_DRAW;
                    playerData.accumulateArenaClassicDraws();
                    expChange = 0;
                    if (streakEnabled && streakResetOnDraw && matchStreakManager != null) {
                        matchStreakManager.recordDraw(playerName);
                    }
                    if (streakEnabled && matchStreakManager != null) {
                        streakDisplay = matchStreakManager.getDisplay(playerName);
                    }
                    player.teleport(back);
                } else {
                    if (result == ClassicArenaRecordData.Result.WIN) {
                        resultMsg = Msg.ARENA_TYPE_CLASSIC_END_RESULT_WIN;
                        playerData.accumulateArenaClassicWins();
                        playerData.setPoint(playerData.getPoint() + cfgManager.getArenaClassicRewardWinPoint());
                        expChange = cfgManager.getArenaClassicRewardWinExp();
                        if (streakEnabled && matchStreakManager != null) {
                            matchStreakManager.recordWin(playerName);
                            streakDisplay = matchStreakManager.getDisplay(playerName);
                        }
                        // 贏家處放煙花
                        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(),
                                EntityType.FIREWORK_ROCKET);
                        FireworkMeta fm = firework.getFireworkMeta();
                        fm.addEffect(FireworkEffect.builder()
                                .with(FireworkEffect.Type.BALL_LARGE)
                                .withFade(Color.PURPLE).withColor(Color.ORANGE)
                                .withColor(Color.YELLOW).withTrail().build());
                        fm.addEffect(FireworkEffect.builder()
                                .with(FireworkEffect.Type.BALL).withFade(Color.AQUA)
                                .withColor(Color.ORANGE).withColor(Color.YELLOW)
                                .withTrail().build());
                        fm.setPower(2);
                        firework.setFireworkMeta(fm);
                        // 提示title，並根據配置開關實現延時傳送
                        MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_WIN_TITLE, Msg.ARENA_TYPE_CLASSIC_WIN_SUBTITLE, 10, 30, 0, player);
                        if (cfgManager.isArenaClassicDelayedBackEnabled()) {
                            AtomicInteger countdown = new AtomicInteger(cfgManager.getArenaClassicDelayedBackTime());
                            String originalWorldName = player.getWorld().getName();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    countdown.getAndDecrement();
                                    if (countdown.get() == 0) {
                                        if (ViaVersion.getOnlinePlayers().contains(player) && player.getWorld().getName().equals(originalWorldName)) {
                                            player.teleport(back);
                                        }
                                        this.cancel();
                                    } else if (cfgManager.getArenaClassicDelayedBackTime() - countdown.get() >= 2) {
                                        MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_GAMER_TITLE, Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_GAMER_SUBTITLE, 0, 25, 0, player, ViaVersion.TitleType.LINE,
                                                "" + countdown);
                                    }
                                }
                            }.runTaskTimer(DuelTimePlugin.getInstance(), 20, 20);
                            BaseArenaListener.tempMovePermit.put(player.getName(), System.currentTimeMillis() + (cfgManager.getArenaClassicDelayedBackTime() + 1) * 1000L);
                        } else {
                            player.teleport(back);
                        }
                    } else {
                        resultMsg = Msg.ARENA_TYPE_CLASSIC_END_RESULT_LOSE;
                        playerData.accumulateArenaClassicLoses();
                        if (streakEnabled && matchStreakManager != null) {
                            matchStreakManager.recordLose(playerName);
                            streakDisplay = matchStreakManager.getDisplay(playerName);
                        }
                        // 等級保護：無段位情形下不扣經驗，取得段位後不會扣到無段位
                        double expDeducted = cfgManager.getArenaClassicRewardWinExp() * cfgManager.getArenaClassicRewardLoseExpRate();
                        List<Tier> tiers = DuelTimePlugin.getInstance().getLevelManager().getTiers();
                        if (tiers.size() > 1) {
                            double expNeededForFirstTier = tiers.get(0).getExpForLevelUp() * tiers.get(1).getLevel();
                            if (playerData.getExp() < expNeededForFirstTier) {
                                expChange = 0;
                            } else {
                                expChange = -1 * (playerData.getExp() - expDeducted > expNeededForFirstTier ? expDeducted : playerData.getExp() - expNeededForFirstTier);
                            }
                        } else {
                            expChange = -1 * cfgManager.getArenaClassicRewardWinExp() * cfgManager.getArenaClassicRewardLoseExpRate();
                        }
                        // 輸家處召喚閃電效果
                        Location loc = ((ClassicGamerData) gamerData).getRecentLocation();
                        loc.getWorld().strikeLightningEffect(loc);
                        if (player.isDead()) {
                            // 死亡時，若開啟了自動復活，則執行復活操作，反之為其手動復活設定復活點為原先的位置
                            if (DuelTimePlugin.getInstance().getCfgManager().isArenaClassicAutoRespawnEnabled()) {
                                if ((DuelTimePlugin.getInstance().getCfgManager().getArenaClassicAutoRespawnCode().equalsIgnoreCase(RespawnCode.SPIGOT.name()))) {
                                    player.spigot().respawn();
                                } else {
                                    player.setHealth(player.getMaxHealth());
                                }
                                Bukkit.getScheduler().runTaskLater(DuelTimePlugin.getInstance(), () -> player.teleport(back), 5);
                            } else {
                                ClassicArenaListener.respawnLocMap.put(player.getName(), back);
                            }
                        } else {
                            // 若未死亡，說明是輸入退出指令的情形（或者被自動復活了），則直接傳送
                            player.teleport(back);
                        }
                    }
                    playerData.setExp(playerData.getExp() + expChange);
                    playerData.accumulateTotalGameNumber();
                    playerData.accumulateArenaClassicTime(time);
                    playerData.accumulateTotalGameTime(time);
                    playerDataCache.set(playerName, playerData);
                    if (result == ClassicArenaRecordData.Result.WIN) {
                        MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_EARN_POINT, player,
                                "" + cfgManager.getArenaClassicRewardWinPoint(), "" + playerData.getPoint());
                    }
                }
                // 用一句話告知結果（若還線上）
                if (player != null) {
                    MsgBuilder.send(resultMsg, player, false);
                    if (streakEnabled && streakShowMessage && matchStreakManager != null) {
                        DynamicLang.send(player, true,
                                "Dynamic.streak.current",
                                "&7Current streak: &f{streak}",
                                "streak", streakDisplay);
                    }
                }
                // 用多行提示語告知比賽歷程的具體資訊，體現DT3重視過程的設計理念
                String opponentPlayerName = getOpponent(playerName);
                double totalDamage = UtilMath.round(classicGamerData.getTotalDamage());
                double maxDamage = UtilMath.round(classicGamerData.getMaxDamage());
                double averageDamage = classicGamerData.getHitTime() != 0 ? UtilMath.round(classicGamerData.getTotalDamage() / classicGamerData.getHitTime()) : 0;
                MsgBuilder.sendsClickable(Msg.ARENA_TYPE_CLASSIC_END_NOTIFY_INFO, player, false,
                        getArenaData().getName(),
                        opponentPlayerName,
                        DuelTimePlugin.getInstance().getLevelManager().getTier(opponentPlayerName).getTitle(),
                        "" + time,
                        UtilFormat.distinguishPositiveNumber(expChange),
                        "" + totalDamage,
                        "" + maxDamage,
                        "" + averageDamage);
                // 記錄比賽資料
                ClassicArenaRecordData recordData = new ClassicArenaRecordData(
                        playerName,
                        getArenaData().getId(),
                        opponentPlayerName,
                        result,
                        time,
                        expChange,
                        classicGamerData.getHitTime(),
                        UtilMath.round(classicGamerData.getTotalDamage()),
                        UtilMath.round(classicGamerData.getMaxDamage()),
                        classicGamerData.getHitTime() != 0 ? UtilMath.round(classicGamerData.getTotalDamage() / classicGamerData.getHitTime()) : 0,
                        new SimpleDateFormat("yyyy/M/d HH:mm").format(new Date())
                );
                recordCache.add(player, recordData);
            }
        } else {
            // 強制停賽，則直接把玩家帶回大廳或原點
            Location lobby = DuelTimePlugin.getInstance().getCacheManager().getLocationCache().get(LocationCache.InternalType.LOBBY.getId());
            for (BaseGamerData gamerData : getGamerDataList()) {
                Location back = lobby != null ? lobby : ((ClassicGamerData) gamerData).getOriginalLocation();
                gamerData.getPlayer().teleport(back);
            }
        }
        // 告知觀眾比賽結果並傳送回原位置
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        if (getArenaData().hasFunction(CLASSIC_SPECTATE)) {
            if ((boolean) (getArenaData().getFunctionData(CLASSIC_SPECTATE)[3])) {
                for (BossBar bossBar : healthBossBars.values()) {
                    bossBar.removeAll();
                }
                healthBossBars.clear();
            }
            for (BaseSpectatorData spectatorData : new ArrayList<>(getSpectatorDataList())) {
                Player spectator = spectatorData.getPlayer();
                Location logLocation = ((ClassicSpectatorData) spectatorData).getOriginalLocation();
                if (spectator != null) {
                    switch (result) {
                        case CLEAR:
                            MsgBuilder.sends(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_OVER, spectator,
                                    getName(), UtilFormat.toString(getGamerDataList()), winner.getName(), "" + time);
                            break;
                        case DRAW:
                            MsgBuilder.sends(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_OVER_IN_A_DRAW, spectator,
                                    getName(), UtilFormat.toString(getGamerDataList()), "" + time);
                            break;
                        case STOPPED:
                            MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_OVER_BY_STOP, spectator,
                                    getName());
                    }
                    // 恢復原本的遊戲模式
                    spectator.setGameMode(((ClassicSpectatorData) spectatorData).getOriginalGameMode());
                    // 根據配置實現延時傳送
                    if (cfgManager.isArenaClassicDelayedBackEnabled()) {
                        AtomicInteger countdown = new AtomicInteger(cfgManager.getArenaClassicDelayedBackTime());
                        String originalWorldName = spectator.getWorld().getName();
                        Location originalLocation = ((ClassicSpectatorData) spectatorData).getOriginalLocation();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                countdown.getAndDecrement();
                                if (countdown.get() == 0) {
                                    if (ViaVersion.getOnlinePlayers().contains(spectator) && spectator.getWorld().getName().equals(originalWorldName)) {
                                        spectator.teleport(originalLocation);
                                    }
                                    this.cancel();
                                } else if (cfgManager.getArenaClassicDelayedBackTime() - countdown.get() >= 2) {
                                    MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_SPECTATOR_TITLE, Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_SPECTATOR_SUBTITLE, 0, 25, 0, spectator, ViaVersion.TitleType.LINE,
                                            "" + countdown);
                                }
                            }
                        }.runTaskTimer(DuelTimePlugin.getInstance(), 20, 20);
                        BaseArenaListener.tempMovePermit.put(spectator.getName(), System.currentTimeMillis() + (cfgManager.getArenaClassicDelayedBackTime() + 1) * 1000L);
                    } else {
                        spectator.teleport(logLocation);
                    }
                }
                // 從ArenaManager中移除觀賽者與競技場的對應關系並隨之清空觀賽者列表
                arenaManager.removeSpectator(spectator);
            }
        }
        // 全服廣播
        DiscordWebhookManager webhookManager = DuelTimePlugin.getInstance().getDiscordWebhookManager();
        if (webhookManager != null && result != Result.STOPPED) {
            String player1 = getGamerDataList().size() > 0 ? getGamerDataList().get(0).getPlayerName() : "-";
            String player2 = getGamerDataList().size() > 1 ? getGamerDataList().get(1).getPlayerName() : "-";
            String winnerName = result == Result.CLEAR && winner != null ? winner.getName() : "-";
            String loserName = "-";
            if (result == Result.CLEAR && winner != null) {
                Player loserPlayer = getOpponent(winner);
                if (loserPlayer != null) {
                    loserName = loserPlayer.getName();
                } else {
                    String loserNameByLookup = getOpponent(winnerName);
                    if (loserNameByLookup != null) {
                        loserName = loserNameByLookup;
                    }
                }
            }
            webhookManager.sendBattleReport(
                    getId(),
                    getName(),
                    result,
                    winnerName,
                    loserName,
                    player1,
                    player2,
                    time);
        }
        if (result == Result.CLEAR) {
            MsgBuilder.broadcast(Msg.ARENA_TYPE_CLASSIC_END_BROADCAST, false,
                    getName(), winner.getName(), getOpponent(winner).getName(), "" + time);
        } else if (result == Result.DRAW) {
            MsgBuilder.broadcast(Msg.ARENA_TYPE_CLASSIC_END_BROADCAST_DRAW, false,
                    getName(), getGamerDataList().get(0).getPlayer().getName(), getGamerDataList().get(1).getPlayer().getName(), "" + time);
        }
        // 清空參賽者資料
        setGamerDataList(new ArrayList<>());
        // 將競技場恢復為等待狀態
        setState(State.WAITING);
    }

    public void confirmResult(ClassicArena.Result result, Player winner) {
        this.result = result;
        this.winner = winner;
    }

    public Stage getStage() {
        return stage;
    }

    public void addSpectator(Player player) {
        if (getGamerData(player.getName()) != null) {
            return;
        }
        addSpectatorData(new ClassicSpectatorData(player, player.getLocation(), player.getGameMode()));
        if (getArenaData().hasFunction(CLASSIC_SPECTATE)) {
            for (BossBar bossBar : healthBossBars.values()) {
                bossBar.addPlayer(player);
            }
        }
    }

    public void updateGamerHealthSpectated(Player player) {
        if (!healthBossBars.isEmpty()) {
            BossBar bossBar = healthBossBars.get(player.getName());
            bossBar.setProgress(player.getHealth() / player.getMaxHealth());
            bossBar.setTitle(MsgBuilder.get(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_GAMER_HEALTH_BOSSBAR, player,
                    player.getName(), "" + UtilFormat.round(player.getHealth(), 1), "" + UtilFormat.round(player.getMaxHealth(), 1)));
        } else {

        }
    }

    public HashMap<String, BossBar> getHealthBossBars() {
        return healthBossBars;
    }

    public Location getPlayerStartLocationMap(String playerName) {
        return playerStartLocationMap.get(playerName);
    }

    public enum Stage {
        COUNTDOWN,
        GAME
    }

    public enum Result {
        CLEAR,
        DRAW,
        STOPPED
    }

    public enum RespawnCode {
        SPIGOT,
        SETHEALTH
    }
}
