package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.gui.simple.ItemDetailInventoryHolder;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CMDClick extends SubCommand {
    public CMDClick() {
        super("click");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (args.length != 2) {
            return true;
        }
        if (args[1].equalsIgnoreCase("item")) {
            ItemStack item = ItemDetailInventoryHolder.itemMap.getOrDefault(sender.getName(), null);
            if (item == null) {
                return true;
            }
            Inventory inv = Bukkit.createInventory(new ItemDetailInventoryHolder(), 9, MsgBuilder.get(Msg.GUI_TYPE_ITEM_DETAIL_TITLE, sender));
            inv.setItem(4, item);
            ((Player) sender).openInventory(inv);
            ItemDetailInventoryHolder.itemMap.remove(sender.getName());
            return true;
        }
        return true;
    }
}
