package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.request.RequestReceiver;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CMDDecline extends SubCommand {

    public CMDDecline() {
        super("decline", "dec", "deny");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, commandSender);
            return true;
        }
        Player receiver = (Player) commandSender;
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        if (arenaManager.declinePendingMatch(receiver)) {
            return true;
        }
        RequestReceiver requestReceiver = DuelTimePlugin.getInstance().getRequestReceiverManager().get(receiver.getName());
        List<String> validSenderNames = requestReceiver.getValidSenderNames();
        if (validSenderNames.isEmpty()) {
            MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_FAIL_NONE, receiver);
            return true;
        }
        if (args.length == 1) {
            if (validSenderNames.size() == 1) {
                // 如果有效的請求者只有一個，則直接拒絕該請求者的請求
                decline(receiver, requestReceiver, validSenderNames.get(0));
            } else {
                // 如果有效的請求者有多個，那麼提示一個列表，讓玩家選擇拒絕
                MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_CHOOSE_LIST_HEADING, receiver);
                int index = 0;
                for (String validSenderName : validSenderNames) {
                    index++;
                    MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_CHOOSE_LIST_BODY, receiver,
                            "" + index, validSenderName);
                }
                MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_CHOOSE_LIST_ENDING, receiver);
            }
            return true;
        }
        String senderNameEntered = args[1];
        if (!validSenderNames.contains(senderNameEntered)) {
            RequestReceiver.InvalidReason reason = requestReceiver.getInvalidReason(senderNameEntered);
            if (reason == RequestReceiver.InvalidReason.TIME_OUT) {
                MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_FAIL_TIME_OUT, receiver);
            }
            if (reason == RequestReceiver.InvalidReason.OFFLINE) {
                MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_FAIL_OFFLINE, receiver);
            }
            if (reason == RequestReceiver.InvalidReason.HAS_NOT_SENT) {
                MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_FAIL_HAS_NOT_SENT, receiver);
            }
            return true;
        }
        decline(receiver, requestReceiver, senderNameEntered);
        return true;
    }

    private void decline(Player receiver, RequestReceiver requestReceiver, String validSenderName) {
        requestReceiver.remove(validSenderName);
        MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_SUCCESS_RECEIVER, receiver, validSenderName);
        MsgBuilder.send(Msg.COMMAND_SUB_DECLINE_SUCCESS_SENDER, Bukkit.getPlayerExact(validSenderName), receiver.getName());
    }
}
