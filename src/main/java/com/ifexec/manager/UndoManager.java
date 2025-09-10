package com.ifexec.manager;

import com.ifexec.model.Trigger;
import org.bukkit.command.CommandSender;

import java.util.*;

public class UndoManager {
    private final TriggerManager triggerManager;
    private final Map<UUID, Deque<Trigger>> undoStacks = new HashMap<>();
    private final int undoLimit;
    private final int undoTimeout; // seconds
    private final Map<UUID, Long> lastUndoTime = new HashMap<>();

    public UndoManager(TriggerManager triggerManager, int undoLimit, int undoTimeout) {
        this.triggerManager = triggerManager;
        this.undoLimit = undoLimit;
        this.undoTimeout = undoTimeout;
    }

    /** Push a trigger to player's undo stack */
    public void push(UUID playerId, Trigger trigger) {
        Deque<Trigger> stack = undoStacks.computeIfAbsent(playerId, k -> new ArrayDeque<>());

        if (stack.size() >= undoLimit) {
            stack.removeFirst(); // keep size within limit
        }
        stack.addLast(trigger);
        lastUndoTime.put(playerId, System.currentTimeMillis());
    }

    /** Undo last trigger for a player */
    public boolean handleUndo(CommandSender sender, UUID playerId) {
        Deque<Trigger> stack = undoStacks.get(playerId);
        if (stack == null || stack.isEmpty()) {
            sender.sendMessage("§cNothing to undo!");
            return false;
        }

        long lastTime = lastUndoTime.getOrDefault(playerId, 0L);
        if ((System.currentTimeMillis() - lastTime) / 1000 < undoTimeout) {
            sender.sendMessage("§cYou must wait before undoing again!");
            return false;
        }

        Trigger last = stack.removeLast();
        triggerManager.removeTrigger(last.getName());
        sender.sendMessage("§aUndid trigger: §e" + last.getName());
        lastUndoTime.put(playerId, System.currentTimeMillis());
        return true;
    }
}
