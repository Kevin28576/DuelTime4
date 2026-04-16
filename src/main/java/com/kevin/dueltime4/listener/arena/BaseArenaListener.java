package com.kevin.dueltime4.listener.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.command.sub.CommandPermission;
import com.kevin.dueltime4.event.arena.ArenaEndEvent;
import com.kevin.dueltime4.stats.Metrics;
import com.kevin.dueltime4.util.UtilGeometry;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.List;

public class BaseArenaListener implements Listener {
    public static HashMap<String, Long> tempMovePermit = new HashMap<>();

    /*
    阻止玩家進入空閑或停用中的的比賽場地
    阻止任何非選手且非觀眾的玩家在其中移動
     */
    @EventHandler
    public void moveIn(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) return;
        Location to = event.getTo();
        BaseArena arena = UtilGeometry.getArena(to);
        if (arena != null) {
            if (arena.getState() == BaseArena.State.WAITING || arena.getState() == BaseArena.State.DISABLED) {
                // 等待狀態或停用狀態下，除非有臨時移動許可權，一律不可入內
                if (tempMovePermit.getOrDefault(player.getName(), 0L) > System.currentTimeMillis()) {
                    return;
                }
                player.teleport(event.getFrom());
                MsgBuilder.sendActionBar(MsgBuilder.get(Msg.ARENA_PROTECTION_WALK_IN_WHILE_AVAILABLE, player, arena.getName()), player, true);
            } else {
                if (!arena.hasPlayer(player) && !arena.hasSpectator(player)) {
                    // 比賽進行時，不允許非選手且非觀眾的玩家入內
                    player.teleport(event.getFrom());
                    MsgBuilder.sendActionBar(MsgBuilder.get(Msg.ARENA_PROTECTION_WALK_IN_WHILE_IN_PROGRESS, player, arena.getName()), player, true);
                }
            }
        }
    }

    /*
    阻止玩家傳送到空閑或停用中的比賽場地
    阻止任何非選手且非觀眾的玩家傳送過去
     */
    @EventHandler
    public void teleportTo(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) return;
        Location to = event.getTo();
        BaseArena arena = UtilGeometry.getArena(to);
        if (arena != null) {
            if (arena.getState() == BaseArena.State.WAITING || arena.getState() == BaseArena.State.DISABLED) {
                // 等待狀態或停用狀態下，一律不許傳送過去
                event.setCancelled(true);
                MsgBuilder.sendActionBar(MsgBuilder.get(Msg.ARENA_PROTECTION_TELEPORT_TO_WHILE_AVAILABLE, player, arena.getName()), player, true);
            } else {
                if (!arena.hasPlayer(player) && !arena.hasSpectator(player)) {
                    // 比賽進行時，不允許非選手且非觀眾的玩家傳送過去
                    event.setCancelled(true);
                    MsgBuilder.sendActionBar(MsgBuilder.get(Msg.ARENA_PROTECTION_TELEPORT_TO_WHILE_IN_PROGRESS, player, arena.getName()), player, true);
                }
            }
        }
    }

    /*
    阻止比賽過程中，選手受到來自其他選手以外所有玩家的直接攻擊（如近戰傷害等）
     */
    @EventHandler
    public void attackGamerDirectly(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getEntity();
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena = arenaManager.getOf(target);
        if (arena == null) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        BaseArena attackerArena = arenaManager.getOf(attacker);
        if (attackerArena == null) {
            // 說明為觀眾或者無關人員，阻止攻擊，並予以提示
            event.setCancelled(true);
            MsgBuilder.sendActionBar(MsgBuilder.get(Msg.ARENA_PROTECTION_ATTACK_GAMER, attacker, arena.getName()), attacker, true);
        } else {
            if (!attackerArena.getId().equals(arena.getId())) {
                // 說明攻擊方是其他競技場的，雖然這種情況不太平凡，但還是考慮一下，不予提示
                event.setCancelled(true);
            }
        }
    }

    /*
    阻止比賽過程中，選手受到來自其他選手以外所有玩家的間接攻擊（如射擊等）
     */
    @EventHandler
    public void attackGamerInDirectly(EntityDamageByEntityEvent event) {
        // 考慮到部分種類的槍械模組的相容性問題，這個暫時不寫
    }

    /*
    阻止玩家在空閑的比賽場地破壞
    阻止非選手的玩家在比賽中的場地破壞
    阻止玩家破壞場地外的任何方塊
    阻止玩家在比賽過程中破壞比賽場地內白名單以外的方塊
     */
    @EventHandler
    public void breakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) {
            return;
        }
        checkBehaviourWithPlayer(player, event.getBlock().getLocation(), event.getBlock().getType(), event, Msg.ARENA_PROTECTION_BREAK, ArenaType.PresetType.PROTECTION_BREAK);
    }

    /*
    阻止玩家在空閑的比賽場地內部和上空放置方塊
    阻止非選手的玩家在比賽中的場地的內部和上空放置方塊
    阻止玩家在場地外放置任何方塊
    阻止玩家在比賽過程中在比賽場地內部放置白名單以外的方塊
     */
    @EventHandler
    public void placeBlock(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) {
            return;
        }
        BaseArena playerArena = DuelTimePlugin.getInstance().getArenaManager().getOf(player);
        BaseArena blockArena = UtilGeometry.getArena(event.getBlock().getLocation());
        if (playerArena == null) {
            boolean isArenaBelow = false;
            if (blockArena == null) {
                blockArena = UtilGeometry.getArenaBelow(event.getBlock().getLocation());
                if (blockArena == null) {
                    return;
                }
                isArenaBelow = true;
            }
            event.setCancelled(true);
            MsgBuilder.sendActionBar(MsgBuilder.get(isArenaBelow ? Msg.ARENA_PROTECTION_PLACE_OVER : Msg.ARENA_PROTECTION_PLACE, player, blockArena.getName()), player, true);
        } else {
            if (blockArena == null || !blockArena.getId().equals(playerArena.getId())) {
                event.setCancelled(true);
            } else {
                Object data = playerArena.getArenaType().getPresets().get(ArenaType.PresetType.PROTECTION_PLACE);
                if (data != null && ((List<Material>) data).contains(event.getBlock().getType())) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }

    /*
    互動方塊的情形，判斷規則類似破壞方塊的情形
     */
    @EventHandler
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        checkBehaviourWithPlayer(player, clickedBlock.getLocation(), clickedBlock.getType(), event, Msg.ARENA_PROTECTION_INTERACT, ArenaType.PresetType.PROTECTION_INTERACT);
    }

    /*
    傾倒液體的情形，判斷規則類似破壞方塊的情形
     */
    @EventHandler
    public void pourLiquid(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) {
            return;
        }
        checkBehaviourWithPlayer(player, event.getBlockClicked().getLocation(), event.getBlockClicked().getType(), event, Msg.ARENA_PROTECTION_POUR_LIQUID, ArenaType.PresetType.PROTECTION_POUR_LIQUID);
    }

    /*
    撈取液體的情形，判斷規則類似破壞方塊的情形
     */
    @EventHandler
    public void fillLiquid(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(CommandPermission.ADMIN)) {
            return;
        }
        checkBehaviourWithPlayer(player, event.getBlockClicked().getLocation(), event.getBlockClicked().getType(), event, Msg.ARENA_PROTECTION_GET_LIQUID, ArenaType.PresetType.PROTECTION_GET_LIQUID);
    }

    /*
    阻止實體破壞空閑場地的門
    根據相關預設存在與否決定是否阻止實體在比賽過程中破壞門
     */
    @EventHandler
    public void entityBreakDoor(EntityBreakDoorEvent event) {
        checkBehaviourWithoutPlayer(event.getEntity().getLocation(), null, event, ArenaType.PresetType.PROTECTION_ENTITY_BREAK_DOOR);
    }

    /*
    阻止實體在空閑場地生成（包括人為的和自然的）
    根據預設白名單決定是否阻止實體在比賽過程中生成
     */
    @EventHandler
    public void entitySpawned(EntitySpawnEvent event) {
        checkBehaviourWithoutPlayer(event.getEntity().getLocation(), event.getEntityType(), event, ArenaType.PresetType.PROTECTION_ENTITY_SPAWN);
    }

    /*
    方塊被點燃的情形，判斷規則類似實體生成的情形，但考慮場地邊界情況時，著火方塊的位置和火焰的位置實際上不一致，所以要考慮以著火點為中心對鄰域進行考慮
     */
    @EventHandler
    public void blockIgnited(BlockIgniteEvent event) {
        Block ignitedBlock = event.getBlock();
        Block[] blocks = {ignitedBlock, ignitedBlock.getRelative(BlockFace.DOWN), ignitedBlock.getRelative(BlockFace.UP),
                ignitedBlock.getRelative(BlockFace.EAST), ignitedBlock.getRelative(BlockFace.SOUTH),
                ignitedBlock.getRelative(BlockFace.WEST), ignitedBlock.getRelative(BlockFace.NORTH)};
        for (Block block : blocks) {
            checkBehaviourWithoutPlayer(block.getLocation(), block.getType(), event, ArenaType.PresetType.PROTECTION_BLOCK_IGNITED);
        }
    }

    /*
    方塊持續燃燒的情形，判斷規則類似實體生成和方塊被點燃的情形
     */
    @EventHandler
    public void blockBurning(BlockBurnEvent event) {
        Block burningBlock = event.getBlock();
        Block[] blocks = {burningBlock, burningBlock.getRelative(BlockFace.DOWN), burningBlock.getRelative(BlockFace.UP),
                burningBlock.getRelative(BlockFace.EAST), burningBlock.getRelative(BlockFace.SOUTH),
                burningBlock.getRelative(BlockFace.WEST), burningBlock.getRelative(BlockFace.NORTH)};
        for (Block block : blocks) {
            checkBehaviourWithoutPlayer(block.getLocation(), block.getType(), event, ArenaType.PresetType.PROTECTION_BLOCK_BURNING);
        }
    }

    /*
    阻止任何情形下透過活塞推入/推出方塊
     */
    @EventHandler
    public void blockMovedIntoOrOutByPiston(BlockPistonExtendEvent event) {
        List<Block> blockPushedList = event.getBlocks();
        Block pistonBlock = event.getBlock();
        BaseArena pistonArena = UtilGeometry.getArena(pistonBlock.getLocation());
        if (pistonArena == null) {
            // 活塞在場地外
            for (Block blockPushed : blockPushedList) {
                // 注意這裡一定要根據活塞的前進方向，確認被推方塊要到達的位置再進行判斷，而不是直接利用被推方塊的位置判斷，下同
                BaseArena arena = UtilGeometry.getArena(blockPushed.getRelative(event.getDirection()).getLocation());
                if (arena != null) {
                    // 目標位置為某個場地，阻止
                    event.setCancelled(true);
                }
            }
        } else {
            // 活塞在場地內
            for (Block blockPushed : blockPushedList) {
                BaseArena targetLocArena = UtilGeometry.getArena(blockPushed.getRelative(event.getDirection()).getLocation());
                if (targetLocArena == null || !targetLocArena.getId().equals(pistonArena.getId())) {
                    // 目標位置非原場地，阻止
                    event.setCancelled(true);
                }
            }
        }
    }

    /*
    阻止任何情形下液體流入場地
     */
    @EventHandler
    public void liquidFlowInto(BlockFromToEvent event) {
        BaseArena arena = UtilGeometry.getArena(event.getToBlock().getLocation());
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    // 用於減少重復程式碼，本類中，有幾個明確有玩家參與且涉及單個方塊的事件具有相似的判定規則
    private void checkBehaviourWithPlayer(Player player, Location blockLocation, Material blockType, Cancellable event, Msg informMsg, ArenaType.PresetType presetType) {
        // 分別獲取玩家當前所屬的競技場和破壞方塊所屬的競技場
        BaseArena playerArena = DuelTimePlugin.getInstance().getArenaManager().getOf(player);
        BaseArena blockArena = UtilGeometry.getArena(blockLocation);
        if (playerArena == null) {
            // 當前玩家不在比賽中，那麼判斷如果當前的方塊處於某個競技場內，則直接阻止
            if (blockArena == null) {
                return;
            }
            event.setCancelled(true);
            MsgBuilder.sendActionBar(MsgBuilder.get(informMsg, player, blockArena.getName()), player, true);
        } else {
            // 當前玩家在比賽中，則判斷當前方塊在不在自己所屬的競技場中，如果不在則一律阻止，反之則根據白名單選擇性阻止
            if (blockArena == null || !blockArena.getId().equals(playerArena.getId())) {
                event.setCancelled(true);
            } else {
                if (!playerArena.getArenaType().getPresets().containsKey(presetType)) {
                    return;
                }
                Object data = playerArena.getArenaType().getPresets().get(presetType);
                if (data != null && ((List<Material>) data).contains(blockType)) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }

    // 用於減少重復程式碼，本類中，有幾個不明確有玩家參與且涉及單個方塊/實體的事件具有相似的判定規則
    private void checkBehaviourWithoutPlayer(Location blockLocation, Object involvedObj, Cancellable event, ArenaType.PresetType presetType) {
        BaseArena arena = UtilGeometry.getArena(blockLocation);
        if (arena == null) {
            return;
        }
        if (arena.getState() == BaseArena.State.WAITING || arena.getState() == BaseArena.State.DISABLED) {
            event.setCancelled(true);
        } else {
            if (!arena.getArenaType().getPresets().containsKey(presetType)) {
                return;
            }
            Object data = arena.getArenaType().getPresets().get(presetType);
            if (presetType.getDataType() != null && data != null && ((List<?>) data).contains(involvedObj)) {
                // 如果預設規定要提供白名單，且該型別的競技場定義了非空白名單，且當前方塊/實體在白名單中，則不阻止
                return;
            }
            event.setCancelled(true);
        }
    }

    /*
    正常結束比賽時累加提交給bstats的資料
     */
    @EventHandler
    public void onArenaEnd(ArenaEndEvent event) {
        Metrics metrics = DuelTimePlugin.getInstance().getMetrics();
        metrics.accumulateGameNumber();
    }
}
