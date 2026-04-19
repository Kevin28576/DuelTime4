package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CMDAdminHelp extends SubCommand {
    private final UtilHelpList helpList;

    public CMDAdminHelp() {
        super("adminhelp", "ahelp", "ah", "adminhelps");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_ADMINHELP, false)
                .add("arena", new String[]{"arena", "a"}, "arena(a)", CommandPermission.ADMIN, Msg.COMMAND_SUB_ARENA_SERIES_DESCRIPTION, true)
                .add("shop", new String[]{"shop", "s"}, "shop(s) help(h)", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_HELP_DESCRIPTION,true)
                .add("point", new String[]{"point", "p", "points"}, "point(p)", CommandPermission.ADMIN, Msg.COMMAND_SUB_POINT_SERIES_DESCRIPTION, true)
                .add("level", new String[]{"level", "l", "lv"}, "level(l)", CommandPermission.ADMIN, Msg.COMMAND_SUB_LEVEL_SERIES_DESCRIPTION, true)
                .add("rank", new String[]{"rank", "r"}, "rank(r)", CommandPermission.ADMIN, Msg.COMMAND_SUB_RANK_SERIES_DESCRIPTION, true)
                .add("lobby", new String[]{"lobby", "l"}, "lobby(l) help(h)", CommandPermission.ADMIN, Msg.COMMAND_SUB_LOBBY_SERIES_DESCRIPTION, true)
                .add("blacklist", new String[]{"blacklist", "b", "blist", "bl"}, "blacklist(b)", CommandPermission.ADMIN, Msg.COMMAND_SUB_BLACKLIST_SERIES_DESCRIPTION, true)
                .add("stop", new String[]{"stop"}, "stop <arena_id|all> [%reason%]", CommandPermission.ADMIN, Msg.COMMAND_SUB_STOP_DESCRIPTION, true)
                .add("reload", new String[]{"reload", "rl"}, "reload(rl)", CommandPermission.ADMIN, Msg.COMMAND_SUB_RELOAD_DESCRIPTION, true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }
        helpList.send(sender, label,null);
        return true;
    }
}
