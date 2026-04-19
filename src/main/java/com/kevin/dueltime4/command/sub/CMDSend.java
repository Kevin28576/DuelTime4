package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.request.RequestData;
import com.kevin.dueltime4.request.RequestReceiver;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CMDSend extends SubCommand {

    public CMDSend() {
        super("send", "sd");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, commandSender);
            return true;
        }
        Player sender = (Player) commandSender;
        if (args.length < 2) {
            CMDHelp.helpList.sendCorrect(sender, 1, CMDHelp.helpList.getSubCommandById("send"), label, args);
            return true;
        }
        String senderName = sender.getName();
        String receiverName = args[1];
        if (DuelTimePlugin.getInstance().getCacheManager().getBlacklistCache().contains(senderName)) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_SELF_IN_BLACK_LIST, sender);
            return true;
        }
        if (senderName.equals(receiverName)) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_SEND_TO_SELF, sender);
            return true;
        }
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        if (arenaManager.getMap().isEmpty()) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_NO_ARENAS, sender);
            return true;
        }
        Player receiver = Bukkit.getPlayerExact(receiverName);
        if (receiver == null) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_OFFLINE, sender,
                    receiverName);
            return true;
        }
        if (DuelTimePlugin.getInstance().getCacheManager().getBlacklistCache().contains(receiverName)) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_RECIPIENT_IN_BLACK_LIST, sender,
                    receiverName);
            return true;
        }
        BaseArena designatedArena = null;
        if (args.length > 2) {
            String designatedArenaEditNameOrId = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
            designatedArena = findArenaByEditNameOrId(arenaManager, designatedArenaEditNameOrId);
            if (designatedArena == null) {
                MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_INVALID_ARENA_ID, sender,
                        designatedArenaEditNameOrId);
                if (args.length == 3) {
                    List<String> arenaEditNameCandidates = arenaManager.getList().stream()
                            .map(baseArena -> baseArena.getArenaData().getName())
                            .distinct()
                            .collect(Collectors.toList());
                    UtilHelpList.sendSuggest(sender, 2, arenaEditNameCandidates, label, args);
                }
                return true;
            }
            if (designatedArena.getState() == BaseArena.State.DISABLED) {
                MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_ARENA_DISABLED, sender,
                        designatedArena.getName());
                return true;
            }
        }
        RequestReceiver requestReceiver = DuelTimePlugin.getInstance().getRequestReceiverManager().get(receiverName);
        RequestData requestData = requestReceiver.get(senderName);
        if (requestData != null && System.currentTimeMillis() < requestData.getEndTime()) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_FAIL_FREQUENTLY, sender,
                    "" + (int) ((System.currentTimeMillis() - requestData.getStartTime()) / 1000), receiverName);
            return true;
        }
        requestReceiver.add(senderName, designatedArena != null ? designatedArena.getId() : null);
        MsgBuilder.send(Msg.COMMAND_SUB_SEND_SUCCESSFULLY, sender,
                receiverName);
        MsgBuilder.sendsClickable(Msg.COMMAND_SUB_SEND_RECEIVE, receiver,false,
                senderName);
        if (designatedArena != null) {
            MsgBuilder.send(Msg.COMMAND_SUB_SEND_RECEIVE_NOTIFY_DESIGNATED_ARENA, receiver, false,
                    designatedArena.getArenaData().getName(), DuelTimePlugin.getInstance().getArenaTypeManager().get(designatedArena.getArenaTypeId()).getName(receiver));
        }
        return true;
    }

    private BaseArena findArenaByEditNameOrId(ArenaManager arenaManager, String entered) {
        for (BaseArena arena : arenaManager.getList()) {
            if (arena.getArenaData().getName().equalsIgnoreCase(entered)) {
                return arena;
            }
        }
        return arenaManager.get(entered);
    }
}
