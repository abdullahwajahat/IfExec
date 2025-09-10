package com.ifexec.manager;

import com.ifexec.IfExec;
import org.bukkit.command.CommandSender;

import java.util.*;

public class UndoManager {

    private final IfExec plugin;
    private final TriggerManager triggerManager;
    private final Map<UUID, Deque<String>> undoHistory; // Player UUID -> stack of trigger names
    private final int undoLimit;
    private final int undoTimeout;

    private final Map<UUID, Long> lastUndoTime;

    public UndoManager(IfExec plugin, TriggerManager triggerManager) {
        this.plugin = plugin;
        this.triggerManager = triggerManager;
        this.undoHistory = new HashMap<>();
        this.lastUndoTime = new HashMap<>();
        this.undoLimit = plugin.getConfig().getInt("undo_limit", 2);
        this.undoTimeout = plugin.getConfig().getInt("undo_timeout", 30);
    }

    public void addAction(UUID playerId, String triggerName) {
        undoHistory.putIfAbsent(playerId, new ArrayDeque<>());
        Deque<String> stack = undoHistory.get(playerId);

        if (stack.size() >= undoLimit) {
            stack.removeFirst(); // remove oldest
        }
        stack.addLast(triggerName);
        lastUndoTime.put(playerId, System.currentTimeMillis());
    }

    public void handleUndo(CommandSender sender) {
        UUID playerId = sender instanceof org.bukkit.entity.Player
                ? ((org.bukkit.entity.Player) sender).getUniqueId()
                : null;

        if (playerId == null) {
            sender.sendMessage(plugin.getMessages().get("undo-player-only"));
            return;
        }

        if (!undoHistory.containsKey(playerId) || undoHistory.get(playerId).isEmpty()) {
            sender.sendMessage(plugin.getMessages().get("undo-none"));
            return;
        }

        // timeout check
        long lastTime = lastUndoTime.getOrDefault(playerId, 0L);
        if ((System.currentTimeMillis() - lastTime) / 1000 > undoTimeout) {
            sender.sendMessage(plugin.getMessages().get("undo-timeout")
                    .replace("%timeout%", String.valueOf(undoTimeout)));
            return;
        }

        String triggerName = undoHistory.get(playerId).removeLast();
        if (triggerManager.removeTrigger(triggerName)) {
            sender.sendMessage(plugin.getMessages().get("undo-success")
                    .replace("%trigger%", triggerName));
        } else {
            sender.sendMessage(plugin.getMessages().get("undo-fail")
                    .replace("%trigger%", triggerName));
        }
    }
}
