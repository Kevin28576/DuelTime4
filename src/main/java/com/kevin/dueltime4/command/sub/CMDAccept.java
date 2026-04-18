package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.request.RequestReceiver;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CMDAccept extends SubCommand {

    public CMDAccept() {
        super("accept", "acc");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, commandSender);
            return true;
        }
        Player receiver = (Player) commandSender;
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        if (arenaManager.acceptPendingMatch(receiver)) {
            return true;
        }
        RequestReceiver requestReceiver = DuelTimePlugin.getInstance().getRequestReceiverManager().get(receiver.getName());
        List<String> validSenderNames = requestReceiver.getValidSenderNames();
        if (validSenderNames.isEmpty()) {
            MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_NONE, receiver);
            return true;
        }
        if (args.length == 1) {
            if (validSenderNames.size() == 1) {
                // 如果有效的請求者只有一個，則直接接受該請求者的請求
                accept(receiver, requestReceiver, validSenderNames.get(0));
            } else {
                // 如果有效的請求者有多個，那麼提示一個列表，讓玩家選擇接受
                MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_CHOOSE_LIST_HEADING, receiver);
                int index = 0;
                for (String validSenderName : validSenderNames) {
                    index++;
                    String arenaId = requestReceiver.get(validSenderName).getData();
                    String arenaDisplayName = arenaManager.get(arenaId).getArenaData().getName();
                    MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_CHOOSE_LIST_BODY, receiver,
                            "" + index, arenaDisplayName, validSenderName);
                }
                MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_CHOOSE_LIST_ENDING, receiver);
            }
            return true;
        }
        // 如果輸入了玩家名引數，則確定該玩家是否為有效的請求者
        String senderNameEntered = args[1];
        if (!validSenderNames.contains(senderNameEntered)) {
            RequestReceiver.InvalidReason reason = requestReceiver.getInvalidReason(senderNameEntered);
            if (reason == RequestReceiver.InvalidReason.TIME_OUT) {
                MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_TIME_OUT, receiver);
            }
            if (reason == RequestReceiver.InvalidReason.OFFLINE) {
                MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_OFFLINE, receiver);
            }
            if (reason == RequestReceiver.InvalidReason.HAS_NOT_SENT) {
                MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_HAS_NOT_SENT, receiver);
            }
            return true;
        }
        accept(receiver, requestReceiver, senderNameEntered);
        return true;
    }

    private void accept(Player receiver, RequestReceiver requestReceiver, String validSenderName) {
        MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_SUCCESS_RECEIVER, receiver,
                validSenderName);
        MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_SUCCESS_SENDER, Bukkit.getPlayerExact(validSenderName),
                receiver.getName());
        String designatedArenaId = requestReceiver.get(validSenderName).getData();
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena;
        if (designatedArenaId != null) {
            arena = arenaManager.get(designatedArenaId);
        } else {
            List<BaseArena> arenaList = new ArrayList<>(arenaManager.getMap().values());
            if (arenaList.isEmpty()) {
                MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_NO_ARENAS, receiver);
                return;
            }
            List<BaseArena> availableArenaList = new ArrayList<>();
            boolean isNeedToWait = false;
            for (BaseArena baseArena : arenaList) {
                if (baseArena.getArenaData().getMinPlayerNumber() <= 2) {
                    isNeedToWait = true;
                    if (baseArena.getState() == BaseArena.State.WAITING) {
                        availableArenaList.add(baseArena);
                    }
                }
            }
            if (availableArenaList.isEmpty()) {
                if (isNeedToWait) {
                    MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_NO_AVAILABLE_ARENAS_TEMPORARILY, receiver);
                } else {
                    MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_FAIL_NO_AVAILABLE_ARENAS, receiver);
                }
                return;
            }
            arena = availableArenaList.get(new Random().nextInt(availableArenaList.size()));
            MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_SUCCESS_NOTIFY_RANDOMLY_CHOSEN_ARENA, Bukkit.getPlayerExact(validSenderName),
                    arena.getArenaData().getName(),
                    DuelTimePlugin.getInstance().getArenaTypeManager().get(arena.getArenaTypeId()).getName(Bukkit.getPlayerExact(validSenderName)));
            MsgBuilder.send(Msg.COMMAND_SUB_ACCEPT_SUCCESS_NOTIFY_RANDOMLY_CHOSEN_ARENA, receiver,
                    arena.getArenaData().getName(),
                    DuelTimePlugin.getInstance().getArenaTypeManager().get(arena.getArenaTypeId()).getName(receiver));
        }
        requestReceiver.clear();
        arenaManager.start(arena.getId(),null, receiver, Bukkit.getPlayerExact(validSenderName));
    }
}
