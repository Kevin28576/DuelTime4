package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDStart extends SubCommand {

    public CMDStart() {
        super("start", "st");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, commandSender);
            return true;
        }
        Player player = (Player) commandSender;
        if (DuelTimePlugin.getInstance().getCacheManager().getBlacklistCache().contains(player.getName())) {
            MsgBuilder.send(Msg.COMMAND_SUB_START_FAIL_SELF_IN_BLACK_LIST, player);
            return true;
        }
        DuelTimePlugin.getInstance().getCustomInventoryManager().getStart().openFor(player);
        return true;
    }
}
