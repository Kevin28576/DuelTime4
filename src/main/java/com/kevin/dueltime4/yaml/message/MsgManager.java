package com.kevin.dueltime4.yaml.message;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MsgManager {
    private static final String[] PRESET_FILE_NAMES = {
            "zh_tw.yml",
            "zh_cn.yml",
            "en_us.yml",
            "ja_jp.yml",
            "ko_kr.yml",
            "fr_fr.yml",
            "de_de.yml",
            "es_es.yml"
    };

    protected static final Map<String, YamlConfiguration> languageYamlFileMap = new HashMap<>();

    public MsgManager() {
        check();
        reload();
        MsgBuilder.prefix = DuelTimePlugin.getInstance().getCfgManager().getPrefix();
    }

    public Map<String, YamlConfiguration> getLanguageYamlFileMap() {
        return languageYamlFileMap;
    }

    public void updatePrefix(String prefix) {
        MsgBuilder.prefix = prefix;
    }

    public void check() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        File targetLanguageFolder = new File(plugin.getDataFolder(), "languages");

        if (!targetLanguageFolder.exists() && !targetLanguageFolder.mkdirs()) {
            return;
        }

        for (String fileName : PRESET_FILE_NAMES) {
            if (plugin.getResource("languages/" + fileName) == null) {
                continue;
            }

            File languageFile = new File(targetLanguageFolder, fileName);
            if (!languageFile.exists()) {
                plugin.saveResource("languages/" + fileName, false);
                continue;
            }

            YamlConfiguration targetYaml;
            try (InputStreamReader targetReader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8)) {
                targetYaml = YamlConfiguration.loadConfiguration(targetReader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            YamlConfiguration originalYaml;
            try (InputStream inputStream = plugin.getResource("languages/" + fileName)) {
                if (inputStream == null) {
                    continue;
                }
                originalYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Set<String> originalKeys = originalYaml.getKeys(true);
            boolean changed = false;
            for (String key : originalKeys) {
                if (!targetYaml.contains(key)) {
                    targetYaml.set(key, originalYaml.get(key));
                    changed = true;
                    plugin.getLogger().info("[DT-Lang] Added missing key '" + key + "' in " + fileName);
                }
            }

            if (changed) {
                try {
                    targetYaml.save(languageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void reload() {
        File languageFolder = new File(DuelTimePlugin.getInstance().getDataFolder(), "languages");
        if (!languageFolder.exists()) {
            return;
        }
        languageYamlFileMap.clear();
        File[] installedLanguageFiles = languageFolder.listFiles();
        if (installedLanguageFiles == null) {
            return;
        }
        for (File installedLanguageFile : installedLanguageFiles) {
            String fileName = installedLanguageFile.getName();
            if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
                continue;
            }
            fileName = fileName.substring(0, fileName.length() - (fileName.endsWith(".yml") ? 4 : 5));
            languageYamlFileMap.put(fileName, YamlConfiguration.loadConfiguration(installedLanguageFile));
        }
    }
}
