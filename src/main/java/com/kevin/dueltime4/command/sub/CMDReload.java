package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CMDReload extends SubCommand {

    public CMDReload() {
        super("reload", "rl");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }
        MsgBuilder.send(Msg.COMMAND_SUB_RELOAD_START, sender);
        long start = System.currentTimeMillis();
        DuelTimePlugin ins = DuelTimePlugin.getInstance();
        for (BaseArena arena : ins.getArenaManager().getList()) {
            if (arena.getState().equals(BaseArena.State.IN_PROGRESS_OPENED) || arena.getState().equals(BaseArena.State.IN_PROGRESS_CLOSED)) {
                ins.getArenaManager().stop(arena.getId(), null);
            }
        }
        DuelTimePlugin.getInstance().getHologramManager().disable();
        ins.getCfgManager().reload();
        ins.getMsgManager().check();
        ins.getMsgManager().reload();
        ins.getMyBatisManager().closeConnection();
        ins.getMyBatisManager().connectToDatabase();
        ins.getArenaManager().reload();
        ins.getCacheManager().reload();
        ins.getCacheManager().getPlayerDataCache().reloadRefreshRankingTimer();
        long end = System.currentTimeMillis();
        MsgBuilder.send(Msg.COMMAND_SUB_RELOAD_SUCCESSFULLY, sender,
                "" + (end - start));
        return true;
    }
}
