package com.kevin.dueltime4.command;

import com.kevin.dueltime4.command.sub.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import java.util.HashSet;
import java.util.Set;

public class CommandHandler {

    private final Set<SubCommand> commands = new HashSet<>();

    public CommandHandler() {
        commands.add(new CMDAccept());
        commands.add(new CMDBalance());
        commands.add(new CMDAdminHelp());
        commands.add(new CMDArena());
        commands.add(new CMDBlacklist());
        commands.add(new CMDDecline());
        commands.add(new CMDHelp());
        commands.add(new CMDLang());
        commands.add(new CMDLevel());
        commands.add(new CMDPoint());
        commands.add(new CMDSend());
        commands.add(new CMDShop());
        commands.add(new CMDLobby());
        commands.add(new CMDRank());
        commands.add(new CMDRecord());
        commands.add(new CMDStats());
        commands.add(new CMDStart());
        commands.add(new CMDSpectate());
        commands.add(new CMDQuit());
        commands.add(new CMDJoin());
        commands.add(new CMDReload());
        commands.add(new CMDStop());
        commands.add(new CMDUpdate());
        commands.add(new CMDClick());
        PluginCommand pluginCommand = Bukkit.getPluginCommand("dueltime4");
        if (pluginCommand == null) {
            pluginCommand = Bukkit.getPluginCommand("dueltime");
        }
        if (pluginCommand != null) {
            CommandExecutor executor = new CommandExecutor(commands);
            pluginCommand.setExecutor(executor);
            pluginCommand.setTabCompleter(executor);
        }
    }

    public SubCommand getSubCommand(String commandEntered) {
        for (SubCommand subCommand : commands) {
            for (String alias : subCommand.getAliases()) {
                if (commandEntered.equalsIgnoreCase(alias)) {
                    return subCommand;
                }
            }
        }
        return null;
    }

    public Set<SubCommand> getCommands() {
        return commands;
    }
}
