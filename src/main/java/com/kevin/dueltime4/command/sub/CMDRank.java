package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.LocationCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.event.ranking.TryToRefreshRankingEvent;
import com.kevin.dueltime4.ranking.Ranking;
import com.kevin.dueltime4.ranking.RankingData;
import com.kevin.dueltime4.ranking.RankingManager;
import com.kevin.dueltime4.ranking.hologram.HologramManager;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDRank extends SubCommand {
    private final UtilHelpList helpList;

    public CMDRank() {
        super("rank", "rk");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_RANK, true)
                .add("me", new String[]{"me"}, "me", null, Msg.COMMAND_SUB_RANK_ME_DESCRIPTION)
                .add("view", new String[]{"view", "v"}, "view(v) <%ranking_type%> [%ranking_page%]", null, Msg.COMMAND_SUB_RANK_VIEW_DESCRIPTION)
                .add("type", new String[]{"type", "t"}, "type(t)", null, Msg.COMMAND_SUB_RANK_TYPE_DESCRIPTION)
                .add("refresh", new String[]{"refresh", "r"}, "refresh(r) <%ranking_type%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_RANK_REFRESH_DESCRIPTION)
                .add("hologram", new String[]{"hologram", "h","hd"}, "hologram(h) add(a)/delete(d)/move(m) <%ranking_type%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_RANK_HOLOGRAM_DESCRIPTION);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            helpList.send(sender, label, args[0]);
            return true;
        }
        RankingManager rankingManager = DuelTimePlugin.getInstance().getRankingManager();
        String commandEntered = args[1];
        UtilHelpList.SingleCommand singleCommand = helpList.getSubCommandByEnter(commandEntered);
        if (singleCommand == null) {
            helpList.send(sender, label, args[0]);
            helpList.sendSuggest(sender, label, args);
            return true;
        }
        String singleCommandId = singleCommand.getId();
        if (singleCommandId.equals("me")) {
            if (!(sender instanceof Player)) {
                MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
                return true;
            }
            MsgBuilder.sends(Msg.COMMAND_SUB_RANK_ME_HEADING, sender);
            for (Ranking ranking : rankingManager.getRankings().values()) {
                if (ranking.getRank(sender.getName()) != -1) {
                    MsgBuilder.sendClickable(Msg.COMMAND_SUB_RANK_ME_BODY, sender, false,
                            ranking.getName(sender),
                            "" + ranking.getRank(sender.getName()),
                            "" + ranking.getContent().size(),
                            ranking.getId());
                } else {
                    MsgBuilder.sendClickable(Msg.COMMAND_SUB_RANK_ME_BODY_NO_DATA, sender, false,
                            ranking.getName(sender),
                            ranking.getId());
                }
            }
            MsgBuilder.sends(Msg.COMMAND_SUB_RANK_ME_ENDING, sender, false);
            return true;
        }
        if (singleCommandId.equals("view")) {
            if (args.length < 3) {
                helpList.sendCorrect(sender, -1, singleCommand, label, args);
                return true;
            }
            String rankingId = args[2];
            Ranking ranking = rankingManager.getRanking(rankingId);
            if (ranking == null) {
                MsgBuilder.sendClickable(Msg.COMMAND_SUB_RANK_INVALID_RANKING_TYPE_ID, sender, false,
                        rankingId);
                UtilHelpList.sendSuggest(sender, 2, rankingManager.getRankings().keySet(), label, args);
                return true;
            }
            if (ranking.getContent().isEmpty()) {
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_VIEW_EMPTY, sender,
                        ranking.getName(sender));
                return true;
            }
            int pageViewed = 1;
            if (args.length > 3) {
                if (!UtilFormat.isInt(args[3])) {
                    MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender, args[3]);
                }
                pageViewed = Integer.parseInt(args[3]);
            }
            double dataSize = ranking.getContent().size();
            int singlePageSize = ranking.getSinglePageSize();
            int totalPages = (int) Math.ceil(dataSize / singlePageSize);
            if (pageViewed < 1 || pageViewed > totalPages) {
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_VIEW_INVALID_RANKING_PAGE, sender,
                        "" + totalPages);
                return true;
            }
            MsgBuilder.sendsClickable(Msg.COMMAND_SUB_RANK_VIEW_HEADING, sender, false,
                    ranking.getName(sender), ranking.getDescription(sender));
            for (int i = singlePageSize * (pageViewed - 1); i < singlePageSize * pageViewed && i < dataSize; i++) {
                RankingData data = ranking.getContent().get(i);
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_VIEW_BODY, sender, false,
                        "" + (i + 1),
                        data.getPlayerName(),
                        UtilFormat.toString(data.getData()),
                        UtilFormat.toString(data.getExtraStr(), sender)
                );
            }
            MsgBuilder.sendsClickable(Msg.COMMAND_SUB_RANK_VIEW_ENDING, sender, false,
                    "" + (pageViewed - 1), "" + pageViewed, "" + totalPages, "" + (pageViewed + 1), rankingId);
            int playerRank = ranking.getRank(sender.getName());
            if (playerRank != -1) {
                RankingData data = ranking.getContent().get(playerRank - 1);
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_VIEW_ENDING_SHOW_ME, sender, false,
                        "" + playerRank,
                        "" + ranking.getContent().size(),
                        UtilFormat.toString(data.getData()),
                        UtilFormat.toString(data.getExtraStr(), sender));
            } else {
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_VIEW_ENDING_SHOW_ME_NO_DATA, sender);
            }
            return true;
        }
        if (singleCommandId.equals("type")) {
            MsgBuilder.send(Msg.COMMAND_SUB_RANK_TYPE_HEADING, sender, false);
            int i = 1;
            for (Ranking ranking : rankingManager.getRankings().values()) {
                MsgBuilder.sends(Msg.COMMAND_SUB_RANK_TYPE_BODY, sender, false,
                        "" + i, ranking.getName(sender), ranking.getId(), ranking.getDescription(sender));
                i++;
            }
            MsgBuilder.send(Msg.COMMAND_SUB_RANK_TYPE_ENDING, sender, false,
                    "" + rankingManager.getRankings().size());
            return true;
        }
        if (singleCommandId.equals("refresh")) {
            if (!singleCommand.judgePermission(sender)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            if (args.length < 3) {
                helpList.sendCorrect(sender, -1, singleCommand, label, args);
                return true;
            }
            String rankingId = args[2];
            if (rankingManager.getRanking(rankingId) == null) {
                MsgBuilder.sendClickable(Msg.COMMAND_SUB_RANK_INVALID_RANKING_TYPE_ID, sender, false,
                        rankingId);
                UtilHelpList.sendSuggest(sender, 2, rankingManager.getRankings().keySet(), label, args);
                return true;
            }
            // 發布請求重新整理排行榜事件
            Bukkit.getServer().getPluginManager().callEvent(new TryToRefreshRankingEvent(sender, DuelTimePlugin.getInstance().getRankingManager().getRanking(rankingId)));
        }
        if (singleCommandId.equals("hologram")) {
            if (!singleCommand.judgePermission(sender)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            HologramManager hologramManager = DuelTimePlugin.getInstance().getHologramManager();
            if (!hologramManager.isEnabled()) {
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_DISABLED, sender);
                return true;
            }
            if (args.length < 4) {
                helpList.sendCorrect(sender, 2, helpList.getSubCommandById("hologram"), label, args);
                return true;
            }
            boolean isAdd = args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("a");
            boolean isDelete = args[2].equalsIgnoreCase("delete") || args[2].equalsIgnoreCase("d");
            boolean isMove = args[2].equalsIgnoreCase("move") || args[2].equalsIgnoreCase("m");
            if (!isAdd && !isDelete && !isMove) {
                helpList.sendCorrect(sender, 2, helpList.getSubCommandById("hologram"), label, args);
                return true;
            }
            String rankingIdEntered = args[3];
            Ranking ranking = rankingManager.getRanking(rankingIdEntered);
            if (ranking == null) {
                MsgBuilder.sendClickable(Msg.COMMAND_SUB_RANK_INVALID_RANKING_TYPE_ID, sender, false,
                        rankingIdEntered);
                UtilHelpList.sendSuggest(sender, 3, rankingManager.getRankings().keySet(), label, args);
                return true;
            }
            String rankingId = ranking.getId();
            LocationCache locationCache = DuelTimePlugin.getInstance().getCacheManager().getLocationCache();
            Location hologramLoc = locationCache.get(rankingId);
            if (isAdd || isMove) {
                if (!(sender instanceof Player)) {
                    MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
                    return true;
                }
                Location playerLoc = ((Player) sender).getLocation();
                if (isAdd) {
                    if (hologramLoc != null) {
                        MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_ADD_FAIL_ALREADY_EXISTS, sender,
                                ranking.getName(sender), UtilFormat.toString(hologramLoc));
                        return true;
                    }
                    locationCache.add(rankingId, playerLoc);
                    hologramManager.create(ranking);
                    MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_ADD_SUCCESSFULLY, sender,
                            ranking.getName(sender), UtilFormat.toString(playerLoc));
                } else {
                    if (hologramLoc == null) {
                        MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_MOVE_FAIL_NOT_EXISTS, sender,
                                ranking.getName(sender));
                        return true;
                    }
                    locationCache.set(rankingId, playerLoc);
                    hologramManager.move(rankingId, playerLoc);
                    MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_MOVE_SUCCESSFULLY, sender,
                            ranking.getName(sender),UtilFormat.toString(playerLoc));
                }
            } else {
                if (hologramLoc == null) {
                    MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_DELETE_FAIL_NOT_EXISTS, sender,
                            ranking.getName(sender));
                    return true;
                }
                locationCache.remove(rankingId);
                hologramManager.destroy(rankingId);
                MsgBuilder.send(Msg.COMMAND_SUB_RANK_HOLOGRAM_DELETE_SUCCESSFULLY,sender,
                        ranking.getName(sender),UtilFormat.toString(hologramLoc));
            }
            return true;
        }
        return true;
    }
}
