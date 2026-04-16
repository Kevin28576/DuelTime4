package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.BlacklistCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CMDBlacklist extends SubCommand {
    private final UtilHelpList helpList;

    public CMDBlacklist() {
        super("blacklist", "b", "bl", "blist");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_BLACKLIST, true)
                .add("add", new String[]{"add", "a"}, "add(a) <%player%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_BLACKLIST_ADD_DESCRIPTION)
                .add("remove", new String[]{"remove", "r"}, "remove(r) <%player%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_BLACKLIST_REMOVE_DESCRIPTION)
                .add("view", new String[]{"view", "v"}, "view(v)", CommandPermission.ADMIN, Msg.COMMAND_SUB_BLACKLIST_VIEW_DESCRIPTION);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }
        if (args.length == 1) {
            helpList.send(sender, label, args[0]);
            return true;
        }
        BlacklistCache cache = DuelTimePlugin.getInstance().getCacheManager().getBlacklistCache();
        String commandEntered = args[1];
        UtilHelpList.SingleCommand singleCommand = helpList.getSubCommandByEnter(commandEntered);
        if (singleCommand == null) {
            helpList.send(sender, label, args[0]);
            helpList.sendSuggest(sender, label, args);
            return true;
        }
        String singleCommandId = singleCommand.getId();
        if (singleCommandId.equals("add") || singleCommandId.equals("remove")) {
            if (args.length < 3) {
                helpList.sendCorrect(sender, -1, args[1], label, args);
                return true;
            }
            String targetPlayerName = args[2];
            if (singleCommandId.equals("add")) {
                if (!Bukkit.getOfflinePlayer(targetPlayerName).hasPlayedBefore()) {
                    helpList.sendCorrect(sender, 2, args[1], label, args);
                    MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender,
                            targetPlayerName);
                    return true;
                }
                if (cache.contains(targetPlayerName)) {
                    MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_THE_PLAYER_HAS_BEEN_IN, sender,
                            targetPlayerName);
                    return true;
                }
                cache.add(targetPlayerName);
                MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_ADD_SUCCESSFULLY, sender,
                        targetPlayerName);
            } else {
                if (!cache.contains(targetPlayerName)) {
                    MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_THE_PLAYER_NOT_FOUND, sender,
                            targetPlayerName);
                    return true;
                }
                cache.remove(targetPlayerName);
                MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_REMOVE_SUCCESSFULLY, sender,
                        targetPlayerName);
            }
            return true;
        }
        if (singleCommandId.equals("view")) {
            List<String> blacklist = cache.get();
            if (blacklist.isEmpty()) {
                MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_VIEW_EMPTY, sender);
                return true;
            }
            MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_VIEW_HEADING, sender, false);
            int index = 0;
            for (String playerNameInBlacklist : blacklist) {
                index++;
                MsgBuilder.sendClickable(Msg.COMMAND_SUB_BLACKLIST_VIEW_BODY, sender, false,
                        "" + index, playerNameInBlacklist);
            }
            MsgBuilder.send(Msg.COMMAND_SUB_BLACKLIST_VIEW_ENDING, sender, false,
                    "" + index);
            return true;
        }
        return true;
    }
}
