package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDPoint extends SubCommand {
    private final UtilHelpList helpList;

    public CMDPoint() {
        super("point", "p", "points");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_POINT, true)
                .add("me", new String[]{"me"}, "me", null, Msg.COMMAND_SUB_POINT_VIEW_SELF_DESCRIPTION)
                .add("add", new String[]{"add", "a","give","g"}, "add(a) <%player%> <%value%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_POINT_ADD_DESCRIPTION)
                .add("set", new String[]{"set", "s"}, "set(s) <%player%> <%value%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_POINT_SET_DESCRIPTION)
                .add("view", new String[]{"view", "v"}, "view(v) <%player%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_POINT_VIEW_PLAYER_DESCRIPTION);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            helpList.send(sender, label, args[0]);
            return true;
        }
        PlayerDataCache cache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
        String commandEntered = args[1];
        UtilHelpList.SingleCommand singleCommand = helpList.getSubCommandByEnter(commandEntered);
        if (singleCommand == null) {
            helpList.send(sender, label, args[0]);
            helpList.sendSuggest(sender, label, args);
            return true;
        }
        String singleCommandId = singleCommand.getId();
        if (singleCommandId.equals("me")) {
            if (!(sender instanceof Player)) {
                MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
                return true;
            }
            double point = cache.get(sender.getName()).getPoint();
            MsgBuilder.send(Msg.COMMAND_SUB_POINT_VIEW_SELF, sender,
                    "" + point);
            return true;
        }
        if (singleCommandId.equals("add") || singleCommandId.equals("set")) {
            if (!singleCommand.judgePermission(sender)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            if (args.length < 4) {
                helpList.sendCorrect(sender, -1, commandEntered, label, args);
                return true;
            }
            String targetPlayerName = args[2];
            PlayerData playerData = cache.getAnyway(targetPlayerName);
            if (playerData == null) {
                helpList.sendCorrect(sender, 2, commandEntered, label, args);
                MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender,
                        targetPlayerName);
                return true;
            }
            String pointEnteredString = args[3];
            if (!UtilFormat.isDouble(pointEnteredString)) {
                helpList.sendCorrect(sender, 3, commandEntered, label, args);
                MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender, pointEnteredString);
                return true;
            }
            double pointEntered = Double.parseDouble(pointEnteredString);
            if (singleCommand.getId().equals("add")) {
                // 操作為新增
                if (pointEntered <= 0) {
                    helpList.sendCorrect(sender, 3, commandEntered, label, args);
                    MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, sender, pointEnteredString);
                    return true;
                }
                double pointTotal = playerData.getPoint() + pointEntered;
                playerData.setPoint(pointTotal);
                MsgBuilder.send(Msg.COMMAND_SUB_POINT_ADD_SUCCESSFULLY, sender,
                        targetPlayerName, "" + pointTotal);
            } else {
                // 操作為設定
                if (pointEntered < 0) {
                    helpList.sendCorrect(sender, 3, commandEntered, label, args);
                    MsgBuilder.send(Msg.ERROR_VALUE_IS_NEGATIVE, sender, pointEnteredString);
                    return true;
                }
                playerData.setPoint(pointEntered);
                MsgBuilder.send(Msg.COMMAND_SUB_POINT_SET_SUCCESSFULLY, sender,
                        targetPlayerName, "" + pointEntered);
            }
            // 完成快取變更
            cache.set(targetPlayerName, playerData);
            return true;
        }
        if (singleCommandId.equals("view")) {
            if (!singleCommand.judgePermission(sender)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            if (args.length < 3) {
                helpList.sendCorrect(sender, -1, commandEntered, label, args);
                return true;
            }
            String targetPlayerName = args[2];
            PlayerData playerData = cache.getAnyway(targetPlayerName);
            if (playerData == null) {
                helpList.sendCorrect(sender, 2, commandEntered, label, args);
                MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender,
                        targetPlayerName);
                return true;
            }
            double point = playerData.getPoint();
            MsgBuilder.send(Msg.COMMAND_SUB_POINT_VIEW_PLAYER, sender,
                    targetPlayerName, "" + point);
            return true;
        }
        return true;
    }
}
