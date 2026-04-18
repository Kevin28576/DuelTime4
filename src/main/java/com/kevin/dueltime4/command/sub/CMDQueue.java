package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.QueueMatchConfirmManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CMDQueue extends SubCommand {
    public CMDQueue() {
        super("queue", "q");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }

        if (args.length < 2 || !isAlias(args[1], "debug", "d", "status", "s")) {
            sendUsage(sender, label);
            return true;
        }

        sendDebugPanel(sender);
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        DynamicLang.send(sender, true,
                "Dynamic.queue-debug.usage",
                "&cUsage: /{label} queue debug",
                "label", label);
    }

    private void sendDebugPanel(CommandSender sender) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        ArenaManager arenaManager = plugin.getArenaManager();
        QueueMatchConfirmManager confirmManager = arenaManager.getQueueMatchConfirmManager();
        Map<String, String> waitingMap = arenaManager.getWaitingPlayerToArenaMap();

        int waitingTotal = waitingMap.size();
        int waitingArenaCount = (int) waitingMap.values().stream().distinct().count();
        int pendingArenaCount = confirmManager.getPendingArenaCount();

        DynamicLang.send(sender, true,
                "Dynamic.queue-debug.separator",
                "&8&m---------------------------");
        DynamicLang.send(sender, true,
                "Dynamic.queue-debug.title",
                "&aQueue Debug Panel");
        DynamicLang.send(sender, true,
                "Dynamic.queue-debug.summary",
                "&7Waiting: &f{waiting} &7| Arenas: &f{arenas} &7| Pending Confirm: &f{pending}",
                "waiting", String.valueOf(waitingTotal),
                "arenas", String.valueOf(waitingArenaCount),
                "pending", String.valueOf(pendingArenaCount));

        List<BaseArena> arenas = new ArrayList<>(arenaManager.getList());
        arenas.sort(Comparator.comparing(BaseArena::getName, String.CASE_INSENSITIVE_ORDER));
        boolean hasArenaLine = false;
        for (BaseArena arena : arenas) {
            String arenaId = arena.getId();
            int waitingCount = arenaManager.getWaitingPlayers(arenaId).size();
            boolean hasPending = confirmManager.hasPendingForArena(arenaId);
            if (waitingCount == 0 && !hasPending) {
                continue;
            }

            hasArenaLine = true;
            int acceptedCount = confirmManager.getAcceptedCount(arenaId);
            int requiredCount = confirmManager.getRequiredCount(arenaId);
            long eta = arenaManager.getEstimatedQueueWaitSeconds(arenaId);
            String confirmState = hasPending ? (acceptedCount + "/" + requiredCount) : "-";
            DynamicLang.send(sender, true,
                    "Dynamic.queue-debug.arena-line",
                    "&7- &f{arena} &8({id}) &7| waiting: &f{waiting} &7| eta: &f{eta}s &7| confirm: &f{confirm}",
                    "arena", arena.getName(),
                    "id", arenaId,
                    "waiting", String.valueOf(waitingCount),
                    "eta", String.valueOf(eta),
                    "confirm", confirmState);
        }
        if (!hasArenaLine) {
            DynamicLang.send(sender, true,
                    "Dynamic.queue-debug.no-arena",
                    "&7No active queue arenas at this moment.");
        }

        if (waitingMap.isEmpty()) {
            DynamicLang.send(sender, true,
                    "Dynamic.queue-debug.no-player",
                    "&7No players currently waiting.");
        } else {
            List<String> playerNames = new ArrayList<>(waitingMap.keySet());
            playerNames.sort(String.CASE_INSENSITIVE_ORDER);
            for (String playerName : playerNames) {
                String arenaId = waitingMap.get(playerName);
                BaseArena arena = arenaManager.get(arenaId);
                String arenaName = arena != null ? arena.getName() : arenaId;
                long waited = arenaManager.getWaitingSeconds(playerName);
                long remaining = arenaManager.getEstimatedQueueRemainingSeconds(playerName, arenaId);
                long cooldown = arenaManager.getQueuePenaltyRemainingSeconds(playerName);
                DynamicLang.send(sender, true,
                        "Dynamic.queue-debug.player-line",
                        "&8  * &f{player} &7@ &f{arena} &8({id}) &7| waited: &f{waited}s &7| eta: &f{eta}s &7| cooldown: &f{cooldown}s",
                        "player", playerName,
                        "arena", arenaName,
                        "id", arenaId,
                        "waited", String.valueOf(waited),
                        "eta", String.valueOf(remaining),
                        "cooldown", String.valueOf(cooldown));
            }
        }

        DynamicLang.send(sender, true,
                "Dynamic.queue-debug.separator",
                "&8&m---------------------------");
    }

    private boolean isAlias(String entered, String... aliases) {
        String normalized = entered.toLowerCase(Locale.ROOT);
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}
