package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class CMDStop extends SubCommand {

    public CMDStop() {
        super("stop");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }
        if (args.length < 2) {
            CMDHelp.helpList.sendCorrect(sender, 1, CMDHelp.helpList.getSubCommandById("stop"), label, args);
            return true;
        }
        Player player = (Player) sender;
        String arenaIdEntered = args[1];
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        BaseArena arena = arenaManager.get(arenaIdEntered);
        if (arena == null) {
            MsgBuilder.send(Msg.COMMAND_SUB_STOP_FAIL_ARENA_NOT_EXISTS, player,
                    arenaIdEntered);
            UtilHelpList.sendSuggest(sender, 1,
                    arenaManager.getList().stream().map(BaseArena::getId).collect(Collectors.toList()), label, args);
            return true;
        }
        if (arena.getState() != BaseArena.State.IN_PROGRESS_OPENED && arena.getState() != BaseArena.State.IN_PROGRESS_CLOSED) {
            MsgBuilder.send(Msg.COMMAND_SUB_STOP_FAIL_NO_GAME, player,
                    arenaIdEntered);
            return true;
        }
        MsgBuilder.send(Msg.COMMAND_SUB_STOP_TENTATIVELY, player,
                DuelTimePlugin.getInstance().getArenaTypeManager().get(arena.getArenaTypeId()).getName(sender),
                arenaIdEntered);
        String reason = args.length > 2 ? args[2] : null;
        // 發布事件，方便其他外掛編寫關停比賽的邏輯
        DuelTimePlugin.getInstance().getArenaManager().stop(arenaIdEntered, reason);
        return true;
    }
}
