package com.ifexec.manager;

import com.ifexec.IfExec;
import com.ifexec.model.Trigger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class UndoManager {
    private final IfExec plugin;
    private final TriggerManager triggerManager;

    // per-player stacks
    private final Map<UUID, Deque<Trigger>> perPlayerStacks = new HashMap<>();
    // global stack (for console or fallback)
    private final Deque<Trigger> globalStack = new ArrayDeque<>();

    private final Map<UUID, Long> lastPushTime = new HashMap<>();
    private long lastGlobalPushTime = 0L;

    public UndoManager(IfExec plugin, TriggerManager triggerManager) {
        this.plugin = plugin;
        this.triggerManager = triggerManager;
    }

    // push for a player (playerId may be null => push to global)
    public void push(UUID playerId, Trigger trigger) {
        int limit = plugin.getConfigManager().getConfig().getInt("undo_limit", 2);
        long now = System.currentTimeMillis();
        if (playerId == null) {
            globalStack.addLast(trigger.cloneTrigger());
            while (globalStack.size() > limit) globalStack.removeFirst();
            lastGlobalPushTime = now;
        } else {
            Deque<Trigger> stack = perPlayerStacks.computeIfAbsent(playerId, k -> new ArrayDeque<>());
            stack.addLast(trigger.cloneTrigger());
            while (stack.size() > limit) stack.removeFirst();
            lastPushTime.put(playerId, now);
        }
    }

    // global push convenience
    public void push(Trigger trigger) { push(null, trigger); }

    private boolean isExpired(long pushedAt) {
        int timeout = plugin.getConfigManager().getConfig().getInt("undo_timeout", 30);
        return (System.currentTimeMillis() - pushedAt) / 1000L > timeout;
    }

    public boolean hasUndo(CommandSender sender) {
        if (sender instanceof Player p) {
            UUID id = p.getUniqueId();
            Deque<Trigger> s = perPlayerStacks.get(id);
            if (s != null && !s.isEmpty()) {
                Long pushed = lastPushTime.getOrDefault(id, 0L);
                return !isExpired(pushed);
            }
        }
        if (!globalStack.isEmpty()) {
            return !isExpired(lastGlobalPushTime);
        }
        return false;
    }

    public Optional<Trigger> pop(CommandSender sender) {
        if (sender instanceof Player p) {
            UUID id = p.getUniqueId();
            Deque<Trigger> stack = perPlayerStacks.get(id);
            if (stack != null && !stack.isEmpty()) {
                long pushed = lastPushTime.getOrDefault(id, 0L);
                if (isExpired(pushed)) {
                    stack.clear();
                    return Optional.empty();
                }
                return Optional.of(stack.removeLast());
            }
        }
        if (!globalStack.isEmpty()) {
            if (isExpired(lastGlobalPushTime)) {
                globalStack.clear();
                return Optional.empty();
            }
            return Optional.of(globalStack.removeLast());
        }
        return Optional.empty();
    }
}
