package com.kevin.dueltime4.yaml.message;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.viaversion.ViaVersion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示語構建器
 * 用來獲取/直接傳送某個處理後的提示語
 */
public class MsgBuilder {
    private static final String BUILTIN_DEFAULT_LANGUAGE = "zh_tw";
    protected static String prefix;

    private static String getLanguage(CommandSender sender) {
        String language = DuelTimePlugin.getInstance().getCfgManager().getDefaultLanguage();
        if (sender instanceof Player) {
            String languageUsed = null;
            if (DuelTimePlugin.getInstance().getCacheManager() != null
                    && DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache() != null
                    && DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache().get(sender.getName()) != null) {
                languageUsed = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache().get(sender.getName()).getLanguage();
            }
            if (languageUsed != null && MsgManager.languageYamlFileMap.containsKey(languageUsed)) {
                language = languageUsed;
            }
        }
        return language;
    }

    private static YamlConfiguration getLanguageConfig(CommandSender sender) {
        String language = getLanguage(sender);
        if (language == null) {
            return null;
        }
        return MsgManager.languageYamlFileMap.get(language);
    }

    private static YamlConfiguration getBuiltInDefaultLanguageConfig() {
        return MsgManager.languageYamlFileMap.get(BUILTIN_DEFAULT_LANGUAGE);
    }

    private static String getResolvedSingleMessage(Msg msg, YamlConfiguration languageConfig) {
        if (languageConfig != null) {
            String message = languageConfig.getString(msg.getKey());
            if (message != null) {
                return message;
            }
            List<String> messages = languageConfig.getStringList(msg.getKey());
            if (!messages.isEmpty()) {
                return messages.get(0);
            }
        }

        YamlConfiguration builtInDefaultConfig = getBuiltInDefaultLanguageConfig();
        if (builtInDefaultConfig != null) {
            String message = builtInDefaultConfig.getString(msg.getKey());
            if (message != null) {
                return message;
            }
            List<String> messages = builtInDefaultConfig.getStringList(msg.getKey());
            if (!messages.isEmpty()) {
                return messages.get(0);
            }
        }

        return msg.getDefaultMessage();
    }

    private static List<String> getMessagesFromConfig(YamlConfiguration config, String key) {
        if (config == null) {
            return null;
        }
        List<String> messages = config.getStringList(key);
        if (!messages.isEmpty()) {
            return new ArrayList<>(messages);
        }
        String singleMessage = config.getString(key);
        if (singleMessage != null) {
            return Collections.singletonList(singleMessage);
        }
        return null;
    }

    private static List<String> getResolvedMessages(Msg msg, YamlConfiguration languageConfig) {
        List<String> messages = getMessagesFromConfig(languageConfig, msg.getKey());
        if (messages != null) {
            return messages;
        }

        messages = getMessagesFromConfig(getBuiltInDefaultLanguageConfig(), msg.getKey());
        if (messages != null) {
            return messages;
        }

        return new ArrayList<>(Arrays.asList(msg.getDefaultMessages()));
    }

    /**
     * 完整方法：獲取單條提示語
     *
     * @param msg       提示語
     * @param sender    接收的玩家或後臺
     * @param hasPrefix 是否需要字首（載入訊息最前面）
     * @param replacers 變數替換者，依次替換掉 Msg 中已確立的變數
     * @return 最終的單條提示語
     */
    public static String get(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        // 獲取玩家所使用的語言檔案，如果是後臺則使用預設語言
        YamlConfiguration languageConfig = getLanguageConfig(sender);
        // 獲取該語言檔案下相應的提示語，如果沒有則優先使用 zh_tw.yml 內建預設，再回退到 Msg.java 內建字串
        String message = getResolvedSingleMessage(msg, languageConfig);
        // 根據需要為訊息新增字首
        if (hasPrefix) {
            message = prefix + message;
        }
        // 替換顏色符號
        message = message.replace("&", "§");
        // 執行變數替換
        String[] variables = msg.getVariable();
        for (int i = 0; i < variables.length; i++) {
            message = message.replace("{" + variables[i] + "}", replacers[i]);
        }
        // 返回最終的提示語
        return message;
    }

    /**
     * 快捷方法：獲取單條提示語（無字首、可選變數）
     */
    public static String get(Msg msg, CommandSender sender, String... replacers) {
        return get(msg, sender, false, replacers);
    }

    /**
     * 完整方法：傳送單條提示語
     */
    public static void send(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        CommandSender target = sender != null ? sender : Bukkit.getConsoleSender();
        target.sendMessage(get(msg, sender, hasPrefix, replacers));
    }

    /**
     * 快捷方法：傳送單條提示語（有字首、可選變數）
     */
    public static void send(Msg msg, CommandSender sender, String... replacers) {
        CommandSender target = sender != null ? sender : Bukkit.getConsoleSender();
        target.sendMessage(get(msg, sender, true, replacers));
    }

    /**
     * 完整方法：獲取多條提示語
     *
     * @param msg       提示語
     * @param sender    接收的玩家或後臺
     * @param hasPrefix 是否需要字首（加在第一行）
     * @param replacers 變數替換者，依次替換掉 Msg 中已確立的變數
     * @return 最終的提示語集合
     */
    public static List<String> gets(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        // 獲取玩家所使用的語言檔案，如果是後臺則使用預設語言
        YamlConfiguration languageConfig = getLanguageConfig(sender);
        // 獲取該語言檔案下相應的提示語，若缺失則優先回退到 zh_tw.yml
        List<String> messages = getResolvedMessages(msg, languageConfig);
        if (hasPrefix) {
            List<String> messagesWithPrefix = new ArrayList<>();
            messagesWithPrefix.add(prefix);
            messagesWithPrefix.addAll(messages);
            messages = messagesWithPrefix;
        }
        List<String> messagesReplaced = new ArrayList<>();
        // 替換顏色符號與變數
        for (String message : messages) {
            // 執行變數替換
            String[] variables = msg.getVariable();
            for (int i = 0; i < variables.length; i++) {
                message = replace(message, variables[i], replacers[i]);
            }
            // 執行顏色符號替換
            message = message.replace("&", "§");
            messagesReplaced.add(message);
        }
        // 返回最終的提示語
        return messagesReplaced;
    }

    /**
     * 快捷方法：獲取多條提示語（無字首、可選變數）
     */
    public static List<String> gets(Msg msg, CommandSender sender, String... replacers) {
        return gets(msg, sender, false, replacers);
    }

    /**
     * 完整方法：傳送多條提示語
     */
    public static void sends(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        for (String message : gets(msg, sender, hasPrefix, replacers)) {
            sender.sendMessage(message);
        }
    }

    /**
     * 快捷方法：傳送多條提示語（無字首、可選變數）
     */
    public static void sends(Msg msg, CommandSender sender, String... replacers) {
        for (String message : gets(msg, sender, false, replacers)) {
            sender.sendMessage(message);
        }
    }

    /**
     * 完整方法：獲取單條/多條含有可點選文字的提示語的TextComponent
     *
     * @param msg       提示語
     * @param sender    接收的玩家或後臺
     * @param hasPrefix 是否需要字首（若是，則根據提示語分類討論新增）
     * @param replacers 變數替換者，依次替換掉Msg中已確立的變數
     * @return 處理後的TextComponent集合
     */
    public static List<TextComponent> getClickable(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        YamlConfiguration languageConfig = getLanguageConfig(sender);
        // 獲取單行化的字串，如果本來就是單行就直接處理，反之則加上換行符[line]後合併成單行進行處理
        List<String> messagesList = getResolvedMessages(msg, languageConfig);
        String singleMessage = messagesList.get(0);
        boolean isMultiple = messagesList.size() > 1;
        if (isMultiple) {
            StringBuilder stringBuilder = new StringBuilder(messagesList.get(0));
            for (int i = 1; i < messagesList.size(); i++) {
                stringBuilder.append("[line]").append(messagesList.get(i));
            }
            singleMessage = stringBuilder.toString();
        }

        // 執行變數替換，這裡的變數替換就不只替換Msg提示語內容的變數了，還要替換掉懸浮文字、執行指令內容、建議指令內容裡的變數。千萬注意：這裡要深複製ClickableSettingsList，直接clone仍會影響到原物件
        String[][] internalSettingsListRaw = msg.getClickableSettingsList().clone();
        String[][] internalSettingsListReplaced = new String[internalSettingsListRaw.length][];
        for (int i = 0; i < internalSettingsListRaw.length; i++) {
            internalSettingsListReplaced[i] = Arrays.copyOf(internalSettingsListRaw[i], internalSettingsListRaw[i].length);
        }
        String[] variables = msg.getVariable();
        for (int i = 0; i < variables.length; i++) {
            singleMessage = replace(singleMessage, variables[i], replacers[i]);
            for (int i2 = 0; i2 < internalSettingsListReplaced.length; i2++) {
                for (int i3 = 0; i3 < 4; i3++) {
                    internalSettingsListReplaced[i2][i3] = replace(internalSettingsListReplaced[i2][i3], variables[i], replacers[i]);
                }
            }
        }
        // 執行顏色符號替換
        singleMessage = singleMessage.replace("&", "§");
        // 定義正規表示式，非貪婪式匹配所有被[clickable]包圍起來的內容（即可點選文字的設定內容），供後續識別
        String pattern = "\\[clickable](.*?)\\[clickable]";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(singleMessage);
        // 收集所有匹配到的內容
        List<String> clickablePlaceHolderFound = new ArrayList<>();
        while (matcher.find()) {
            String matchText = matcher.group(1);
            // 先收集匹配到的內容
            clickablePlaceHolderFound.add(matchText);
            // 操作原message，使匹配到的內容替換為統一的標識，方便後續以[split]為分割符”打碎“原message
            singleMessage = singleMessage.replace("[clickable]" + matchText + "[clickable]", "[split][clickable][split]");
        }
        // “打碎”原message，”碎片“為普通文字、包含換行符的文字、點選文字的設定內容文字
        String[] messageClips = singleMessage.split("\\[split]");
        // 建立一個TextComponent集合，一個元素代表一行
        List<TextComponent> textComponents = new ArrayList<>();
        TextComponent nowTextComponent;
        // 按需要分類討論新增字首
        if (hasPrefix) {
            if (isMultiple) {
                textComponents.add(new TextComponent(prefix));
                nowTextComponent = new TextComponent("");
            } else {
                nowTextComponent = new TextComponent(prefix);
            }
        } else {
            nowTextComponent = new TextComponent("");
        }
        /*
        開始將文字碎片轉化為TextComponent
        整體邏輯：
        遇到[clickable]時，按照原設定內容配置可點選文字
        遇到[line]時，換行，將當前操作的TextComponent新增到集合中，表示一行編輯完畢
        未遇到上述時，直接追加TextComponent
         */
        int nowClickablePlaceHolderIndex = 0;
        for (String messageClip : messageClips) {
            TextComponent textComponent;
            if (messageClip.equals("[clickable]")) {
                /*
                檢測為可點選文字的佔位符，開始解析原本匹配到的設定內容
                設定內容格式：佔位符名（外掛內定，必須）+分隔符+直接展示的文字內容（必須）+分隔符+觸發滑鼠懸浮事件後展示的文字內容（非必須）
                這裡的分隔符為連續的兩個英文冒號":"，即"::"
                 */
                String originalSettingsContent = clickablePlaceHolderFound.get(nowClickablePlaceHolderIndex);
                String[] settings = originalSettingsContent.split("::");
                /*
                篩選出配置項設定夠的設定內容
                多餘的內容會被無視
                 */
                if (settings.length >= 2) {
                    String clickableTextPlaceHolderType = settings[0];
                    // 根據設定內容的第一項的佔位符名獲取其對應的內定設定內容
                    String[] settingsMatched = null;
                    for (String[] internalSettings : internalSettingsListReplaced) {
                        if (clickableTextPlaceHolderType.equals(internalSettings[0])) {
                            settingsMatched = internalSettings;
                            break;
                        }
                    }
                    // 如果有效則進一步配置這個可點選文字
                    if (settingsMatched != null) {
                        String clickableTextDisplayed = settings[1];
                        textComponent = new TextComponent(clickableTextDisplayed);
                        // 檢測是否設定了可選項“懸浮文字”
                        if (settings.length >= 3) {
                            String clickableTextHovered = settings[2];
                            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(clickableTextHovered.replace("||", "\n")).create()));
                        }
                        // 檢測內定配置中是否設定了可選項“點選後執行的命令”
                        if (!settingsMatched[1].isEmpty()) {
                            // 這裡使用了正規表示式清除所有顏色符號
                            String clickableTextRunCommand = settingsMatched[1].replaceAll("§[0-9a-fA-Fk-oK-OrR]", "");
                            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickableTextRunCommand));
                        }
                        // 檢測內定配置中是否設定了可選項“點選後建議的命令”
                        if (!settingsMatched[2].isEmpty()) {
                            // 這裡使用了正規表示式清除所有顏色符號
                            String clickableTextSuggestCommand = settingsMatched[2].replaceAll("§[0-9a-fA-Fk-oK-OrR]", "");
                            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, clickableTextSuggestCommand));
                        }
                        // 檢測內定配置中是否設定了可選項“點選後開啟的URL”
                        if (!settingsMatched[3].isEmpty()) {
                            String clickableTextOpenURL = settingsMatched[3];
                            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickableTextOpenURL));
                        }
                        // 完成本行文字追加
                        nowTextComponent.addExtra(textComponent);
                        // 佔位符序號自加，讓下次找到[clickable]佔位符後，可以替換為對應的內容
                        nowClickablePlaceHolderIndex++;
                        continue;
                    }
                }
            }
            /*
            如果遇到的不是佔位符，或者是佔位符但在解析過程中出現不符合條件的情況，視為非設定內容
            接著再判斷碎片是否包含換行符
             */
            if (messageClip.contains("[line]")) {
                // 檢測含有一個或多個換行符，則以[line]為分隔符打碎，將第一個碎片加到本行，接著以後每一個碎片（除了最後一個）單獨成行並新增到textComponents中，最後一個碎片繼任nowTextComponent
                String[] lineMessageClips = messageClip.split("\\[line]");
                for (int i = 0; i < lineMessageClips.length; i++) {
                    String lineMessageClip = lineMessageClips[i];
                    if (i == 0) {
                        nowTextComponent.addExtra(new TextComponent(lineMessageClip));
                        textComponents.add(nowTextComponent);
                    } else if (i == lineMessageClips.length - 1) {
                        nowTextComponent = new TextComponent(lineMessageClip);
                    } else {
                        textComponents.add(new TextComponent(lineMessageClip));
                    }
                }
                continue;
            }
            // 若也不是換行符，則視為普通文字，繼續在本行執行文字追加
            nowTextComponent.addExtra(new TextComponent(messageClip));
        }
        // 把最後一次編輯的行對應的TextComponent新增到集合中
        textComponents.add(nowTextComponent);
        return textComponents;
    }

    public static void broadcast(Msg msg, boolean prefix, String... replacers) {
        for (Player player : ViaVersion.getOnlinePlayers()) {
            send(msg, player, prefix, replacers);
        }
        Bukkit.getConsoleSender().sendMessage(get(msg, null, prefix, replacers));
    }

    /**
     * 完整方法：傳送Title（根據Msg型別）
     */
    public static void sendTitle(Msg titleMsg, Msg subTitleMsg, int fadeIn, int stay, int fadeOut, Player player, ViaVersion.TitleType titleTypeAsMessage, String... replacers) {
        if (player != null) {
            ViaVersion.sendTitle(player, get(titleMsg, player, replacers), get(subTitleMsg, player, replacers), fadeIn, stay, fadeOut, titleTypeAsMessage);
        }
    }

    /**
     * 快捷方法：傳送Title（根據Msg型別），預設若伺服器為1.8以下版本則不傳送
     */
    public static void sendTitle(Msg titleMsg, Msg subTitleMsg, int fadeIn, int stay, int fadeOut, Player player, String... replacers) {
        if (player != null) {
            sendTitle(titleMsg, subTitleMsg, fadeIn, stay, fadeOut, player, null, replacers);
        }
    }

    /**
     * 完整方法：傳送Title（根據String型別）
     */
    public static void sendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut, Player player, ViaVersion.TitleType titleTypeAsMessage) {
        if (player != null) {
            ViaVersion.sendTitle(player, title, subTitle, fadeIn, stay, fadeOut, titleTypeAsMessage);
        }
    }

    /**
     * 快捷方法：傳送Title（根據String型別），預設若伺服器為1.8以下版本則不傳送
     */
    public static void sendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut, Player player) {
        if (player != null) {
            sendTitle(title, subTitle, fadeIn, stay, fadeOut, player, null);
        }
    }

    /**
     * 完整方法：傳送ActionBar（根據Msg型別）
     */
    public static void sendActionBar(Msg actionBarMsg, Player player, boolean considerLowVersion, String... replacers) {
        if (player != null) {
            ViaVersion.sendActionBar(player, get(actionBarMsg, player, replacers), considerLowVersion);
        }
    }

    /**
     * 快捷方法：傳送ActionBar（根據Msg型別），預設若伺服器為1.8以下版本則不傳送
     */
    public static void sendActionBar(Msg actionBarMsg, Player player, String... replacers) {
        if (player != null) {
            ViaVersion.sendActionBar(player, get(actionBarMsg, player, replacers), false);
        }
    }

    /**
     * 完整方法：傳送ActionBar（根據String型別）
     */
    public static void sendActionBar(String actionBar, Player player, boolean considerLowVersion) {
        if (player != null) {
            ViaVersion.sendActionBar(player, actionBar, considerLowVersion);
        }
    }

    /**
     * 快捷方法：傳送ActionBar（根據String型別），預設若伺服器為1.8以下版本則不傳送
     */
    public static void sendActionBar(String actionBar, Player player) {
        if (player != null) {
            ViaVersion.sendActionBar(player, actionBar, false);
        }
    }

    /**
     * 完整方法：傳送含有可點選文字的單條提示語（如果接收方是後臺，那也能確保能正常展示）
     */
    public static void sendClickable(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        TextComponent textComponent = getClickable(msg, sender, hasPrefix, replacers).get(0);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(textComponent);
        } else {
            sender.sendMessage(textComponent.toPlainText());
        }
    }

    /**
     * 完整方法：傳送含有可點選文字的多條提示語
     */
    public static void sendsClickable(Msg msg, CommandSender sender, boolean hasPrefix, String... replacers) {
        List<TextComponent> textComponents = getClickable(msg, sender, hasPrefix, replacers);
        for (TextComponent textComponent : textComponents) {
            if (sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(textComponent);
            } else {
                sender.sendMessage(textComponent.toPlainText());
            }
        }
    }

    /**
     * 替換佔位符的內容，尤其是特殊佔位符（進度條等）
     *
     * @param message  原字串
     * @param variable 變數名（可能需要特殊處理，如進度條傳入的包括變數名id和一些屬性的定義）
     * @param replacer 傳入的替換內容（可能需要特殊處理，如進度條傳入的替換內容則是一組double型資料）
     * @return 替換後的字串
     */
    private static String replace(String message, String variable, String replacer) {
        if (variable.startsWith("progress_bar_") && message.contains("progress_bar_")) {
            // 特殊變數：進度條
            try {
                String id = message.split("_")[2];
                Matcher matcher = Pattern.compile("(\\{progress_bar_" + id + "_.*?\\})").matcher(message);
                String variableFound = null;
                while (matcher.find()) {
                    variableFound = matcher.group(1);
                }
                if (variableFound == null) return message;
                String[] splits = variableFound.split("_");
                int number = Integer.parseInt(splits[3]);
                String symbol = splits[4];
                char completedPartColorCode = splits[5].charAt(0);
                char remainingPartColorCode = splits[6].charAt(0);
                double completedValue = Double.parseDouble(replacer.split(" ")[0]);
                double remainingValue = Double.parseDouble(replacer.split(" ")[1]);
                return message.replace(variableFound, UtilFormat.getProgressBarString(number, symbol, completedPartColorCode, remainingPartColorCode, completedValue, remainingValue));
            } catch (Exception e) {
                e.printStackTrace();
                return message;
            }
        }
        // 常規替換
        return message.replace("{" + variable + "}", replacer);
    }
}
