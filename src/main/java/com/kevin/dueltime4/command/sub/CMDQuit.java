package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.event.arena.ArenaTryToQuitEvent;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDQuit extends SubCommand {

    public CMDQuit() {
        super("quit", "q","leave","le");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
            return true;
        }
        Player player = (Player) sender;
        BaseArena arena = DuelTimePlugin.getInstance().getArenaManager().getOf(player);
        if (arena == null) {
            MsgBuilder.send(Msg.COMMAND_SUB_QUIT_NOT_IN_ARENA, player);
            return true;
        }
        // 發布事件，方便其他外掛編寫退出比賽的邏輯
        Bukkit.getServer().getPluginManager().callEvent(new ArenaTryToQuitEvent(player, arena));
        return true;
    }
}
