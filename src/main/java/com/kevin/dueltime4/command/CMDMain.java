package com.kevin.dueltime4.command;

import com.kevin.dueltime4.command.sub.CommandPermission;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDMain {
    private static final String PROJECT_LINK = "https://github.com/kevin/DuelTime4";

    public static boolean onCommand(CommandSender sender, String label) {
        sender.sendMessage("");
        sender.sendMessage("§a§lDuel§2§l§oTime§2§l4 §fBy Kevin");
        sender.sendMessage("§7§m──§f§l§m───────────────────────§7§m──");
        sender.sendMessage("");
        sender.sendMessage("  §b/"+label+"§b help§3(h) §7§l- "+ MsgBuilder.get(Msg.COMMAND_MAIN_HELP,sender));
        if (sender.hasPermission(CommandPermission.ADMIN))
            sender.sendMessage("  §b/"+label+"§b adminhelp§3(ah) §7§l- "+ MsgBuilder.get(Msg.COMMAND_MAIN_ADMIN_HELP,sender));
        sender.sendMessage("");
        sender.sendMessage("§7§m──§f§l§m───────────────────────§7§m──");
        sendSite(sender);
        sender.sendMessage("");
        return true;
    }

    private static void sendSite(CommandSender sender) {
        TextComponent doc = new TextComponent();
        doc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://cloudxact.com/DuelTime4"));
        doc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("點擊開啟本插件的使用教學網站\nClick to open the doc link for this plugin").create()));
        String text1 = "§6[使用文擋 | Document]";
        doc.setText(text1);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(doc);
        } else {
            sender.sendMessage(text1);
        }
    }
}
