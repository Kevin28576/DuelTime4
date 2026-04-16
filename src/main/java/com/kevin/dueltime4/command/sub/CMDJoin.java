package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.event.arena.ArenaTryToJoinEvent;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class CMDJoin extends SubCommand {

    public CMDJoin() {
        super("join", "j");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
            return true;
        }
        if (args.length < 2) {
            CMDHelp.helpList.sendCorrect(sender, 1, CMDHelp.helpList.getSubCommandById("join"), label, args);
            return true;
        }
        Player player = (Player) sender;
        if (DuelTimePlugin.getInstance().getCacheManager().getBlacklistCache().contains(player.getName())) {
            MsgBuilder.send(Msg.COMMAND_SUB_START_FAIL_SELF_IN_BLACK_LIST, player);
            return true;
        }
        String arenaIdEntered = args[1];
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena = arenaManager.get(arenaIdEntered);
        if (arena == null) {
            MsgBuilder.send(Msg.COMMAND_SUB_JOIN_FAIL_ARENA_NOT_EXISTS, player,
                    arenaIdEntered);
            UtilHelpList.sendSuggest(sender, 1,
                    arenaManager.getList().stream().map(BaseArena::getId).collect(Collectors.toList()), label, args);
            return true;
        }
        if (arena.getState() == BaseArena.State.DISABLED) {
            MsgBuilder.send(Msg.COMMAND_SUB_JOIN_FAIL_ARENA_DISABLED, player,
                    arenaIdEntered);
        } else if (arena.getState() == BaseArena.State.IN_PROGRESS_CLOSED) {
            MsgBuilder.send(Msg.COMMAND_SUB_JOIN_FAIL_ARENA_IN_PROGRESS_CLOSED, player,
                    arenaIdEntered);
        } else if (arena.getState() == BaseArena.State.IN_PROGRESS_OPENED) {
            if (arena.isFull()) {
                MsgBuilder.send(Msg.COMMAND_SUB_JOIN_FAIL_ARENA_IN_PROGRESS_OPENED_FULL, player,
                        arenaIdEntered);
            } else {
                MsgBuilder.send(Msg.COMMAND_SUB_JOIN_GAME_TENTATIVELY, player,
                        arenaIdEntered);
                arenaManager.join(player, arenaIdEntered, ArenaTryToJoinEvent.Way.COMMAND);
            }
        } else if (arena.getState() == BaseArena.State.WAITING) {
            BaseArena arenaWait = arenaManager.getWaitingFor(player);
            if (arenaWait != null && arenaWait.getId().equals(arenaIdEntered)) {
                arenaManager.removeWaitingPlayer(player);
                MsgBuilder.send(Msg.ARENA_WAIT_STOP, player, arenaIdEntered);
            } else {
                arenaManager.addWaitingPlayer(player, arenaIdEntered);
            }
        }
        return true;
    }
}
