package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.level.Tier;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;

public class CMDLevel extends SubCommand {
    private final UtilHelpList helpList;

    public CMDLevel() {
        super("level", "lv");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_LEVEL, true)
                .add("me", new String[]{"me"}, "me", null, Msg.COMMAND_SUB_LEVEL_VIEW_SELF_DESCRIPTION)
                .add("view", new String[]{"view", "v"}, "view(v) <%player%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_LEVEL_VIEW_PLAYER_DESCRIPTION)
                .add("exp", new String[]{"exp", "e"}, "exp(e) add(a)/set(s) <%player%> <%value%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_LEVEL_EXP_DESCRIPTION);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            helpList.send(sender, label, args[0]);
            return true;
        }
        PlayerDataCache cache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
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
            LevelManager levelManager = DuelTimePlugin.getInstance().getLevelManager();
            String playerName = sender.getName();
            int level = levelManager.getLevel(playerName);
            String title = levelManager.getTier(playerName).getTitle();
            double totalExp = cache.get(sender.getName()).getExp();
            double needExp = levelManager.calculateRemainingExpForLevelUp(totalExp);
            double completedProgress = levelManager.calculateLevelUpProgress(totalExp);
            double remainingProgress = 1 - completedProgress;
            MsgBuilder.sends(Msg.COMMAND_SUB_LEVEL_VIEW_SELF, sender,
                    "" + level,
                    title,
                    "" + totalExp,
                    "" + needExp,
                    completedProgress + " " + remainingProgress,
                    "" + Double.parseDouble(new DecimalFormat("#.#").format(completedProgress * 100)));
            return true;
        }
        if (singleCommandId.equals("view")) {
            if (!singleCommand.judgePermission(sender)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            if (args.length == 2) {
                helpList.sendCorrect(sender, 2, commandEntered, label, args);
                return true;
            }
            String targetPlayerName = args[2];
            PlayerData targetPlayerData = cache.getAnyway(targetPlayerName);
            if (targetPlayerData == null) {
                helpList.sendCorrect(sender, 2, commandEntered, label, args);
                MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender,
                        targetPlayerName);
                return true;
            }
            double exp = targetPlayerData.getExp();
            LevelManager levelManager = DuelTimePlugin.getInstance().getLevelManager();
            int level = levelManager.getLevel(targetPlayerName, exp);
            Tier tier = levelManager.getTier(targetPlayerName, exp);
            MsgBuilder.sends(Msg.COMMAND_SUB_LEVEL_VIEW_PLAYER, sender,
                    targetPlayerName, "" + level, tier.getTitle(), "" + exp);
            return true;
        }
        if (singleCommandId.equals("exp")) {
            if (!singleCommand.judgePermission(sender)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            if (args.length < 5) {
                helpList.sendCorrect(sender, -1, commandEntered, label, args);
                return true;
            }
            boolean isAdd = args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("a");
            boolean isSet = args[2].equalsIgnoreCase("set") || args[2].equalsIgnoreCase("s");
            if (!isAdd && !isSet) {
                UtilHelpList.sendSuggest(sender, 2, Arrays.asList("add", "set"), label, args);
                return true;
            }
            String targetPlayerName = args[3];
            PlayerData playerData = cache.getAnyway(targetPlayerName);
            if (playerData == null) {
                helpList.sendCorrect(sender, 3, commandEntered, label, args);
                MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender,
                        targetPlayerName);
                return true;
            }
            String expEnteredString = args[4];
            if (!UtilFormat.isDouble(expEnteredString)) {
                helpList.sendCorrect(sender, 4, commandEntered, label, args);
                MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender, expEnteredString);
                return true;
            }
            double expEntered = Double.parseDouble(expEnteredString);
            if (isAdd && expEntered <= 0) {
                MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, sender, expEnteredString);
                return true;
            }
            if (isSet && expEntered < 0) {
                MsgBuilder.send(Msg.ERROR_VALUE_IS_NEGATIVE, sender, expEnteredString);
                return true;
            }
            if (isAdd) {
                // 操作為新增
                playerData.setExp(playerData.getExp() + expEntered);
                MsgBuilder.send(Msg.COMMAND_SUB_LEVEL_ADD_SUCCESSFULLY, sender,
                        targetPlayerName, "" + expEntered);
            } else {
                // 操作為設定
                playerData.setExp(expEntered);
                MsgBuilder.send(Msg.COMMAND_SUB_LEVEL_SET_SUCCESSFULLY, sender,
                        targetPlayerName, "" + expEntered);
            }
            // 完成快取變更
            cache.set(targetPlayerName, playerData);
            return true;
        }
        return true;
    }
}
