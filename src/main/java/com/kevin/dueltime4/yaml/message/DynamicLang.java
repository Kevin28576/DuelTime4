package com.kevin.dueltime4.yaml.message;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.data.pojo.PlayerData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

public final class DynamicLang {
    private static final String ENGLISH_LANGUAGE_KEY = "en_us";

    private DynamicLang() {
    }

    public static void send(CommandSender sender, boolean withPrefix, String key, String fallback, String... replacements) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(get(sender, withPrefix, key, fallback, replacements));
    }

    public static String get(CommandSender sender, boolean withPrefix, String key, String fallback, String... replacements) {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        if (plugin == null) {
            return colorize(applyReplacements(fallback, replacements));
        }

        String resolved = resolveMessage(plugin, sender, key, fallback);
        resolved = applyReplacements(resolved, replacements);
        if (withPrefix) {
            resolved = plugin.getCfgManager().getPrefix() + resolved;
        }
        return colorize(resolved);
    }

    public static String get(CommandSender sender, String key, String fallback, String... replacements) {
        return get(sender, false, key, fallback, replacements);
    }

    private static String resolveMessage(DuelTimePlugin plugin, CommandSender sender, String key, String fallback) {
        Map<String, YamlConfiguration> languageMap = plugin.getMsgManager().getLanguageYamlFileMap();
        YamlConfiguration languageConfig = resolveLanguageConfig(plugin, sender, languageMap);
        String message = getString(languageConfig, key);
        if (isBlank(message)) {
            message = getString(languageMap.get(ENGLISH_LANGUAGE_KEY), key);
        }
        if (isBlank(message)) {
            message = fallback;
        }
        return message == null ? "" : message;
    }

    private static YamlConfiguration resolveLanguageConfig(DuelTimePlugin plugin, CommandSender sender,
                                                           Map<String, YamlConfiguration> languageMap) {
        if (languageMap == null || languageMap.isEmpty()) {
            return null;
        }

        String language = plugin.getCfgManager().getDefaultLanguage();
        if (sender instanceof Player) {
            try {
                if (plugin.getCacheManager() != null && plugin.getCacheManager().getPlayerDataCache() != null) {
                    PlayerData playerData = plugin.getCacheManager().getPlayerDataCache().getAnyway(sender.getName());
                    if (playerData != null && playerData.getLanguage() != null && languageMap.containsKey(playerData.getLanguage())) {
                        language = playerData.getLanguage();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (language != null && languageMap.containsKey(language)) {
            return languageMap.get(language);
        }
        if (languageMap.containsKey(ENGLISH_LANGUAGE_KEY)) {
            return languageMap.get(ENGLISH_LANGUAGE_KEY);
        }
        return languageMap.values().stream().findFirst().orElse(null);
    }

    private static String getString(YamlConfiguration config, String key) {
        if (config == null || key == null || key.isEmpty()) {
            return null;
        }
        String message = config.getString(key);
        return isBlank(message) ? null : message;
    }

    private static String applyReplacements(String message, String... replacements) {
        if (message == null || replacements == null || replacements.length == 0) {
            return message == null ? "" : message;
        }
        String resolved = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            resolved = resolved.replace("{" + placeholder + "}", value == null ? "" : value);
        }
        return resolved;
    }

    private static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('&', '\u00A7').replace('\u79AE', '\u00A7');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
