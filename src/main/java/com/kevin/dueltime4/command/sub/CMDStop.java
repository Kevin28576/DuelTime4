package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
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

        String arenaIdEntered = args[1];
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        String reason = parseReason(args, 2);

        if (isStopAllKeyword(arenaIdEntered)) {
            arenaManager.cancelAllPendingMatches();
            int stopped = arenaManager.stopAllInProgress(reason != null ? reason : "ADMIN_FORCE_STOP_ALL");
            if (stopped <= 0) {
                DynamicLang.send(sender, true,
                        "Dynamic.stop-all.no-game",
                        "&e目前沒有正在進行中的競技場比賽。");
                return true;
            }
            DynamicLang.send(sender, true,
                    "Dynamic.stop-all.success",
                    "&a已強制中止 &f{count} &a個競技場比賽。",
                    "count", String.valueOf(stopped));
            return true;
        }

        BaseArena arena = arenaManager.get(arenaIdEntered);
        if (arena == null) {
            MsgBuilder.send(Msg.COMMAND_SUB_STOP_FAIL_ARENA_NOT_EXISTS, sender, arenaIdEntered);
            UtilHelpList.sendSuggest(sender, 1,
                    arenaManager.getList().stream().map(BaseArena::getId).collect(Collectors.toList()), label, args);
            return true;
        }
        if (arena.getState() != BaseArena.State.IN_PROGRESS_OPENED && arena.getState() != BaseArena.State.IN_PROGRESS_CLOSED) {
            MsgBuilder.send(Msg.COMMAND_SUB_STOP_FAIL_NO_GAME, sender, arenaIdEntered);
            return true;
        }

        MsgBuilder.send(Msg.COMMAND_SUB_STOP_TENTATIVELY, sender,
                DuelTimePlugin.getInstance().getArenaTypeManager().get(arena.getArenaTypeId()).getName(sender),
                arenaIdEntered);
        arenaManager.stop(arenaIdEntered, reason);
        return true;
    }

    private boolean isStopAllKeyword(String entered) {
        return "all".equalsIgnoreCase(entered) || "*".equals(entered);
    }

    private String parseReason(String[] args, int fromIndex) {
        if (args.length <= fromIndex) {
            return null;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, fromIndex, args.length)).trim();
        return reason.isEmpty() ? null : reason;
    }
}
