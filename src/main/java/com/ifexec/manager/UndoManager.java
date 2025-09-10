package com.ifexec.manager;

import com.ifexec.model.Trigger;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.Stack;

public class UndoManager {
    private final Stack<Trigger> stack = new Stack<>();

    public UndoManager(com.ifexec.IfExec plugin, TriggerManager triggerManager) {}

    public void push(Trigger t) { stack.push(t); }

    public void handleUndo(CommandSender sender) {
        if (stack.isEmpty()) {
            sender.sendMessage("§cNothing to undo.");
            return;
        }
        Trigger t = stack.pop();
        sender.sendMessage("§aRestored trigger: " + t.getName());
    }
}
