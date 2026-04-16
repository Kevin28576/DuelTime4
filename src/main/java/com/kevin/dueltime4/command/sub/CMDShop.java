package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.ShopCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.ShopRewardData;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CMDShop extends SubCommand {
    private final UtilHelpList helpList;

    public CMDShop() {
        super("shop", "s", "pointshop", "ps");
        helpList = new UtilHelpList(Msg.COMMAND_TITLE_SHOP, true)
                .add("help", new String[]{"help", "h"}, "help", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_HELP_DESCRIPTION)
                .add("add", new String[]{"add", "a"}, "add(a) <%point%> [%description%]", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_ADD_DESCRIPTION)
                .add("delete", new String[]{"delete", "d"}, "delete(d) <%page%> <%row%> <%column%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_DELETE_DESCRIPTION)
                .add("reset", new String[]{"reset", "r"}, "reset(r) point(p)/description(d)/level(l) <%page%> <%row%> <%column%> <%value_or_content%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_RESET_DESCRIPTION)
                .add("command_add", new String[]{"command", "c", "commands", "cmd"}, "command(c) add(a) <%page%> <%row%> <%column%> <%command_executor%> <%value_or_content%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_COMMAND_ADD_DESCRIPTION)
                .add("command_remove", new String[]{"command", "c", "commands", "cmd"}, "command(c) remove(r) <%page%> <%row%> <%column%> <%index%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_COMMAND_REMOVE_DESCRIPTION)
                .add("command_clear", new String[]{"command", "c", "commands", "cmd"}, "command(c) clear(c) <%page%> <%row%> <%column%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_COMMAND_CLEAR_DESCRIPTION)
                .add("command_list", new String[]{"command", "c", "commands", "cmd"}, "command(c) list(l) <%page%> <%row%> <%column%>", CommandPermission.ADMIN, Msg.COMMAND_SUB_SHOP_COMMAND_LIST_DESCRIPTION);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
                return true;
            }
            DuelTimePlugin.getInstance().getCustomInventoryManager().getShop().openFor(((Player) sender));
            return true;
        }
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }
        ShopCache cache = DuelTimePlugin.getInstance().getCacheManager().getShopCache();
        String commandEntered = args[1];
        UtilHelpList.SingleCommand singleCommand = helpList.getSubCommandByEnter(commandEntered);
        if (singleCommand == null) {
            helpList.send(sender, label, args[0]);
            helpList.sendSuggest(sender, label, args);
            return true;
        }
        String singleCommandId = singleCommand.getId();
        if (singleCommandId.equals("help")) {
            helpList.send(sender, label, args[0]);
            return true;
        }
        if (singleCommandId.equals("add")) {
            if (!(sender instanceof Player)) {
                MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
                return true;
            }
            if (ViaVersionItem.getItemInMainHand((Player) sender).getType().equals(Material.AIR)) {
                MsgBuilder.send(Msg.COMMAND_SUB_SHOP_ADD_EMPTY_ITEMSTACK, sender);
                return true;
            }
            if (args.length < 3) {
                helpList.sendCorrect(sender, -1, singleCommand, label, args);
                return true;
            }
            String pointEnteredString = args[2];
            if (!UtilFormat.isDouble(pointEnteredString)) {
                helpList.sendCorrect(sender, 2, singleCommand, label, args);
                MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender,
                        pointEnteredString);
                return true;
            }
            double pointEntered = Double.parseDouble(pointEnteredString);
            if (pointEntered <= 0) {
                MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, sender,
                        pointEnteredString);
                return true;
            }
            ItemStack itemStack = ViaVersionItem.getItemInMainHand(((Player) sender));
            String description = args.length >= 4 ?
                    args[3].replace("&", "§").replace("_", " ") :
                    null;
            cache.add(itemStack, pointEntered, 0, description, null);
            int[] loc = ShopCache.getLocByIndex(cache.getList().size() - 1);
            MsgBuilder.send(Msg.COMMAND_SUB_SHOP_ADD_SUCCESSFULLY, sender,
                    "" + loc[0], "" + loc[1], "" + loc[2]);
            return true;
        }
        if (singleCommandId.equals("delete")) {
            if (args.length < 5) {
                helpList.sendCorrect(sender, -1, singleCommand, label, args);
                return true;
            }
            int[] loc = checkLocFormat(2, sender, singleCommand, label, args);
            if (loc == null) {
                return true;
            }
            boolean delete = cache.delete(loc[0], loc[1], loc[2]);
            if (!delete) {
                MsgBuilder.send(Msg.COMMAND_SUB_SHOP_INVALID_LOC, sender,
                        "" + loc[0], "" + loc[1], "" + loc[2]);
                return true;
            }
            MsgBuilder.send(Msg.COMMAND_SUB_SHOP_DELETE_SUCCESSFULLY, sender,
                    "" + loc[0], "" + loc[1], "" + loc[2]);
            return true;
        }
        if (singleCommandId.equals("reset")) {
            if (args.length < 7) {
                helpList.sendCorrect(sender, -1, singleCommand, label, args);
                return true;
            }
            boolean isPoint = args[2].equalsIgnoreCase("point") || args[2].equalsIgnoreCase("p");
            boolean isDescription = args[2].equalsIgnoreCase("description") || args[2].equalsIgnoreCase("d");
            boolean isLevel = args[2].equalsIgnoreCase("level") || args[2].equalsIgnoreCase("l");
            if (!isPoint && !isDescription && !isLevel) {
                helpList.sendCorrect(sender, 2, singleCommand, label, args);
                return true;
            }
            int[] loc = checkLocFormat(3, sender, singleCommand, label, args);
            if (loc == null) {
                return true;
            }
            ShopRewardData rewardData = cache.get(loc[0], loc[1], loc[2]);
            if (rewardData == null) {
                MsgBuilder.send(Msg.COMMAND_SUB_SHOP_INVALID_LOC, sender,
                        "" + loc[0], "" + loc[1], "" + loc[2]);
                return true;
            }
            if (isPoint) {
                String pointEnteredString = args[6];
                if (!UtilFormat.isDouble(pointEnteredString)) {
                    helpList.sendCorrect(sender, 3, singleCommand, label, args);
                    MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender,
                            pointEnteredString);
                    return true;
                }
                double pointEntered = Double.parseDouble(pointEnteredString);
                if (pointEntered <= 0) {
                    MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, sender, pointEnteredString);
                    return true;
                }
                rewardData.setPoint(pointEntered);
                cache.set(loc[0], loc[1], loc[2], rewardData);
                MsgBuilder.send(Msg.COMMAND_SUB_SHOP_RESET_POINT_SUCCESSFULLY, sender,
                        pointEnteredString, "" + loc[0], "" + loc[1], "" + loc[2]);
            } else if (isDescription) {
                // 將輸入內容中的下劃線轉化為空格，同時替換顏色符號
                String description = args[6].replace("_", " ").replace("&", "§");
                rewardData.setDescription(description);
                cache.set(loc[0], loc[1], loc[2], rewardData);
                MsgBuilder.send(Msg.COMMAND_SUB_SHOP_RESET_DESCRIPTION_SUCCESSFULLY, sender,
                        description, "" + loc[0], "" + loc[1], "" + loc[2]);
            } else {
                String levelEnteredString = args[6];
                if (!UtilFormat.isInt(levelEnteredString)) {
                    helpList.sendCorrect(sender, 3, singleCommand, label, args);
                    MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender,
                            levelEnteredString);
                    return true;
                }
                int levelEntered = Integer.parseInt(levelEnteredString);
                if (levelEntered <= 0) {
                    MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, sender,
                            levelEnteredString);
                    return true;
                }
                rewardData.setLevelLimit(levelEntered);
                cache.set(loc[0], loc[1], loc[2], rewardData);
                MsgBuilder.send(Msg.COMMAND_SUB_SHOP_RESET_LEVEL_LIMIT_SUCCESSFULLY, sender,
                        levelEnteredString, "" + loc[0], "" + loc[1], "" + loc[2]);
            }
            return true;
        }
        if (singleCommandId.startsWith("command")) {
            // 如果引數不足，則預設想輸入add
            if (args.length < 3) {
                helpList.sendCorrect(sender, 2, helpList.getSubCommandById("command_add"), label, args);
                return true;
            }
            boolean isAdd = args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("a");
            boolean isRemove = args[2].equalsIgnoreCase("remove") || args[2].equalsIgnoreCase("r");
            boolean isClear = args[2].equalsIgnoreCase("clear") || args[2].equalsIgnoreCase("c");
            boolean isView = args[2].equalsIgnoreCase("list") || args[2].equalsIgnoreCase("l");
            if (isAdd || isRemove) {
                singleCommand = isAdd ? helpList.getSubCommandById("command_add") : helpList.getSubCommandById("command_remove");
                if (args.length < (isAdd ? 8 : 6)) {
                    helpList.sendCorrect(sender, -1, singleCommand, label, args);
                    return true;
                }
                int[] loc = checkLocFormat(3, sender, singleCommand, label, args);
                if (loc == null) {
                    return true;
                }
                ShopRewardData rewardData = cache.get(loc[0], loc[1], loc[2]);
                if (rewardData == null) {
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_INVALID_LOC, sender,
                            "" + loc[0], "" + loc[1], "" + loc[2]);
                    return true;
                }
                List<String> commands = rewardData.getCommands() != null ? rewardData.getCommands() : new ArrayList<>();
                if (isAdd) {
                    // 判斷輸入的執行身份是否符合要求，若符合要求則轉化為小寫後存入
                    String commandExecutorEntered = args[6];
                    Msg executorMsg;
                    switch (commandExecutorEntered) {
                        case "player":
                        case "p":
                            commandExecutorEntered = "player";
                            executorMsg = Msg.STRING_COMMAND_EXECUTOR_PLAYER;
                            break;
                        case "op":
                        case "o":
                            commandExecutorEntered = "op";
                            executorMsg = Msg.STRING_COMMAND_EXECUTOR_OP;
                            break;
                        case "console":
                        case "c":
                            commandExecutorEntered = "console";
                            executorMsg = Msg.STRING_COMMAND_EXECUTOR_CONSOLE;
                            break;
                        default:
                            // 如果輸入的執行身份不符合要求，則生成建議
                            StringBuilder commandExecutorEnteredCorrected = new StringBuilder("/").append(label);
                            for (int i = 0; i < args.length; i++) {
                                String strAppended = i == 6 ? "op" : args[i];
                                commandExecutorEnteredCorrected.append(" ").append(strAppended);
                            }
                            MsgBuilder.sendsClickable(Msg.COMMAND_SUB_SHOP_COMMAND_ADD_TIP_COMMAND_EXECUTOR, sender, false,
                                    commandExecutorEntered, commandExecutorEnteredCorrected.toString());
                            UtilHelpList.sendSuggest(sender, 6, Arrays.asList("player", "op", "console"), label, args);
                            return true;
                    }
                    // 將輸入內容中的下劃線轉化為空格，同時替換顏色符號
                    String commandContent = args[7].replace("_", " ").replace("&", "§");
                    commands.add(commandExecutorEntered + ":" + commandContent);
                    rewardData.setCommands(commands);
                    cache.set(loc[0], loc[1], loc[2], rewardData);
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_ADD_SUCCESSFULLY, sender,
                            "/" + commandContent, MsgBuilder.get(executorMsg, sender),
                            "" + loc[0], "" + loc[1], "" + loc[2]);
                } else {
                    String indexEnteredString = args[3];
                    if (!UtilFormat.isInt(indexEnteredString)) {
                        helpList.sendCorrect(sender, 3, commandEntered, label, args);
                        MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender,
                                indexEnteredString);
                        return true;
                    }
                    int indexEntered = Integer.parseInt(indexEnteredString);
                    if (indexEntered <= 0) {
                        MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, sender,
                                indexEnteredString);
                        return true;
                    }
                    if (indexEntered > commands.size()) {
                        MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_INDEX_IS_NOT_FOUND, sender,
                                indexEnteredString);
                        return true;
                    }
                    String commandRemoved = commands.get(indexEntered - 1);
                    if (commands.size() > 1) {
                        commands.remove(indexEntered - 1);
                        rewardData.setCommands(commands);
                    } else {
                        rewardData.setCommands(null);
                    }
                    cache.set(loc[0], loc[1], loc[2], rewardData);
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_REMOVE_SUCCESSFULLY, sender,
                            commandRemoved, "" + loc[0], "" + loc[1], "" + loc[2], indexEnteredString);
                }
                return true;
            }
            if (isClear || isView) {
                singleCommand = isClear ? helpList.getSubCommandById("command_clear") : helpList.getSubCommandById("command_list");
                if (args.length < 6) {
                    helpList.sendCorrect(sender, -1, singleCommand, label, args);
                    return true;
                }
                int[] loc = checkLocFormat(3, sender, singleCommand, label, args);
                if (loc == null) {
                    return true;
                }
                ShopRewardData rewardData = cache.get(loc[0], loc[1], loc[2]);
                if (rewardData == null) {
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_INVALID_LOC, sender,
                            "" + loc[0], "" + loc[1], "" + loc[2]);
                    return true;
                }
                if (singleCommandId.equals("command_clear")) {
                    rewardData.setCommands(null);
                    cache.set(loc[0], loc[1], loc[2], rewardData);
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_CLEAR_SUCCESSFULLY, sender);
                } else {
                    List<String> commands = rewardData.getCommands();
                    if (commands == null) {
                        MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_EMPTY, sender);
                        return true;
                    }
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_LIST_HEADING, sender, false,
                            "" + loc[0], "" + loc[1], "" + loc[2]);
                    for (int i = 0; i < commands.size(); i++) {
                        String commandData = commands.get(i);
                        String commandExecutor = commandData.split(":")[1];
                        String commandContent = commandData.substring(commandExecutor.length());
                        Msg executorMsg;
                        switch (commandExecutor) {
                            case "player":
                                executorMsg = Msg.STRING_COMMAND_EXECUTOR_PLAYER;
                                break;
                            case "op":
                                executorMsg = Msg.STRING_COMMAND_EXECUTOR_OP;
                                break;
                            case "console":
                                executorMsg = Msg.STRING_COMMAND_EXECUTOR_CONSOLE;
                                break;
                            default:
                                continue;
                        }
                        MsgBuilder.sendClickable(Msg.COMMAND_SUB_SHOP_COMMAND_LIST_BODY, sender, false,
                                "" + (i + 1),
                                commandContent,
                                MsgBuilder.get(executorMsg, sender),
                                "" + loc[0], "" + loc[1], "" + loc[2]);
                    }
                    MsgBuilder.send(Msg.COMMAND_SUB_SHOP_COMMAND_LIST_ENDING, sender, false,
                            "" + rewardData.getCommands().size());
                }
                return true;
            }
            UtilHelpList.sendSuggest(sender, 2, Arrays.asList("add", "set", "clear", "list"), label, args);
            return true;
        }
        return true;
    }

    /**
     * 用來檢查輸入的位置資料（頁數、行數、列數）是否都為正整數
     * 中途一旦檢查不透過，則立即返回null，並告知不透過的原因
     *
     * @return 一個三元陣列，裡麵包含檢查都透過的位置資料，依次為：頁數、行數、列數
     */
    private int[] checkLocFormat(int startArgIndex, CommandSender sender,
                                 UtilHelpList.SingleCommand singleCommand, String label, String[] args) {
        int[] loc = new int[3];
        int argIndex = startArgIndex;
        for (int i = 0; i < 3; i++) {
            String valueEnteredString = args[startArgIndex + i];
            if (!UtilFormat.isInt(valueEnteredString)) {
                helpList.sendCorrect(sender, argIndex, singleCommand, label, args);
                MsgBuilder.send(Msg.ERROR_INCORRECT_INTEGER_FORMAT, sender,
                        valueEnteredString);
                return null;
            }
            int value = Integer.parseInt(valueEnteredString);
            if (value <= 0) {
                helpList.sendCorrect(sender, argIndex, singleCommand, label, args);
                return null;
            }
            loc[i] = value;
            argIndex++;
        }
        return loc;
    }
}
