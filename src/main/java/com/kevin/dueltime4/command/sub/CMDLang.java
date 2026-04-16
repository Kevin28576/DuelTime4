package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.util.UtilHelpList;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.stream.Collectors;

public class CMDLang extends SubCommand {

    public CMDLang() {
        super("lang", "language", "languages");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Map<String, YamlConfiguration> langMap = DuelTimePlugin.getInstance().getMsgManager().getLanguageYamlFileMap();
        if (args.length == 1) {
            if (langMap.isEmpty()) {
                MsgBuilder.send(Msg.COMMAND_SUB_LANG_NO_FILES_INSTALLED, sender);
                return true;
            }
            MsgBuilder.send(Msg.COMMAND_SUB_LANG_LIST_HEADING, sender);
            int index = 0;
            for (String fileName : langMap.keySet()) {
                index++;
                MsgBuilder.sendClickable(Msg.COMMAND_SUB_LANG_LIST_BODY, sender, false,
                        "" + index, fileName);
            }
            MsgBuilder.send(Msg.COMMAND_SUB_LANG_LIST_ENDING, sender,
                    "" + index);
            return true;
        }
        if (!(sender instanceof Player)) {
            MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
            return true;
        }
        String fileName = args[1];
        if (!langMap.containsKey(fileName)) {
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                fileName = fileName.substring(0, fileName.length() - (fileName.endsWith(".yml") ? 4 : 5));
                if (!langMap.containsKey(fileName)) {
                    MsgBuilder.send(Msg.COMMAND_SUB_LANG_THE_FILE_NOT_EXISTS, sender,
                            fileName);
                    UtilHelpList.sendSuggest(sender, 1,
                            langMap.keySet(), label, args);
                    return true;
                }
            }
        }
        PlayerDataCache cache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
        String playerName = sender.getName();
        PlayerData playerData = cache.get(playerName);
        playerData.setLanguage(fileName);
        cache.set(playerName, playerData);
        MsgBuilder.send(Msg.COMMAND_SUB_LANG_SWITCH_SUCCESSFULLY, sender,
                MsgBuilder.get(Msg.LANGUAGE_NAME, sender),
                fileName);
        return true;
    }
}
