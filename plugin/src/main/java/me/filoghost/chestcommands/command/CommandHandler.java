/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.chestcommands.command;

import me.filoghost.chestcommands.ChestCommands;
import me.filoghost.chestcommands.Permissions;
import me.filoghost.chestcommands.menu.InternalMenu;
import me.filoghost.chestcommands.menu.MenuManager;
import me.filoghost.chestcommands.menucreator.MenuCreatorInventoryHolder;
import me.filoghost.chestcommands.util.Utils;
import me.filoghost.fcommons.command.CommandException;
import me.filoghost.fcommons.command.CommandValidate;
import me.filoghost.fcommons.command.annotation.Description;
import me.filoghost.fcommons.command.annotation.DisplayPriority;
import me.filoghost.fcommons.command.annotation.MinArgs;
import me.filoghost.fcommons.command.annotation.Name;
import me.filoghost.fcommons.command.annotation.Permission;
import me.filoghost.fcommons.command.annotation.UsageArgs;
import me.filoghost.fcommons.command.multi.MultiCommandManager;
import me.filoghost.fcommons.command.multi.SubCommand;
import me.filoghost.fcommons.command.multi.SubCommandSession;
import me.filoghost.fcommons.logging.ErrorCollector;
import me.filoghost.chestcommands.menucreator.MenuCreatorListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CommandHandler extends MultiCommandManager {

    private final MenuManager menuManager;

    public CommandHandler(MenuManager menuManager, String label) {
        super(label);
        this.menuManager = menuManager;
    }

    @Override
    protected String getSubCommandDefaultPermission(SubCommand subCommand) {
        return Permissions.COMMAND_PREFIX + "." + subCommand.getName();
    }

    @Override
    protected void sendNoArgsMessage(CommandSender sender, String rootCommandLabel) {
        sender.sendMessage(ChestCommands.CHAT_PREFIX);
        sender.sendMessage(ChatColor.GREEN + "Version: " + ChatColor.GRAY + ChestCommands.getPluginInstance().getDescription().getVersion());
        sender.sendMessage(ChatColor.GREEN + "Developer: " + ChatColor.GRAY + "filoghost");
        sender.sendMessage(ChatColor.GREEN + "Commands: " + ChatColor.GRAY + "/" + rootCommandLabel + " help");
    }

    @Override
    protected void sendUnknownSubCommandMessage(SubCommandSession session) {
        session.getSender().sendMessage(ChatColor.RED + "Unknown sub-command \"" + session.getSubLabelUsed() + "\". "
                + "Use \"/" + session.getRootLabelUsed() + " help\" to see available commands.");
    }

    @Name("help")
    @Permission(Permissions.COMMAND_PREFIX + "help")
    public void help(CommandSender sender, SubCommandSession session) {
        sender.sendMessage(ChestCommands.CHAT_PREFIX + "Commands:");
        for (SubCommand subCommand : getAllSubCommands()) {
            if (subCommand == session.getSubCommand()) {
                continue;
            }
            String usageText = getUsageText(session.getRootLabelUsed(), subCommand);
            sender.sendMessage(ChatColor.WHITE + usageText + ChatColor.GRAY + " - " + subCommand.getDescription());
        }
    }

    @Name("reload")
    @Description("Reloads the plugin.")
    @Permission(Permissions.COMMAND_PREFIX + "reload")
    @DisplayPriority(100)
    public void reload(CommandSender sender) {
        ChestCommands.closeAllMenus();

        ErrorCollector errorCollector = ChestCommands.load();

        if (!errorCollector.hasErrors()) {
            sender.sendMessage(ChestCommands.CHAT_PREFIX + "Plugin reloaded.");
        } else {
            errorCollector.logToConsole();
            sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "Plugin reloaded with " + errorCollector.getErrorsCount() + " error(s).");
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "Please check the console.");
            }
        }
    }

    @Name("errors")
    @Description("Displays the last load errors on the console.")
    @Permission(Permissions.COMMAND_PREFIX + "errors")
    @DisplayPriority(3)
    public void errors(CommandSender sender) {
        ErrorCollector errorCollector = ChestCommands.getLastLoadErrors();

        if (errorCollector.hasErrors()) {
            errorCollector.logToConsole();
            sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "Last time the plugin loaded, "
                    + errorCollector.getErrorsCount() + " error(s) were found.");
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "Errors were printed on the console.");
            }
        } else {
            sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.GREEN + "Last plugin load was successful, no errors logged.");
        }
    }

    @Name("list")
    @Description("Lists the loaded menus.")
    @Permission(Permissions.COMMAND_PREFIX + "list")
    @DisplayPriority(2)
    public void list(CommandSender sender) {
        sender.sendMessage(ChestCommands.CHAT_PREFIX + "Loaded menus:");
        for (String file : menuManager.getMenuFileNames()) {
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + file);
        }
    }

    @Name("open")
    @Description("Opens a menu for a player.")
    @Permission(Permissions.COMMAND_PREFIX + "open")
    @MinArgs(1)
    @UsageArgs("<menu> [player]")
    @DisplayPriority(1)
    public void open(CommandSender sender, String[] args) throws CommandException {
        Player target;

        if (sender instanceof Player) {
            if (args.length > 1) {
                CommandValidate.check(sender.hasPermission(Permissions.COMMAND_PREFIX + "open.others"),
                        "You don't have the permission to open a menu for other players.");
                target = Bukkit.getPlayerExact(args[1]);
            } else {
                target = (Player) sender;
            }
        } else {
            CommandValidate.minLength(args, 2, "You must specify a player from the console.");
            target = Bukkit.getPlayerExact(args[1]);
        }

        CommandValidate.notNull(target, "That player is not online.");

        String menuName = Utils.addYamlExtension(args[0]);
        InternalMenu menu = menuManager.getMenuByFileName(menuName);
        CommandValidate.notNull(menu, "The menu \"" + menuName + "\" was not found.");

        if (!sender.hasPermission(menu.getOpenPermission())) {
            menu.sendNoOpenPermissionMessage(sender);
            return;
        }

        if (sender.getName().equalsIgnoreCase(target.getName())) {
            sender.sendMessage(ChatColor.GREEN + "Opening the menu " + menuName + ".");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Opening the menu " + menuName + " to " + target.getName() + ".");
        }

        menu.open(target);
    }

    /*@Name("edit")
    @Description("Create/edit a custom gui.")
    @Permission(Permissions.COMMAND_PREFIX + "edit")
    @DisplayPriority(100)
    public void edit(CommandSender sender, String[] args) {
        ChestCommands.closeAllMenus();

        if(!(sender instanceof Player))
        {
            sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "This command cannot be run from console.");
            return;
        }

        if(args.length == 0)
        {
            sender.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "You must specify a menu name.");
            return;
        }

        Player player = (Player) sender;

        if(player.getGameMode() != GameMode.CREATIVE)
        {
            player.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "You must be in creative mode to edit the menu.");
            return;
        }

        String menuName = Utils.addYamlExtension(args[0]);
        InternalMenu menu = menuManager.getMenuByFileName(menuName);
        if(menu == null)
        {
            player.sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "The menu \"" + menuName + "\" was not found.");
            return;
        }
        MenuCreatorListener.getInstance().openGuiEditor((Player) sender, menu);
    }*/

}
