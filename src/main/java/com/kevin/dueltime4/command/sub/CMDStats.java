package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.level.Tier;
import com.kevin.dueltime4.stats.MatchStreakManager;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CMDStats extends SubCommand {

    public CMDStats() {
        super("stats", "stat");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        String targetName;

        if (args.length >= 2) {
            targetName = args[1];
        } else {
            if (!(sender instanceof Player)) {
                DynamicLang.send(sender, true,
                        "Dynamic.stats.usage",
                        "&cUsage: /{label} stats <player>",
                        "label", label);
                return true;
            }
            targetName = sender.getName();
        }

        PlayerDataCache playerDataCache = plugin.getCacheManager().getPlayerDataCache();
        PlayerData playerData = playerDataCache.getAnyway(targetName);
        if (playerData == null) {
            MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender, targetName);
            return true;
        }

        LevelManager levelManager = plugin.getLevelManager();
        int level = levelManager.getLevel(targetName, playerData.getExp());
        Tier tier = levelManager.getTier(targetName, playerData.getExp());

        int classicWins = playerData.getArenaClassicWins();
        int classicDraws = playerData.getArenaClassicDraws();
        int classicLoses = playerData.getArenaClassicLoses();
        int classicTotal = classicWins + classicDraws + classicLoses;
        double winRate = classicTotal == 0 ? 0D : (classicWins * 100D / classicTotal);

        MatchStreakManager streakManager = plugin.getMatchStreakManager();
        String streak = streakManager == null ? "-" : streakManager.getDisplay(targetName);
        long queuePenalty = plugin.getArenaManager().getQueuePenaltyRemainingSeconds(targetName);

        int recordCount = 0;
        List<BaseRecordData> records = plugin.getCacheManager().getArenaRecordCache().getAnyway(targetName);
        if (records != null) {
            recordCount = records.size();
        }

        DynamicLang.send(sender, true,
                "Dynamic.stats.separator",
                "&7&m---------------------------");
        DynamicLang.send(sender, true,
                "Dynamic.stats.title",
                "&aStats &7- &f{player}",
                "player", targetName);
        DynamicLang.send(sender, true,
                "Dynamic.stats.level-tier",
                "&7Level: &f{level} &7| Tier: {tier}",
                "level", String.valueOf(level),
                "tier", tier.getTitle());
        DynamicLang.send(sender, true,
                "Dynamic.stats.exp-point",
                "&7Exp: &f{exp} &7| Point: &f{point}",
                "exp", String.valueOf(UtilFormat.round(playerData.getExp(), 2)),
                "point", String.valueOf(UtilFormat.round(playerData.getPoint(), 2)));
        DynamicLang.send(sender, true,
                "Dynamic.stats.classic",
                "&7Classic: &aW {win} &eD {draw} &cL {lose} &7| WR: &f{wr}%",
                "win", String.valueOf(classicWins),
                "draw", String.valueOf(classicDraws),
                "lose", String.valueOf(classicLoses),
                "wr", String.valueOf(UtilFormat.round(winRate, 1)));
        DynamicLang.send(sender, true,
                "Dynamic.stats.streak-records",
                "&7Streak: &f{streak} &7| Records: &f{records}",
                "streak", streak,
                "records", String.valueOf(recordCount));
        DynamicLang.send(sender, true,
                "Dynamic.stats.total",
                "&7Total Games: &f{games} &7| Total Time: &f{time}",
                "games", String.valueOf(playerData.getTotalGameNumber()),
                "time", formatDuration(sender, playerData.getTotalGameTime()));
        if (queuePenalty > 0) {
            DynamicLang.send(sender, true,
                    "Dynamic.stats.queue-cooldown",
                    "&7Queue Cooldown: &e{seconds}s",
                    "seconds", String.valueOf(queuePenalty));
        }
        DynamicLang.send(sender, true,
                "Dynamic.stats.separator",
                "&7&m---------------------------");
        return true;
    }

    private String formatDuration(CommandSender sender, int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int leftSeconds = seconds % 60;

        String hourSuffix = DynamicLang.get(sender, "Dynamic.stats.duration-hour", "h");
        String minuteSuffix = DynamicLang.get(sender, "Dynamic.stats.duration-minute", "m");
        String secondSuffix = DynamicLang.get(sender, "Dynamic.stats.duration-second", "s");

        if (hours > 0) {
            return hours + hourSuffix + " " + minutes + minuteSuffix + " " + leftSeconds + secondSuffix;
        }
        if (minutes > 0) {
            return minutes + minuteSuffix + " " + leftSeconds + secondSuffix;
        }
        return leftSeconds + secondSuffix;
    }
}
