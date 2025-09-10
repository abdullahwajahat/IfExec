package com.ifexec.manager;

import com.ifexec.IfExec;
import com.ifexec.model.Trigger;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class UndoManager {
    private final IfExec plugin;
    private final TriggerManager triggerManager;
    private final Deque<Trigger> stack = new ArrayDeque<>();

    public UndoManager(IfExec plugin, TriggerManager triggerManager) {
        this.plugin = plugin;
        this.triggerManager = triggerManager;
    }

    public void push(Trigger t) {
        if (t == null) return;
        int limit = plugin.getConfigManager().getConfig().getInt("undo_limit", 2);
        stack.push(t.cloneTrigger());
        while (stack.size() > limit) stack.removeLast();
        scheduleExpiry();
    }

    private void scheduleExpiry() {
        int timeout = plugin.getConfigManager().getConfig().getInt("undo_timeout", 30);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stack.isEmpty()) stack.removeLast();
            }
        }.runTaskLater(plugin, timeout * 20L);
    }

    public Optional<Trigger> pop() {
        if (stack.isEmpty()) return Optional.empty();
        return Optional.of(stack.pop());
    }

    public boolean hasUndo() { return !stack.isEmpty(); }
}
