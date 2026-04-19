package com.kevin.dueltime4.listener.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.ClassicArena;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.arena.gamer.ClassicGamerData;
import com.kevin.dueltime4.arena.spectator.ClassicSpectatorData;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.sub.CommandPermission;
import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.event.arena.*;
import com.kevin.dueltime4.gui.simple.ItemDetailInventoryHolder;
import com.kevin.dueltime4.util.UtilGeometry;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kevin.dueltime4.arena.type.ArenaType.FunctionInternalType.*;

public class ClassicArenaListener implements Listener {
    /*
    經典模式玩家傷害事件：
    更新打擊資料
    更新觀戰附加功能中的實時血量
    阻止倒計時期間的傷害
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!((event.getEntity()) instanceof Player)) {
            return;
        }
        Entity damagerEntity = event.getDamager();
        Player attacker;
        if (damagerEntity instanceof Player) {
            attacker = (Player) damagerEntity;
        } else if (damagerEntity instanceof Projectile) {
            ProjectileSource projectileSource = ((Projectile) damagerEntity).getShooter();
            if (!(projectileSource instanceof Player)) {
                return;
            }
            attacker = (Player) projectileSource;
        } else {
            return;
        }
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        Player target = (Player) event.getEntity();
        BaseArena arena = arenaManager.getOf(target);
        if (!(arena instanceof ClassicArena)) {
            return;
        }
        BaseArena attackerArena = arenaManager.getOf(attacker);
        if (attackerArena == null || !arena.getId().equals(attackerArena.getArenaData().getId())) {
            return;
        }
        if (((ClassicArena) arena).getStage() == ClassicArena.Stage.COUNTDOWN) {
            event.setCancelled(true);
            MsgBuilder.sendActionBar(Msg.ARENA_TYPE_CLASSIC_DAMAGE_DURING_COUNTDOWN, attacker, true);
        } else {
            ClassicGamerData gamerData = (ClassicGamerData) arena.getGamerData(attacker.getName());
            gamerData.addHitTime();
            gamerData.addDamage(event.getDamage());
            gamerData.checkAndSetMaxDamage(event.getDamage());
        }
        if (arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_SPECTATE) && (boolean) (arena.getArenaData().getFunctionData(CLASSIC_SPECTATE)[3])) {
            ((ClassicArena) arena).updateGamerHealthSpectated(target);
        }
    }

    /*
    經典模式玩家死亡事件：
    若有一方死亡，無論什麼原因，直接視為這一方輸掉遊戲
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        BaseArena arena = DuelTimePlugin.getInstance().getArenaManager().getOf(event.getEntity());
        if (arena == null) return;
        if (!(arena instanceof ClassicArena)) return;
        ruleAsLoss((ClassicArena) arena, event.getEntity(), null, QuitPenaltyTrigger.NONE);
    }

    /*
    經典模式玩家下線事件：
    下線一方直接視為輸掉遊戲
     */
    @EventHandler
    public void onLeaveServer(PlayerQuitEvent event) {
        if (DuelTimePlugin.isServerShuttingDown()
                && DuelTimePlugin.getInstance().getCfgManager().isRestartProtectionEnabled()) {
            return;
        }
        BaseArena arena = DuelTimePlugin.getInstance().getArenaManager().getOf(event.getPlayer());
        if (arena == null) return;
        if (!(arena instanceof ClassicArena)) return;
        Player player = event.getPlayer();
        Player opponent = ((ClassicArena) arena).getOpponent(player);
        ruleAsLoss((ClassicArena) arena, player,
                () -> MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_LEAVE_SERVER_INFORM_OPPONENT, opponent),
                QuitPenaltyTrigger.DISCONNECT);
    }

    /*
    經典模式玩家請求退出場比賽事件
     */
    @EventHandler
    public void onTryToQuit(ArenaTryToQuitEvent event) {
        BaseArena arena = event.getArena();
        if (arena == null) return;
        if (!(arena instanceof ClassicArena)) return;
        Player player = event.getPlayer();
        Player opponent = ((ClassicArena) arena).getOpponent(player);
        ruleAsLoss((ClassicArena) arena, player, () -> {
            MsgBuilder.send(Msg.COMMAND_SUB_QUIT_SUCCESSFULLY, player, arena.getArenaData().getName());
            MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_QUIT_INFORM_OPPONENT, opponent);
        }, QuitPenaltyTrigger.QUIT_COMMAND);

    }

    // 僅僅用於減少重復程式碼，用於處理那些必定判為輸的情況，如死亡、下線等
    private void ruleAsLoss(ClassicArena arena, Player player, Action action, QuitPenaltyTrigger penaltyTrigger) {
        if (arena == null || player == null) {
            return;
        }
        Player opponent = arena.getOpponent(player);
        if (opponent == null) {
            DuelTimePlugin.getInstance().getArenaManager().stop(arena.getId(), "opponent-missing");
            return;
        }
        if (action != null) {
            action.run();
        }
        if (shouldApplyQuitPenalty(penaltyTrigger)) {
            applyQuitPenalty(player, penaltyTrigger, opponent.getName());
        }
        arena.confirmResult(ClassicArena.Result.CLEAR, opponent);
        ClassicGamerData loserData = (ClassicGamerData) arena.getGamerData(player.getName());
        if (loserData != null) {
            loserData.confirmResult(ClassicArenaRecordData.Result.LOSE);
        }
        ClassicGamerData winnerData = (ClassicGamerData) arena.getGamerData(opponent.getName());
        if (winnerData != null) {
            winnerData.confirmResult(ClassicArenaRecordData.Result.WIN);
        }
        DuelTimePlugin.getInstance().getArenaManager().end(arena.getId());
    }

    private void applyQuitPenalty(Player player, QuitPenaltyTrigger trigger, String opponentName) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        CfgManager cfgManager = plugin.getCfgManager();
        if (!cfgManager.isArenaClassicLeavePenaltyEnabled()) {
            return;
        }

        double pointPenalty = cfgManager.getArenaClassicLeavePenaltyPoint();
        int queueCooldown = cfgManager.getArenaClassicLeavePenaltyCooldown();
        boolean canApplyPoint = cfgManager.isArenaClassicLeavePenaltyApplyPointDeduction() && pointPenalty > 0;
        boolean canApplyCooldown = cfgManager.isArenaClassicLeavePenaltyApplyQueueCooldown() && queueCooldown > 0;
        if (!canApplyPoint && !canApplyCooldown) {
            return;
        }

        String playerName = player.getName();
        double deductedPoint = 0D;
        double currentPoint = 0D;
        boolean hasCurrentPoint = false;
        if (canApplyPoint) {
            PlayerDataCache playerDataCache = plugin.getCacheManager().getPlayerDataCache();
            PlayerData playerData = playerDataCache.getAnyway(playerName);
            if (playerData != null) {
                double before = playerData.getPoint();
                double after = Math.max(0, before - pointPenalty);
                deductedPoint = Math.max(0, before - after);
                if (after != before) {
                    playerData.setPoint(after);
                    playerDataCache.set(playerName, playerData);
                }
                currentPoint = after;
                hasCurrentPoint = true;
                if (player.isOnline()) {
                    DynamicLang.send(player, true,
                            "Dynamic.leave-penalty.point-deducted",
                            "&cYou left an active match: -{point} point(s). Current: &f{current}",
                            "point", String.valueOf(pointPenalty),
                            "current", String.valueOf(after));
                }
            }
        }

        if (canApplyCooldown) {
            plugin.getArenaManager().applyQueuePenalty(playerName, queueCooldown);
            if (player.isOnline()) {
                DynamicLang.send(player, true,
                        "Dynamic.leave-penalty.queue-cooldown",
                        "&eQueue cooldown applied: {seconds} second(s).",
                        "seconds", String.valueOf(queueCooldown));
            }
        }

        if (!hasCurrentPoint) {
            PlayerData playerData = plugin.getCacheManager().getPlayerDataCache().getAnyway(playerName);
            if (playerData != null) {
                currentPoint = playerData.getPoint();
                hasCurrentPoint = true;
            }
        }

        if (plugin.getDiscordWebhookManager() != null) {
            String reasonKey = switch (trigger) {
                case QUIT_COMMAND -> "quit-command";
                case DISCONNECT -> "disconnect";
                default -> "unknown";
            };
            plugin.getDiscordWebhookManager().sendLeavePenalty(
                    playerName,
                    opponentName,
                    reasonKey,
                    deductedPoint,
                    hasCurrentPoint ? currentPoint : 0D,
                    canApplyCooldown ? queueCooldown : 0);
        }
    }

    private boolean shouldApplyQuitPenalty(QuitPenaltyTrigger trigger) {
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        if (!cfgManager.isArenaClassicLeavePenaltyEnabled()) {
            return false;
        }
        if (DuelTimePlugin.isServerShuttingDown() && cfgManager.isRestartProtectionEnabled()) {
            return false;
        }
        return switch (trigger) {
            case QUIT_COMMAND -> cfgManager.isArenaClassicLeavePenaltyApplyOnQuitCommand();
            case DISCONNECT -> cfgManager.isArenaClassicLeavePenaltyApplyOnDisconnect();
            default -> false;
        };
    }

    private interface Action {
        void run();
    }

    private enum QuitPenaltyTrigger {
        NONE,
        QUIT_COMMAND,
        DISCONNECT
    }

    /*
    經典模式玩家移動事件：
    根據附加功能資料判定倒計時期間禁止移動與否
    比賽開始後參賽者禁止離開競技場區域
    比賽開始後觀眾禁止離開觀眾席區域
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BaseArena baseArena = DuelTimePlugin.getInstance().getArenaManager().getOf(player);
        if (baseArena instanceof ClassicArena) {
            // 是選手
            ClassicArena arena = (ClassicArena) baseArena;
            if (arena.getStage().equals(ClassicArena.Stage.COUNTDOWN)) {
                if (!(boolean) arena.getArenaData().getFunctionData(CLASSIC_COUNTDOWN)[1]) {
                    player.teleport(arena.getPlayerStartLocationMap(player.getName()));
                }
            } else {
                ClassicGamerData gamerData = ((ClassicGamerData) arena.getGamerData(player.getName()));
                if (!UtilGeometry.inArena(event.getTo(), arena)) {
                    player.teleport(gamerData.getRecentLocation());
                } else {
                    gamerData.updateRecentLocation(player.getLocation());
                }
            }
            return;
        }
        baseArena = DuelTimePlugin.getInstance().getArenaManager().getSpectate(player);
        if (baseArena instanceof ClassicArena) {
            // 是觀眾
            Object[] spectateFunctionData = baseArena.getArenaData().getFunctionData(ArenaType.FunctionInternalType.CLASSIC_SPECTATE);
            Location spectateZoneD1 = (Location) spectateFunctionData[0];
            Location spectateZoneD2 = (Location) spectateFunctionData[1];
            ClassicSpectatorData spectatorData = (ClassicSpectatorData) baseArena.getSpector(player.getName());
            if (!UtilGeometry.inZone(event.getTo(), spectateZoneD1, spectateZoneD2)) {
                player.teleport(spectatorData.getRecentLocation());
                MsgBuilder.sendActionBar(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_SPECTATOR_MOVE_OUT_ZONE, player, true);
            } else {
                spectatorData.updateRecentLocation(player.getLocation());
            }
        }
    }

    /*
    經典模式輸入指令事件
    禁止非op玩家使用除了quit以外的所有指令
     */
    @EventHandler
    public void onEnterCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        BaseArena arena = DuelTimePlugin.getInstance().getArenaManager().getOf(player);
        if (arena == null) {
            return;
        }
        if (!(arena instanceof ClassicArena)) {
            return;
        }
        String commandEntered = event.getMessage();
        List<String> commandAliases = DuelTimePlugin.getInstance().getCommand("dueltime").getAliases();
        boolean isQuitCommand = false;
        for (String commandAlias : commandAliases) {
            for (String labelAlias : DuelTimePlugin.getInstance().getCommandHandler().getSubCommand("quit").getAliases()) {
                if (commandEntered.equals("/" + commandAlias + " " + labelAlias)) {
                    isQuitCommand = true;
                    break;
                }
            }
        }
        if (!isQuitCommand) {
            if (player.hasPermission(CommandPermission.ADMIN)) {
                return;
            }
            MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_USE_COMMAND_IN_GAME, player);
            event.setCancelled(true);
        }
    }

    /*
    經典模式請求開賽事件
    檢測各種開賽條件，如物品檢測（附加功能）等
     */
    @EventHandler
    public void onTryToStart(ArenaTryToStartEvent event) {
        BaseArena baseArena = event.getArena();
        if (!(baseArena instanceof ClassicArena)) {
            return;
        }
        ClassicArena arena = (ClassicArena) baseArena;
        boolean checkKeyword = arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_INVENTORY_CHECK_KEYWORD);
        boolean checkMaterial = arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_INVENTORY_CHECK_TYPE);
        if (!checkKeyword && !checkMaterial) {
            return;
        }
        String checkRange = null;
        List<String> keywords = null;
        List<String> types = null;
        if (checkKeyword) {
            Object[] checkKeywordFuncData = arena.getArenaData().getFunctionData(ArenaType.FunctionInternalType.CLASSIC_INVENTORY_CHECK_KEYWORD);
            checkRange = (String) checkKeywordFuncData[0];
            keywords = (List<String>) checkKeywordFuncData[1];
        }
        if (checkMaterial) {
            types = (List<String>) arena.getArenaData().getFunctionData(ArenaType.FunctionInternalType.CLASSIC_INVENTORY_CHECK_TYPE)[0];
        }
        Player playerDetected = null;
        ItemStack itemStackDetected = null;
        boolean isKeywordDetected = false;
        Player[] players = event.getPlayers();
        check:
        for (Player player : players) {
            Inventory inventory = player.getInventory();
            for (int slot = -1; slot < 40; slot++) {
                // -1代表遊標上的物品，防止玩家把物品藏在遊標上逃避檢測
                ItemStack itemStack = slot == -1 ? player.getItemOnCursor() : inventory.getItem(slot);
                if (itemStack == null) continue;
                if (keywords != null && itemStack.hasItemMeta()) {
                    if (checkRange.equals("name") || checkRange.equals("all")) {
                        String displayName = itemStack.getItemMeta().getDisplayName();
                        if (displayName == null) continue;
                        for (String keyword : keywords) {
                            if (displayName.contains(keyword)) {
                                playerDetected = player;
                                itemStackDetected = itemStack;
                                isKeywordDetected = true;
                                break check;
                            }
                        }
                    }
                    if (checkRange.equals("lore") || checkRange.equals("all")) {
                        List<String> lores = itemStack.getItemMeta().getLore();
                        if (lores == null) continue;
                        for (String lore : lores) {
                            for (String keyword : keywords) {
                                if (lore.contains(keyword)) {
                                    playerDetected = player;
                                    itemStackDetected = itemStack;
                                    isKeywordDetected = true;
                                    break check;
                                }
                            }
                        }
                    }
                }
                if (types != null) {
                    for (String type : types) {
                        String[] clips = type.split(":");
                        String material = clips[0];
                        byte subId = clips.length == 1 ? (byte) 0 : Byte.parseByte(clips[1]);
                        if (itemStack.getType().name().equalsIgnoreCase(material) && itemStack.getData().getData() == subId) {
                            playerDetected = player;
                            itemStackDetected = itemStack;
                            break check;
                        }
                    }
                }
            }
        }
        if (itemStackDetected != null) {
            event.setCancelled(true);
            String name = itemStackDetected.getItemMeta().getDisplayName();
            if (name == null) name = itemStackDetected.getType().name();
            List<String> lore = itemStackDetected.getItemMeta().getLore();
            String loreStr = name + (lore != null ? "||" + String.join("||", lore) : "");
            ItemDetailInventoryHolder.itemMap.put(playerDetected.getName(), itemStackDetected);
            if (isKeywordDetected) {
                MsgBuilder.sendClickable(Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_KEYWORD_DETECTED_TIP, playerDetected, true,
                        arena.getName(), name, loreStr);
                for (Player player : players)
                    MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_KEYWORD_DETECTED, player,
                            playerDetected.getName(), arena.getName());
            } else {
                MsgBuilder.sendClickable(Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_TYPE_DETECTED_TIP, playerDetected, true,
                        arena.getName(), name, loreStr);
                for (Player player : players)
                    MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_FUNCTION_INVENTORY_CHECK_TYPE_DETECTED, player,
                            playerDetected.getName(), arena.getName());
            }
        }
    }

    /*
    經典模式正式開賽事件（具體點來說是競技場透過start()初始化完畢後，而不是倒計時結束後）
    用於處理賽前指令（即入場後的一瞬間執行的指令）
     */
    @EventHandler
    public void onStart(ArenaStartEvent event) {
        BaseArena baseArena = event.getArena();
        if (!(baseArena instanceof ClassicArena)) {
            return;
        }
        ClassicArena arena = (ClassicArena) baseArena;
        if (arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_PRE_GAME_COMMAND)) {
            List<String> commandDataList = (List<String>) (arena.getArenaData().getFunctionData(ArenaType.FunctionInternalType.CLASSIC_PRE_GAME_COMMAND)[0]);
            for (String commandData : commandDataList) {
                String identity = commandData.split(":")[0];
                String content = commandData.substring(identity.length() + 1)
                        .replace("{world}", arena.getArenaData().getDiagonalPointLocation1().getWorld().getName());
                if (identity.equalsIgnoreCase("single_console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content);
                } else {
                    for (BaseGamerData gamerData : arena.getGamerDataList()) {
                        Player player = gamerData.getPlayer();
                        content = content.replace("{player}", player.getName());
                        switch (identity) {
                            case "player":
                                Bukkit.dispatchCommand(gamerData.getPlayer(), content);
                                break;
                            case "op":
                                boolean isOpBefore = player.isOp();
                                if (!isOpBefore) player.setOp(true);
                                Bukkit.dispatchCommand(gamerData.getPlayer(), content);
                                if (!isOpBefore) player.setOp(false);
                                break;
                            case "console":
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content);
                        }
                    }
                }
            }
        }
    }

    /*
    經典模式請求觀戰事件
     */
    @EventHandler
    public void onTryToSpectate(ArenaTryToSpectateEvent event) {
        BaseArena baseArena = event.getArena();
        if (!(baseArena instanceof ClassicArena)) {
            return;
        }
        ClassicArena arena = (ClassicArena) baseArena;
        Player player = event.getPlayer();
        if (!arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_SPECTATE)) {
            MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_FAIL_UNAVAILABLE, player,
                    arena.getArenaData().getName());
            return;
        }
        arena.addSpectator(player);
        player.teleport((Location) (arena.getArenaData().getFunctionData(ArenaType.FunctionInternalType.CLASSIC_SPECTATE)[2]));
        ClassicSpectatorData spectatorData = (ClassicSpectatorData) arena.getSpector(player.getName());
        if (DuelTimePlugin.serverVersionInt >= 8) {
            Bukkit.getScheduler().runTaskLater(DuelTimePlugin.getInstance(), () ->
                    player.setGameMode(GameMode.SPECTATOR), 1);
        }
        spectatorData.updateRecentLocation(player.getLocation());
        MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_SUCCESSFULLY, player,
                arena.getArenaData().getName());
    }

    /*
    經典模式請求退出觀戰事件
     */
    @EventHandler
    public void onTryToQuitSpectate(ArenaTryToQuitSpectateEvent event) {
        BaseArena baseArena = event.getArena();
        if (!(baseArena instanceof ClassicArena)) {
            return;
        }
        ClassicArena arena = (ClassicArena) baseArena;
        Player player = event.getPlayer();
        ClassicSpectatorData spectatorData = (ClassicSpectatorData) arena.getSpector(player.getName());
        if (arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_SPECTATE) && (boolean) (arena.getArenaData().getFunctionData(CLASSIC_SPECTATE)[3])) {
            for (BossBar bossBar : arena.getHealthBossBars().values()) {
                bossBar.removePlayer(player);
            }
        }
        if (DuelTimePlugin.serverVersionInt >= 8) {
            player.setGameMode(spectatorData.getOriginalGameMode());
        }
        spectateLeaveWorldSkipCheck.add(player.getName());
        player.teleport(spectatorData.getOriginalLocation());
        spectateLeaveWorldSkipCheck.remove(player.getName());
    }

    /*
    經典模式請求強制停止競技場比賽事件
     */
    @EventHandler
    public void onTryToStop(ArenaTryToStopEvent event) {
        BaseArena arena = event.getArena();
        if (!(arena instanceof ClassicArena)) {
            return;
        }
        ((ClassicArena) arena).confirmResult(ClassicArena.Result.STOPPED, null);
        for (BaseGamerData baseGamerData : arena.getGamerDataList()) {
            String reason = event.getReason();
            if (reason == null || reason.isEmpty()) {
                MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_END_RESULT_STOPPED, baseGamerData.getPlayer(), false);
            } else {
                MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_END_RESULT_STOPPED_WITH_REASON, baseGamerData.getPlayer(), false,
                        event.getReason());
            }
        }
        DuelTimePlugin.getInstance().getArenaManager().end(arena.getId());
    }

    public static Map<String, Location> respawnLocMap = new HashMap<>();

    /*
    玩家復活事件
    復活後傳送回原點或大廳點（用於為新增自動重生這一附加功能的情況）
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (respawnLocMap.containsKey(player.getName())) {
            Bukkit.getScheduler().runTaskLater(DuelTimePlugin.getInstance(), () -> {
                player.teleport(respawnLocMap.get(player.getName()));
                respawnLocMap.remove(player.getName());
            }, 5);
        }
    }

    /*
    玩家下線事件
    移除潛在的指定重生點，避免影響一些指定上線地點的登入類外掛的運作
     */
    @EventHandler
    public void onLeaveServerWithRespawnLoc(PlayerQuitEvent event) {
        respawnLocMap.remove(event.getPlayer().getName());
    }

    private static final List<String> spectateLeaveWorldSkipCheck = new ArrayList<>();

    /*
    觀眾傳送事件
    一旦發生跨世界傳送，直接視為取消觀看
     */
    @EventHandler
    public void onSpectatorLeaveArenaWorld(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (spectateLeaveWorldSkipCheck.contains(player.getName())) return;
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena = arenaManager.getSpectate(player);
        if (arena == null) {
            return;
        }
        if (event.getTo().getWorld().getName().equals(arena.getArenaData().getDiagonalPointLocation1().getWorld().getName())) {
            return;
        }
        if (DuelTimePlugin.serverVersionInt >= 8) {
            player.setGameMode(((ClassicSpectatorData) arena.getSpector(player.getName())).getOriginalGameMode());
        }
        arenaManager.removeSpectator(player);
    }

    /*
    觀眾離開伺服器事件
    一旦離開伺服器，直接視為取消觀看
     */
    @EventHandler
    public void onSpectatorLeaveServer(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena = arenaManager.getSpectate(player);
        if (arena == null) {
            return;
        }
        if (DuelTimePlugin.serverVersionInt >= 8) {
            player.setGameMode(((ClassicSpectatorData) arena.getSpector(player.getName())).getOriginalGameMode());
        }
        arenaManager.removeSpectator(player);
    }

    /*
    實體生成事件
    如果競技場新增了禁止實體刷出的附加功能且涉及實體種類不在白名單內，則阻止
     */
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        BaseArena baseArena = UtilGeometry.getArena(event.getLocation());
        if (!(baseArena instanceof ClassicArena)) {
            return;
        }
        ClassicArena arena = (ClassicArena) baseArena;
        if (arena.getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_BAN_ENTITY_SPAWN)) {
            List<String> whitelist = (List<String>) arena.getArenaData().getFunctionData(CLASSIC_BAN_ENTITY_SPAWN)[0];
            if (!whitelist.contains(event.getEntityType().name())) {
                event.setCancelled(true);
            }
        }
    }
}
