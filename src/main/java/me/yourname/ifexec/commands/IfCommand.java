package me.yourname.ifexec.commands;

import me.yourname.ifexec.IfExec;
import me.yourname.ifexec.models.Trigger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class IfCommand implements CommandExecutor, TabCompleter {
    private final IfExec plugin;

    public IfCommand(IfExec plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /if <subcommand>");
            return true;
        }

        if (args[0].equalsIgnoreCase("undo")) {
            if (plugin.getUndoManager().isEmpty()) {
                sender.sendMessage(plugin.getMessageUtil().format("undo_empty"));
                return true;
            }
            Trigger restored = plugin.getUndoManager().pop();
            plugin.getTriggerManager().addTrigger(restored);
            sender.sendMessage(plugin.getMessageUtil().format(
                    plugin.getConfig().getString("undo_success").replace("{name}", restored.getName())));
            return true;
        }

        sender.sendMessage("§cUnknown subcommand!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("undo");
        }
        return completions;
    }
}
