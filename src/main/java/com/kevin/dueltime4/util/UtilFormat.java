package com.kevin.dueltime4.util;

import com.kevin.dueltime4.arena.base.BaseArenaData;
import com.kevin.dueltime4.arena.base.BaseGamerData;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UtilFormat {
    public static boolean isDouble(String enter) {
        try {
            Double.parseDouble(enter);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isInt(String enter) {
        try {
            Integer.parseInt(enter);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isIDStyle(String enter) {
        String regex = "^[a-zA-Z0-9_]+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(enter);
        return matcher.matches();
    }

    public static double round(double number, int scale) {
        return new BigDecimal(Double.toString(number)).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static String distinguishPositiveNumber(double number) {
        double numberRounded = round(number, 1);
        if (number > 0) {
            return "§a+" + numberRounded;
        } else if (number < 0) {
            return "§c" + numberRounded;
        } else {
            return "§7+0";
        }
    }

    public static String toString(Object obj, CommandSender commandSender, StringifyTag... tags) {
        if (obj instanceof Object[]) {
            // 如果是Object集合，就拆分並格式化
            StringBuilder builder = new StringBuilder("§f(");
            for (Object objSplit : (Object[]) obj) {
                builder.append(stringify(objSplit, commandSender, tags)).append("§f, ");
            }
            return builder.substring(0, builder.length() - 1) + "§f)";
        } else {
            // 如果是單個Object，則直接格式化
            return stringify(obj, commandSender, tags);
        }
    }

    public static String toString(Object obj, StringifyTag... tags) {
        return toString(obj, null, tags);
    }

    private static String stringify(Object obj, CommandSender commandSender, StringifyTag... tags) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof Location) {
            Location loc = (Location) obj;
            return "(" + loc.getWorld().getName() + ": " + round(loc.getX(),1) + "," + round(loc.getY(),1) + "," + round(loc.getZ(),1) + ")";
        }
        if (obj instanceof Boolean) {
            boolean bool = (boolean) obj;
            return bool ? "§a" + MsgBuilder.get(Msg.STRING_TRUE, commandSender) : "§c" + MsgBuilder.get(Msg.STRING_FALSE, commandSender);
        }
        if (obj instanceof Msg) {
            return MsgBuilder.get((Msg) obj, commandSender);
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty() && list.get(0) instanceof String) {
                StringBuilder stringBuilder = new StringBuilder();
                boolean isLimitLineLength = tags != null && Arrays.asList(tags).contains(StringifyTag.LIST_LIMIT_LINE_LENGTH);
                boolean isLimitListSize = tags != null && Arrays.asList(tags).contains(StringifyTag.LIST_LIMIT_LIST_SIZE);
                int lineLengthLimited = 6;// 限制單行的顯示長度為6個單位
                int listSizeLimited = 6;// 限制最多顯示行數為6行
                int i = 0;
                for (Object line : list) {
                    String lineStr = (String) line;
                    if (isLimitLineLength && lineStr.length() > lineLengthLimited) {
                        lineStr = lineStr.substring(0, lineLengthLimited);
                        if (lineStr.charAt(lineLengthLimited - 1) == '§') {
                            lineStr = lineStr.substring(0, lineLengthLimited - 1);
                        }
                        stringBuilder.append(lineStr);
                        stringBuilder.append("..");
                    } else {
                        stringBuilder.append(lineStr);
                    }
                    stringBuilder.append(" ");
                    i++;
                    if (isLimitListSize && i >= listSizeLimited && list.size() > i) {
                        // 還有沒顯示的行，則不再顯示，用省略號代替
                        stringBuilder.append("...");
                        break;
                    }
                }
                return MsgBuilder.get(Msg.STRING_LIST, commandSender,
                        "" + list.size(), stringBuilder.toString());
            }
            if (!list.isEmpty() && list.get(0) instanceof BaseGamerData) {
                return list.stream()
                        .map(gamerData -> ((BaseGamerData) gamerData).getPlayer().getName())
                        .collect(Collectors.joining(","));
            }
        }
        return obj.toString();
    }

    public enum StringifyTag {
        LIST_LIMIT_LINE_LENGTH,// 限制單行長度
        LIST_LIMIT_LIST_SIZE,// 限制列表行數
    }

    public static boolean isColorCode(char ch) {
        return "1234567890abcdef".indexOf(ch) != -1;
    }

    public static String getProgressBarString(int symbolNumber, String symbol, char completedPartColorCode, char remainingPartColorCode, double completedPartValue, double remainingPartValue) {
        if (!isColorCode(completedPartColorCode) || !isColorCode(remainingPartColorCode)) {
            return "§7Invalid Color Code";
        }
        double proportion = completedPartValue / (completedPartValue + remainingPartValue);
        int completedPartSymbolNumber = (int) (proportion * symbolNumber);
        int remainingPartSymbolNumber = symbolNumber - completedPartSymbolNumber;
        StringBuilder stringBuilder = new StringBuilder(completedPartValue != 0 ? "§" + completedPartColorCode : "");
        for (int i = 0; i < symbolNumber; i++) {
            if (i == symbolNumber - remainingPartSymbolNumber) stringBuilder.append("§").append(remainingPartColorCode);
            stringBuilder.append(symbol);
        }
        return stringBuilder.toString();
    }
}
