package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.network.UpdateManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public class CMDUpdate extends SubCommand {
    public CMDUpdate() {
        super("update", "upd");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        UpdateManager updateManager = DuelTimePlugin.getInstance().getUpdateManager();
        switch (sub) {
            case "check", "c" -> updateManager.checkNow(sender);
            case "download", "dl", "d" -> updateManager.downloadNow(sender);
            case "status", "s", "stat" -> updateManager.sendStatus(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        DynamicLang.send(sender, true,
                "Dynamic.updater.usage-check",
                "&c用法：/{label} update check",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.updater.usage-download",
                "&c用法：/{label} update download",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.updater.usage-status",
                "&c用法：/{label} update status",
                "label", label);
    }
}
