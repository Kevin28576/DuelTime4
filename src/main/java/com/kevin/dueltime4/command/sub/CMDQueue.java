package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.QueueMatchConfirmManager;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        if (args.length < 2) {
            sendUsage(sender, label);
            return true;
        }

        String operation = args[1].toLowerCase(Locale.ROOT);
        if (isAlias(operation, "debug", "d")) {
            if (!sender.hasPermission(CommandPermission.ADMIN)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            sendDebugPanel(sender);
            return true;
        }
        if (isAlias(operation, "sound", "notify", "reminder")) {
            handleSound(sender, label, args);
            return true;
        }
        if (isAlias(operation, "cooldown", "cd", "penalty")) {
            handleCooldown(sender, label, args);
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private void handleSound(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
            return;
        }

        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        CfgManager cfgManager = plugin.getCfgManager();
        if (!cfgManager.isArenaClassicQueueSoundEnabled()) {
            DynamicLang.send(player, true,
                    "Dynamic.queue.sound.global-disabled",
                    "&c伺服器目前已關閉排隊提醒音效。");
            return;
        }
        if (!cfgManager.isArenaClassicQueueSoundAllowPlayerToggle()) {
            DynamicLang.send(player, true,
                    "Dynamic.queue.sound.player-toggle-disabled",
                    "&e伺服器目前不允許玩家自行切換排隊提醒音效。");
            sendSoundStatus(player);
            return;
        }

        if (args.length < 3 || isAlias(args[2], "status", "s")) {
            sendSoundStatus(player);
            return;
        }

        PlayerDataCache playerDataCache = plugin.getCacheManager().getPlayerDataCache();
        PlayerData playerData = playerDataCache.getAnyway(player.getName());
        if (playerData == null) {
            DynamicLang.send(player, true,
                    "Dynamic.queue.sound.load-failed",
                    "&c無法讀取你的玩家設定，請稍後再試。");
            return;
        }

        Boolean targetState = parseBooleanLike(args[2]);
        if (targetState == null && isAlias(args[2], "toggle", "t")) {
            targetState = !playerData.isQueueSoundEnabled();
        }
        if (targetState == null) {
            DynamicLang.send(player, true,
                    "Dynamic.queue.sound.usage",
                    "&c用法：/{label} queue sound [on|off|toggle|status]",
                    "label", label);
            return;
        }

        playerData.setQueueSoundEnabled(targetState);
        playerDataCache.set(player.getName(), playerData);
        DynamicLang.send(player, true,
                targetState ? "Dynamic.queue.sound.updated-on" : "Dynamic.queue.sound.updated-off",
                targetState
                        ? "&a排隊提醒音效已開啟。"
                        : "&e排隊提醒音效已關閉。");
    }

    private void sendSoundStatus(Player player) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        CfgManager cfgManager = plugin.getCfgManager();
        PlayerData playerData = plugin.getCacheManager().getPlayerDataCache().getAnyway(player.getName());
        boolean playerEnabled = playerData == null || playerData.isQueueSoundEnabled();
        boolean globalEnabled = cfgManager.isArenaClassicQueueSoundEnabled();
        String globalState = DynamicLang.get(player,
                globalEnabled ? "Dynamic.queue.sound.state-on" : "Dynamic.queue.sound.state-off",
                globalEnabled ? "&a開啟" : "&c關閉");
        String playerState = DynamicLang.get(player,
                playerEnabled ? "Dynamic.queue.sound.state-on" : "Dynamic.queue.sound.state-off",
                playerEnabled ? "&a開啟" : "&c關閉");
        DynamicLang.send(player, true,
                "Dynamic.queue.sound.status",
                "&7排隊提醒音效：全域 {global} &8| &7個人 {player}",
                "global", globalState,
                "player", playerState);
    }

    private void handleCooldown(CommandSender sender, String label, String[] args) {
        String targetName;
        if (args.length >= 3) {
            targetName = args[2];
            if (!(sender instanceof Player player) || !player.getName().equalsIgnoreCase(targetName)) {
                if (!sender.hasPermission(CommandPermission.ADMIN)) {
                    MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                DynamicLang.send(sender, true,
                        "Dynamic.queue.cooldown.usage",
                        "&c用法：/{label} queue cooldown [player]",
                        "label", label);
                return;
            }
            targetName = sender.getName();
        }

        long remaining = DuelTimePlugin.getInstance().getArenaManager().getQueuePenaltyRemainingSeconds(targetName);
        boolean self = sender.getName().equalsIgnoreCase(targetName);
        if (remaining > 0) {
            DynamicLang.send(sender, true,
                    self ? "Dynamic.queue.cooldown.active" : "Dynamic.queue.cooldown.active-other",
                    self
                            ? "&e你的離場冷卻剩餘 &f{seconds} &e秒。"
                            : "&e玩家 &f{player} &e離場冷卻剩餘 &f{seconds} &e秒。",
                    "player", targetName,
                    "seconds", String.valueOf(remaining));
            return;
        }
        DynamicLang.send(sender, true,
                self ? "Dynamic.queue.cooldown.none" : "Dynamic.queue.cooldown.none-other",
                self
                        ? "&a你目前沒有離場冷卻。"
                        : "&a玩家 &f{player} &a目前沒有離場冷卻。",
                "player", targetName);
    }

    private void sendUsage(CommandSender sender, String label) {
        DynamicLang.send(sender, true,
                "Dynamic.queue.usage-main",
                "&c用法：/{label} queue <debug|sound|cooldown>",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.queue.usage-sound",
                "&7 - /{label} queue sound [on|off|toggle|status]",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.queue.usage-cooldown",
                "&7 - /{label} queue cooldown [player]",
                "label", label);
        if (sender.hasPermission(CommandPermission.ADMIN)) {
            DynamicLang.send(sender, true,
                    "Dynamic.queue.usage-debug",
                    "&7 - /{label} queue debug",
                    "label", label);
        }
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

    private Boolean parseBooleanLike(String raw) {
        String normalized = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "t", "yes", "y", "1", "on", "enable", "enabled" -> true;
            case "false", "f", "no", "n", "0", "off", "disable", "disabled" -> false;
            default -> null;
        };
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

