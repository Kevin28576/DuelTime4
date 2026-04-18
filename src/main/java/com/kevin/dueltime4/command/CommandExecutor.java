package com.kevin.dueltime4.command;


import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.cache.BlacklistCache;
import com.kevin.dueltime4.cache.ShopCache;
import com.kevin.dueltime4.command.sub.CMDBalance;
import com.kevin.dueltime4.command.sub.CommandPermission;
import com.kevin.dueltime4.data.pojo.ShopRewardData;
import com.kevin.dueltime4.request.RequestReceiver;
import com.kevin.dueltime4.util.UtilSimilarityComparer;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CommandExecutor implements TabExecutor {
    private static final Set<String> ADMIN_ONLY_ROOT_COMMANDS = new HashSet<>(Arrays.asList(
            "adminhelp",
            "balance",
            "blacklist",
            "reload",
            "stop",
            "update"
    ));
    private static final List<String> ROOT_COMMANDS = Arrays.asList(
            "accept",
            "adminhelp",
            "arena",
            "balance",
            "blacklist",
            "decline",
            "help",
            "join",
            "language",
            "level",
            "lobby",
            "point",
            "quit",
            "rank",
            "record",
            "reload",
            "send",
            "shop",
            "spectate",
            "start",
            "stats",
            "stop",
            "update"
    );

    public CommandExecutor(Set<SubCommand> commands) {
    }

    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            return CMDMain.onCommand(sender, label);
        } else {
            SubCommand subCommand = DuelTimePlugin.getInstance().getCommandHandler()
                    .getSubCommand(args[0]);
            if (subCommand == null) {
                MsgBuilder.send(Msg.ERROR_SUB_COMMAND_NOT_EXISTS, sender,
                        args[0]);
                String mostSimilarSubCommand = UtilSimilarityComparer.getMostSimilar(args[0], getRootSubCommandCandidates(sender));
                if (mostSimilarSubCommand != null) {
                    String commandSuggested = "§2/" + label + " §a§n" + mostSimilarSubCommand + "§r";
                    MsgBuilder.sendClickable(Msg.COMMAND_SUGGEST, sender, false, commandSuggested);
                }
                return true;
            }
            return subCommand.onCommand(sender, command, label, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return complete(args[0], getRootSubCommandCandidates(sender));
        }

        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getCommandHandler() == null) {
            return Collections.emptyList();
        }
        SubCommand subCommand = plugin.getCommandHandler().getSubCommand(args[0]);
        if (subCommand == null) {
            return Collections.emptyList();
        }

        String root = subCommand.getAliases()[0].toLowerCase(Locale.ROOT);
        switch (root) {
            case "help":
                return tabHelp(args);
            case "adminhelp":
                return tabAdminHelp(sender, args);
            case "arena":
                return tabArena(sender, args);
            case "blacklist":
                return tabBlacklist(sender, args);
            case "accept":
            case "decline":
                return tabAcceptOrDecline(sender, args);
            case "lang":
                return tabLang(args);
            case "level":
                return tabLevel(sender, args);
            case "point":
                return tabPoint(sender, args);
            case "send":
                return tabSend(sender, args);
            case "shop":
                return tabShop(sender, args);
            case "lobby":
                return tabLobby(sender, args);
            case "rank":
                return tabRank(sender, args);
            case "stats":
                return tabStats(sender, args);
            case "balance":
                return tabBalance(sender, args);
            case "update":
                return tabUpdate(sender, args);
            case "join":
            case "spectate":
                return tabArenaId(args, 1);
            case "stop":
                return sender.hasPermission(CommandPermission.ADMIN) ? tabArenaId(args, 1) : Collections.emptyList();
            case "click":
                return args.length == 2 ? complete(args[1], Collections.singletonList("item")) : Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    private List<String> tabHelp(String[] args) {
        if (args.length != 2) {
            return Collections.emptyList();
        }
        return complete(args[1], Arrays.asList(
                "arena", "point", "level", "rank",
                "start", "send", "accept", "decline",
                "join", "shop", "lobby", "spectate",
                "quit", "record", "language"
        ));
    }

    private List<String> tabAdminHelp(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN) || args.length != 2) {
            return Collections.emptyList();
        }
        return complete(args[1], Arrays.asList(
                "arena", "shop", "point", "level",
                "rank", "lobby", "blacklist", "stop", "reload"
        ));
    }

    private List<String> tabArena(CommandSender sender, String[] args) {
        boolean isAdmin = sender.hasPermission(CommandPermission.ADMIN);
        if (args.length == 2) {
            List<String> candidates = new ArrayList<>(Arrays.asList("list", "type"));
            if (isAdmin) {
                candidates.addAll(Arrays.asList("create", "view", "delete", "function"));
            }
            return complete(args[1], candidates);
        }
        if (!isAdmin) {
            return Collections.emptyList();
        }

        if (isAlias(args[1], "create", "c") && args.length == 3) {
            return complete(args[2], getArenaTypeIds());
        }
        if ((isAlias(args[1], "view", "v") || isAlias(args[1], "delete", "d")) && args.length == 3) {
            return complete(args[2], getArenaIds());
        }
        if (!isAlias(args[1], "function", "f", "functions")) {
            return Collections.emptyList();
        }
        if (args.length == 3) {
            return complete(args[2], Arrays.asList("add", "remove", "reset", "clear", "list", "type", "copy"));
        }

        if (isAlias(args[2], "type", "t") && args.length == 4) {
            return complete(args[3], getArenaTypeIds());
        }
        if (isAlias(args[2], "copy", "cp")) {
            if (args.length == 4 || args.length == 5) {
                return complete(args[args.length - 1], getArenaIds());
            }
            if (args.length == 6) {
                return complete(args[5], getArenaFunctionIdsByArenaId(args[3]));
            }
            return Collections.emptyList();
        }

        if (args.length == 4) {
            return complete(args[3], getArenaIds());
        }
        if ((isAlias(args[2], "add", "a") || isAlias(args[2], "remove", "r") || isAlias(args[2], "reset", "rs")) && args.length == 5) {
            return complete(args[4], getArenaFunctionIdsByArenaId(args[3]));
        }
        return Collections.emptyList();
    }

    private List<String> tabBlacklist(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return complete(args[1], Arrays.asList("add", "remove", "view"));
        }
        if (isAlias(args[1], "add", "a") && args.length == 3) {
            return complete(args[2], getOnlinePlayerNames(sender, true));
        }
        if (isAlias(args[1], "remove", "r") && args.length == 3) {
            return complete(args[2], getBlacklistNames());
        }
        return Collections.emptyList();
    }

    private List<String> tabAcceptOrDecline(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return Collections.emptyList();
        }
        return complete(args[1], getRequestSenderNames(sender));
    }

    private List<String> tabLang(String[] args) {
        if (args.length != 2) {
            return Collections.emptyList();
        }
        return complete(args[1], getLanguageFileNames());
    }

    private List<String> tabLevel(CommandSender sender, String[] args) {
        boolean isAdmin = sender.hasPermission(CommandPermission.ADMIN);
        if (args.length == 2) {
            List<String> candidates = new ArrayList<>(Collections.singletonList("me"));
            if (isAdmin) {
                candidates.addAll(Arrays.asList("view", "exp"));
            }
            return complete(args[1], candidates);
        }
        if (isAlias(args[1], "view", "v") && isAdmin && args.length == 3) {
            return complete(args[2], getOnlinePlayerNames(sender, true));
        }
        if (isAlias(args[1], "exp", "e") && isAdmin) {
            if (args.length == 3) {
                return complete(args[2], Arrays.asList("add", "set"));
            }
            if (args.length == 4) {
                return complete(args[3], getOnlinePlayerNames(sender, true));
            }
        }
        return Collections.emptyList();
    }

    private List<String> tabPoint(CommandSender sender, String[] args) {
        boolean isAdmin = sender.hasPermission(CommandPermission.ADMIN);
        if (args.length == 2) {
            List<String> candidates = new ArrayList<>(Collections.singletonList("me"));
            if (isAdmin) {
                candidates.addAll(Arrays.asList("add", "set", "view"));
            }
            return complete(args[1], candidates);
        }
        if (!isAdmin) {
            return Collections.emptyList();
        }
        if ((isAlias(args[1], "add", "a", "give", "g") || isAlias(args[1], "set", "s") || isAlias(args[1], "view", "v")) && args.length == 3) {
            return complete(args[2], getOnlinePlayerNames(sender, true));
        }
        return Collections.emptyList();
    }

    private List<String> tabSend(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return complete(args[1], getOnlinePlayerNames(sender, false));
        }
        if (args.length == 3) {
            return complete(args[2], getArenaIds());
        }
        return Collections.emptyList();
    }

    private List<String> tabShop(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return complete(args[1], Arrays.asList("help", "add", "delete", "reset", "command"));
        }

        if (isAlias(args[1], "delete", "d")) {
            return tabShopLocationArgs(args, 2);
        }
        if (isAlias(args[1], "reset", "r")) {
            if (args.length == 3) {
                return complete(args[2], Arrays.asList("point", "description", "level"));
            }
            return tabShopLocationArgs(args, 3);
        }
        if (isAlias(args[1], "command", "c", "commands", "cmd")) {
            if (args.length == 3) {
                return complete(args[2], Arrays.asList("add", "remove", "clear", "list"));
            }
            if (isAlias(args[2], "add", "a")) {
                if (args.length <= 6) {
                    return tabShopLocationArgs(args, 3);
                }
                if (args.length == 7) {
                    return complete(args[6], Arrays.asList("player", "op", "console"));
                }
            }
            if (isAlias(args[2], "remove", "r")) {
                if (args.length <= 6) {
                    return tabShopLocationArgs(args, 3);
                }
                if (args.length == 7) {
                    return complete(args[6], getShopCommandIndexes(args[3], args[4], args[5]));
                }
            }
            if (isAlias(args[2], "clear", "c") || isAlias(args[2], "list", "l")) {
                return tabShopLocationArgs(args, 3);
            }
        }
        return Collections.emptyList();
    }

    private List<String> tabLobby(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN) || args.length != 2) {
            return Collections.emptyList();
        }
        return complete(args[1], Arrays.asList("help", "set", "delete"));
    }

    private List<String> tabRank(CommandSender sender, String[] args) {
        boolean isAdmin = sender.hasPermission(CommandPermission.ADMIN);
        if (args.length == 2) {
            List<String> candidates = new ArrayList<>(Arrays.asList("me", "view", "type"));
            if (isAdmin) {
                candidates.addAll(Arrays.asList("refresh", "hologram"));
            }
            return complete(args[1], candidates);
        }
        if ((isAlias(args[1], "view", "v") || isAlias(args[1], "refresh", "r")) && args.length == 3) {
            if (isAlias(args[1], "refresh", "r") && !isAdmin) {
                return Collections.emptyList();
            }
            return complete(args[2], getRankingIds());
        }
        if (isAlias(args[1], "hologram", "h", "hd") && isAdmin) {
            if (args.length == 3) {
                return complete(args[2], Arrays.asList("add", "delete", "move"));
            }
            if (args.length == 4) {
                return complete(args[3], getRankingIds());
            }
        }
        return Collections.emptyList();
    }

    private List<String> tabStats(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return Collections.emptyList();
        }
        return complete(args[1], getOnlinePlayerNames(sender, true));
    }

    private List<String> tabBalance(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return complete(args[1], Arrays.asList("view", "set", "config"));
        }
        if ((isAlias(args[1], "view", "v", "list", "ls")) && args.length == 3) {
            return complete(args[2], getOnlinePlayerNames(sender, true));
        }
        if ((isAlias(args[1], "set", "s")) && args.length == 3) {
            return complete(args[2], getOnlinePlayerNames(sender, true));
        }
        if ((isAlias(args[1], "set", "s")) && args.length == 4) {
            return complete(args[3], Arrays.asList("0", "10", "100"));
        }
        if ((isAlias(args[1], "config", "cfg", "c")) && args.length == 3) {
            return complete(args[2], Arrays.asList("view", "set"));
        }
        if ((isAlias(args[1], "config", "cfg", "c")) && isAlias(args[2], "set", "s") && args.length == 4) {
            return complete(args[3], Arrays.asList(CMDBalance.getSettingKeys()));
        }
        if ((isAlias(args[1], "config", "cfg", "c")) && isAlias(args[2], "set", "s") && args.length == 5) {
            switch (args[3].toLowerCase(Locale.ROOT)) {
                case "win-exp":
                    return complete(args[4], Arrays.asList("30", "50", "80"));
                case "win-point":
                    return complete(args[4], Arrays.asList("1", "2", "3"));
                case "lose-exp-rate":
                    return complete(args[4], Arrays.asList("0.2", "0.3", "0.5"));
                case "confirm-timeout":
                    return complete(args[4], Arrays.asList("10", "15", "20"));
                case "streak-enabled":
                case "streak-show-message":
                case "streak-reset-on-draw":
                case "leave-penalty-enabled":
                case "leave-penalty-apply-on-quit-command":
                case "leave-penalty-apply-on-disconnect":
                case "leave-penalty-apply-point-deduction":
                case "leave-penalty-apply-queue-cooldown":
                    return complete(args[4], Arrays.asList("true", "false"));
                case "leave-penalty-point":
                    return complete(args[4], Arrays.asList("1", "2", "3"));
                case "leave-penalty-cooldown":
                    return complete(args[4], Arrays.asList("10", "20", "30"));
                default:
                    return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    private List<String> tabUpdate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return complete(args[1], Arrays.asList("check", "download", "status"));
        }
        return Collections.emptyList();
    }

    private List<String> tabArenaId(String[] args, int argIndex) {
        if (args.length != argIndex + 1) {
            return Collections.emptyList();
        }
        return complete(args[argIndex], getArenaIds());
    }

    private List<String> tabShopLocationArgs(String[] args, int firstArgIndex) {
        if (args.length == firstArgIndex + 1) {
            return complete(args[firstArgIndex], getShopPages());
        }
        if (args.length == firstArgIndex + 2) {
            return complete(args[firstArgIndex + 1], getRangeString(1, 4));
        }
        if (args.length == firstArgIndex + 3) {
            return complete(args[firstArgIndex + 2], getRangeString(1, 5));
        }
        return Collections.emptyList();
    }

    private List<String> getRootSubCommandCandidates(CommandSender sender) {
        List<String> candidates = new ArrayList<>(ROOT_COMMANDS);
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            candidates.removeIf(ADMIN_ONLY_ROOT_COMMANDS::contains);
        }
        return candidates;
    }

    private List<String> getOnlinePlayerNames(CommandSender sender, boolean includeSelf) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(playerName -> includeSelf || !playerName.equalsIgnoreCase(sender.getName()))
                .collect(Collectors.toList());
    }

    private List<String> getRequestSenderNames(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getRequestReceiverManager() == null) {
            return Collections.emptyList();
        }
        RequestReceiver requestReceiver = plugin.getRequestReceiverManager().get(sender.getName());
        return requestReceiver == null ? Collections.emptyList() : requestReceiver.getValidSenderNames();
    }

    private List<String> getArenaIds() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getArenaManager() == null) {
            return Collections.emptyList();
        }
        return plugin.getArenaManager().getList().stream()
                .map(BaseArena::getId)
                .collect(Collectors.toList());
    }

    private List<String> getArenaTypeIds() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getArenaTypeManager() == null) {
            return Collections.emptyList();
        }
        return plugin.getArenaTypeManager().getList().stream()
                .map(ArenaType::getId)
                .collect(Collectors.toList());
    }

    private List<String> getArenaFunctionIdsByArenaId(String arenaId) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getArenaManager() == null) {
            return Collections.emptyList();
        }
        BaseArena arena = plugin.getArenaManager().get(arenaId);
        if (arena == null) {
            return Collections.emptyList();
        }
        return getArenaFunctionIdsByTypeId(arena.getArenaTypeId());
    }

    private List<String> getArenaFunctionIdsByTypeId(String arenaTypeId) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getArenaTypeManager() == null) {
            return Collections.emptyList();
        }
        ArenaType arenaType = plugin.getArenaTypeManager().get(arenaTypeId);
        if (arenaType == null || arenaType.getFunctionDef() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(arenaType.getFunctionDef().keySet());
    }

    private List<String> getLanguageFileNames() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getMsgManager() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(plugin.getMsgManager().getLanguageYamlFileMap().keySet());
    }

    private List<String> getRankingIds() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getRankingManager() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(plugin.getRankingManager().getRankings().keySet());
    }

    private List<String> getBlacklistNames() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getCacheManager() == null) {
            return Collections.emptyList();
        }
        BlacklistCache blacklistCache = plugin.getCacheManager().getBlacklistCache();
        return blacklistCache == null ? Collections.emptyList() : blacklistCache.get();
    }

    private List<String> getShopPages() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getCacheManager() == null) {
            return Collections.singletonList("1");
        }
        ShopCache shopCache = plugin.getCacheManager().getShopCache();
        if (shopCache == null) {
            return Collections.singletonList("1");
        }
        int size = shopCache.getList().size();
        int maxPage = Math.max(1, (int) Math.ceil(size / 20.0));
        return getRangeString(1, maxPage);
    }

    private List<String> getShopCommandIndexes(String pageString, String rowString, String columnString) {
        Integer page = parsePositiveInteger(pageString);
        Integer row = parsePositiveInteger(rowString);
        Integer column = parsePositiveInteger(columnString);
        if (page == null || row == null || column == null) {
            return Collections.emptyList();
        }

        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null || plugin.getCacheManager() == null || plugin.getCacheManager().getShopCache() == null) {
            return Collections.emptyList();
        }
        ShopRewardData rewardData = plugin.getCacheManager().getShopCache().get(page, row, column);
        if (rewardData == null || rewardData.getCommands() == null) {
            return Collections.emptyList();
        }
        return getRangeString(1, rewardData.getCommands().size());
    }

    private List<String> getRangeString(int from, int to) {
        if (to < from) {
            return Collections.emptyList();
        }
        return IntStream.rangeClosed(from, to)
                .mapToObj(String::valueOf)
                .collect(Collectors.toList());
    }

    private Integer parsePositiveInteger(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private boolean isAlias(String entered, String... aliases) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(entered)) {
                return true;
            }
        }
        return false;
    }

    private List<String> complete(String entered, Collection<String> candidates) {
        String enteredLower = entered == null ? "" : entered.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(value -> value != null && value.toLowerCase(Locale.ROOT).startsWith(enteredLower))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
