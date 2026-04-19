package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CMDBalance extends SubCommand {

    private static final Map<String, Setting> SETTINGS = new LinkedHashMap<>();

    static {
        SETTINGS.put("win-exp", new Setting("Arena.classic.reward.win-exp", SettingType.DOUBLE, 0D, null));
        SETTINGS.put("win-point", new Setting("Arena.classic.reward.win-point", SettingType.DOUBLE, 0D, null));
        SETTINGS.put("lose-exp-rate", new Setting("Arena.classic.reward.lose-exp-rate", SettingType.DOUBLE, 0D, 1D));
        SETTINGS.put("confirm-timeout", new Setting("Arena.classic.matchmaking.confirm-timeout", SettingType.INTEGER, 5D, null));
        SETTINGS.put("watchdog-enabled", new Setting("Arena.classic.matchmaking.watchdog.enabled", SettingType.BOOLEAN, null, null));
        SETTINGS.put("watchdog-interval-seconds", new Setting("Arena.classic.matchmaking.watchdog.interval-seconds", SettingType.INTEGER, 1D, null));
        SETTINGS.put("watchdog-cleanup-offline-players", new Setting("Arena.classic.matchmaking.watchdog.cleanup-offline-players", SettingType.BOOLEAN, null, null));
        SETTINGS.put("watchdog-cleanup-invalid-arena", new Setting("Arena.classic.matchmaking.watchdog.cleanup-invalid-arena", SettingType.BOOLEAN, null, null));
        SETTINGS.put("watchdog-trigger-match-check", new Setting("Arena.classic.matchmaking.watchdog.trigger-match-check", SettingType.BOOLEAN, null, null));
        SETTINGS.put("streak-enabled", new Setting("Arena.classic.streak.enabled", SettingType.BOOLEAN, null, null));
        SETTINGS.put("streak-show-message", new Setting("Arena.classic.streak.show-message", SettingType.BOOLEAN, null, null));
        SETTINGS.put("streak-reset-on-draw", new Setting("Arena.classic.streak.reset-on-draw", SettingType.BOOLEAN, null, null));
        SETTINGS.put("queue-sound-enabled", new Setting("Arena.classic.matchmaking.queue-sound.enabled", SettingType.BOOLEAN, null, null));
        SETTINGS.put("queue-sound-allow-player-toggle", new Setting("Arena.classic.matchmaking.queue-sound.allow-player-toggle", SettingType.BOOLEAN, null, null));
        SETTINGS.put("queue-sound-interval-seconds", new Setting("Arena.classic.matchmaking.queue-sound.interval-seconds", SettingType.INTEGER, 1D, null));
        SETTINGS.put("queue-sound-volume", new Setting("Arena.classic.matchmaking.queue-sound.volume", SettingType.DOUBLE, 0D, 5D));
        SETTINGS.put("queue-sound-pitch", new Setting("Arena.classic.matchmaking.queue-sound.pitch", SettingType.DOUBLE, 0.1D, 2D));
        SETTINGS.put("leave-penalty-enabled", new Setting("Arena.classic.matchmaking.leave-penalty.enabled", SettingType.BOOLEAN, null, null));
        SETTINGS.put("leave-penalty-apply-on-quit-command", new Setting("Arena.classic.matchmaking.leave-penalty.apply-on-quit-command", SettingType.BOOLEAN, null, null));
        SETTINGS.put("leave-penalty-apply-on-disconnect", new Setting("Arena.classic.matchmaking.leave-penalty.apply-on-disconnect", SettingType.BOOLEAN, null, null));
        SETTINGS.put("leave-penalty-apply-point-deduction", new Setting("Arena.classic.matchmaking.leave-penalty.apply-point-deduction", SettingType.BOOLEAN, null, null));
        SETTINGS.put("leave-penalty-apply-queue-cooldown", new Setting("Arena.classic.matchmaking.leave-penalty.apply-queue-cooldown", SettingType.BOOLEAN, null, null));
        SETTINGS.put("leave-penalty-point", new Setting("Arena.classic.matchmaking.leave-penalty.point", SettingType.DOUBLE, 0D, null));
        SETTINGS.put("leave-penalty-cooldown", new Setting("Arena.classic.matchmaking.leave-penalty.cooldown", SettingType.INTEGER, 0D, null));
        SETTINGS.put("restart-protection-enabled", new Setting("System.restart-protection.enabled", SettingType.BOOLEAN, null, null));
        SETTINGS.put("restart-protection-broadcast-message", new Setting("System.restart-protection.broadcast-message", SettingType.BOOLEAN, null, null));
    }

    public CMDBalance() {
        super("balance", "bal");
    }

    public static String[] getSettingKeys() {
        return SETTINGS.keySet().toArray(String[]::new);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }

        if (args.length == 1) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        if (isAlias(sub, "view", "v", "list", "ls")) {
            handleView(sender, label, args);
            return true;
        }
        if (isAlias(sub, "set", "s")) {
            handleSet(sender, label, args);
            return true;
        }
        if (isAlias(sub, "config", "cfg", "c")) {
            handleConfig(sender, label, args);
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private void handleView(CommandSender sender, String label, String[] args) {
        String targetName;
        if (args.length >= 3) {
            targetName = args[2];
        } else if (sender instanceof Player) {
            targetName = sender.getName();
        } else {
            DynamicLang.send(sender, true,
                    "Dynamic.balance.usage-main-view",
                    "&cUsage: /{label} balance view <player>",
                    "label", label);
            return;
        }

        PlayerDataCache cache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
        PlayerData playerData = cache.getAnyway(targetName);
        if (playerData == null) {
            MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender, targetName);
            return;
        }

        DynamicLang.send(sender, true,
                "Dynamic.balance.panel-separator",
                "&7&m---------------------------");
        DynamicLang.send(sender, true,
                "Dynamic.balance.player-title",
                "&aBalance &7- &f{player}",
                "player", targetName);
        DynamicLang.send(sender, true,
                "Dynamic.balance.player-value",
                "&7Point: &f{value}",
                "value", String.valueOf(UtilFormat.round(playerData.getPoint(), 2)));
        DynamicLang.send(sender, true,
                "Dynamic.balance.panel-separator",
                "&7&m---------------------------");
    }

    private void handleSet(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            DynamicLang.send(sender, true,
                    "Dynamic.balance.usage-main-set",
                    "&cUsage: /{label} balance set <player> <value>",
                    "label", label);
            return;
        }

        String targetName = args[2];
        String valueInput = args[3];
        if (!UtilFormat.isDouble(valueInput)) {
            MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, sender, valueInput);
            return;
        }

        double value = Double.parseDouble(valueInput);
        if (value < 0) {
            MsgBuilder.send(Msg.ERROR_VALUE_IS_NEGATIVE, sender, valueInput);
            return;
        }

        PlayerDataCache cache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
        PlayerData playerData = cache.getAnyway(targetName);
        if (playerData == null) {
            MsgBuilder.send(Msg.ERROR_PLAYER_NO_FOUND, sender, targetName);
            return;
        }

        double oldValue = playerData.getPoint();
        playerData.setPoint(value);
        cache.set(targetName, playerData);

        DynamicLang.send(sender, true,
                "Dynamic.balance.set-success",
                "&aUpdated player &f{player} &abalance: &f{old} &7-> &f{new}",
                "player", targetName,
                "old", stripTrailingZero(oldValue),
                "new", stripTrailingZero(value));
    }

    private void handleConfig(CommandSender sender, String label, String[] args) {
        if (args.length == 2 || isAlias(args[2], "view", "v", "list", "ls")) {
            sendConfigPanel(sender, label);
            return;
        }

        if (!isAlias(args[2], "set", "s")) {
            DynamicLang.send(sender, true,
                    "Dynamic.balance.usage-config-view",
                    "&cUsage: /{label} balance config view",
                    "label", label);
            DynamicLang.send(sender, true,
                    "Dynamic.balance.usage-config-set",
                    "&cUsage: /{label} balance config set <key> <value>",
                    "label", label);
            return;
        }

        if (args.length < 5) {
            DynamicLang.send(sender, true,
                    "Dynamic.balance.usage-config-set",
                    "&cUsage: /{label} balance config set <key> <value>",
                    "label", label);
            return;
        }

        String key = args[3].toLowerCase(Locale.ROOT);
        Setting setting = SETTINGS.get(key);
        if (setting == null) {
            DynamicLang.send(sender, true,
                    "Dynamic.balance.config-unknown-key",
                    "&cUnknown key: &f{key}",
                    "key", args[3]);
            DynamicLang.send(sender, true,
                    "Dynamic.balance.config-available",
                    "&7Available: &f{keys}",
                    "keys", String.join(", ", SETTINGS.keySet()));
            return;
        }

        String valueInput = args[4];
        Object oldValue = DuelTimePlugin.getInstance().getCfgManager().getConfig().get(setting.path);
        Object newValue;
        switch (setting.type) {
            case BOOLEAN -> {
                Boolean parsedBoolean = parseBoolean(valueInput);
                if (parsedBoolean == null) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-boolean-required",
                            "&cValue must be true/false.");
                    return;
                }
                newValue = parsedBoolean;
            }
            case INTEGER -> {
                if (!UtilFormat.isInt(valueInput)) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-integer-required",
                            "&cValue must be an integer.");
                    return;
                }
                int parsed = Integer.parseInt(valueInput);
                if (setting.min != null && parsed < setting.min) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-min-required",
                            "&cValue must be >= {value}",
                            "value", stripTrailingZero(setting.min));
                    return;
                }
                if (setting.max != null && parsed > setting.max) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-max-required",
                            "&cValue must be <= {value}",
                            "value", stripTrailingZero(setting.max));
                    return;
                }
                newValue = parsed;
            }
            case DOUBLE -> {
                if (!UtilFormat.isDouble(valueInput)) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-number-required",
                            "&cValue must be a number.");
                    return;
                }
                double parsed = Double.parseDouble(valueInput);
                if (setting.min != null && parsed < setting.min) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-min-required",
                            "&cValue must be >= {value}",
                            "value", stripTrailingZero(setting.min));
                    return;
                }
                if (setting.max != null && parsed > setting.max) {
                    DynamicLang.send(sender, true,
                            "Dynamic.balance.config-max-required",
                            "&cValue must be <= {value}",
                            "value", stripTrailingZero(setting.max));
                    return;
                }
                newValue = parsed;
            }
            default -> {
                return;
            }
        }

        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        cfgManager.getConfig().set(setting.path, newValue);
        cfgManager.save();
        cfgManager.reload();
        if (DuelTimePlugin.getInstance().getQueueWatchdogService() != null) {
            DuelTimePlugin.getInstance().getQueueWatchdogService().restartFromConfig();
        }

        DynamicLang.send(sender, true,
                "Dynamic.balance.config-updated",
                "&aUpdated &f{key} &a: &f{old} &7-> &f{new}",
                "key", key,
                "old", String.valueOf(oldValue),
                "new", String.valueOf(newValue));
    }

    private void sendConfigPanel(CommandSender sender, String label) {
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        DynamicLang.send(sender, true,
                "Dynamic.balance.panel-separator",
                "&7&m---------------------------");
        DynamicLang.send(sender, true,
                "Dynamic.balance.config-title",
                "&aBalance Config Panel");
        for (Map.Entry<String, Setting> entry : SETTINGS.entrySet()) {
            Object value = cfgManager.getConfig().get(entry.getValue().path);
            DynamicLang.send(sender, true,
                    "Dynamic.balance.config-item",
                    "&7{key}: &f{value}",
                    "key", entry.getKey(),
                    "value", String.valueOf(value));
        }
        DynamicLang.send(sender, true,
                "Dynamic.balance.config-hint",
                "&7Use: &f/{label} balance config set <key> <value>",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.balance.panel-separator",
                "&7&m---------------------------");
    }

    private void sendUsage(CommandSender sender, String label) {
        DynamicLang.send(sender, true,
                "Dynamic.balance.usage-main-view",
                "&cUsage: /{label} balance view <player>",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.balance.usage-main-set",
                "&cUsage: /{label} balance set <player> <value>",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.balance.usage-config-view",
                "&cUsage: /{label} balance config view",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.balance.usage-config-set",
                "&cUsage: /{label} balance config set <key> <value>",
                "label", label);
    }

    private boolean isAlias(String entered, String... aliases) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(entered)) {
                return true;
            }
        }
        return false;
    }

    private Boolean parseBoolean(String raw) {
        String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "true", "t", "yes", "y", "1", "on" -> true;
            case "false", "f", "no", "n", "0", "off" -> false;
            default -> null;
        };
    }

    private String stripTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private enum SettingType {
        BOOLEAN,
        INTEGER,
        DOUBLE
    }

    private static class Setting {
        private final String path;
        private final SettingType type;
        private final Double min;
        private final Double max;

        private Setting(String path, SettingType type, Double min, Double max) {
            this.path = path;
            this.type = type;
            this.min = min;
            this.max = max;
        }
    }
}
