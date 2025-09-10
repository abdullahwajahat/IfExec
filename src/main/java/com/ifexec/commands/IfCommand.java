package com.ifexec.commands;

import com.ifexec.IfExec;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.manager.UndoManager;
import com.ifexec.model.Trigger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class IfCommand implements CommandExecutor {
    private final IfExec plugin;
    private final TriggerManager triggerManager;
    private final Messages messages;
    private final UndoManager undoManager;

    public IfCommand(IfExec plugin) {
        this.plugin = plugin;
        this.triggerManager = plugin.getTriggerManager();
        this.messages = plugin.getMessages();
        this.undoManager = plugin.getUndoManager();
    }

    private void msg(CommandSender sender, String key, Map<String,String> ph) {
        String text = messages.get(key);
        for (Map.Entry<String,String> e : ph.entrySet())
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        sender.sendMessage(messages.get("plugin_prefix") + " " + text);
    }

    private void msg(CommandSender sender, String key) {
        sender.sendMessage(messages.get("plugin_prefix") + " " + messages.get(key));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            msg(sender, "usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help" -> msg(sender, "help");
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getMessages().reload();
                msg(sender, "trigger_edited");
            }
            case "listall" -> triggerManager.listAll(sender);
            case "list" -> {
                if (args.length > 1) triggerManager.listOne(sender, args[1]);
                else triggerManager.listAll(sender);
            }
            case "remove" -> triggerManager.handleRemove(sender, args);
            case "enable" -> triggerManager.handleEnableDisable(sender, args, true);
            case "disable" -> triggerManager.handleEnableDisable(sender, args, false);
            case "edit" -> triggerManager.handleEdit(sender, args);
            case "undo" -> undoManager.handleUndo(sender);
            case "on", "isin" -> triggerManager.handleCreate(sender, args);
            default -> msg(sender, "unknown_subcommand");
        }
        return true;
    }
}
