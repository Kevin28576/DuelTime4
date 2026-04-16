package com.kevin.dueltime4.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public abstract class SubCommand {
    private final String[] aliases;
    
    public SubCommand(String... aliases) {
        this.aliases = aliases;
    }

    public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String[] args);

    public String[] getAliases() {
        return aliases;
    }
}
