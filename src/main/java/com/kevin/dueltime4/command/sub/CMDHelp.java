package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CMDHelp extends SubCommand {
    protected static UtilHelpList helpList;

    public CMDHelp() {
        super("help", "h", "helps");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_HELP, false)
                .add("arena", new String[]{"arena", "a"}, "arena(a)", null, Msg.COMMAND_SUB_ARENA_SERIES_DESCRIPTION, true)
                .add("point", new String[]{"point", "p", "points"}, "point(p)", null, Msg.COMMAND_SUB_POINT_SERIES_DESCRIPTION, true)
                .add("level", new String[]{"level", "lv"}, "level(l)", null, Msg.COMMAND_SUB_LEVEL_SERIES_DESCRIPTION, true)
                .add("rank", new String[]{"rank", "rk"}, "rank(rk)", null, Msg.COMMAND_SUB_RANK_SERIES_DESCRIPTION, true)
                .add("start", new String[]{"start", "st"}, "start(st)", null, Msg.COMMAND_SUB_START_DESCRIPTION)
                .add("send", new String[]{"send", "sd"}, "send(sd) <%player%> [%arena_edit_name%]", null, Msg.COMMAND_SUB_SEND_DESCRIPTION)
                .add("accept", new String[]{"accept", "acc"}, "accept(acc) [%player%]", null, Msg.COMMAND_SUB_ACCEPT_DESCRIPTION)
                .add("decline", new String[]{"decline", "dec","deny"}, "decline(dec) [%player%]", null, Msg.COMMAND_SUB_DECLINE_DESCRIPTION)
                .add("join", new String[]{"join", "j"}, "join(j) <%arena_id%>", null, Msg.COMMAND_SUB_JOIN_DESCRIPTION)
                .add("shop", new String[]{"shop", "s"}, "shop(s)", null, Msg.COMMAND_SUB_SHOP_DESCRIPTION)
                .add("lobby", new String[]{"lobby", "l"}, "lobby(l)", null, Msg.COMMAND_SUB_LOBBY_DESCRIPTION)
                .add("spectate", new String[]{"spectate", "sp", "watch", "w"}, "spectate(sp) <%arena_id%>", null, Msg.COMMAND_SUB_SPECTATE_DESCRIPTION)
                .add("quit", new String[]{"quit", "q", "leave", "le"}, "quit(q)", null, Msg.COMMAND_SUB_QUIT_DESCRIPTION)
                .add("record", new String[]{"record", "r", "records"}, "record(r)", null, Msg.COMMAND_SUB_RECORD_DESCRIPTION)
                .add("lang", new String[]{"lang", "language", "languages"}, "lang [%language_file_name%]", null, Msg.COMMAND_SUB_LANG_DESCRIPTION);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        helpList.send(sender, label, null);
        return true;
    }
}
