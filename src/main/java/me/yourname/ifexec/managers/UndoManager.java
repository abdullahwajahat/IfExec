package me.yourname.ifexec.managers;

import me.yourname.ifexec.IfExec;
import me.yourname.ifexec.models.Trigger;

import java.util.LinkedList;
import java.util.Queue;

public class UndoManager {
    private final IfExec plugin;
    private final Queue<Trigger> history = new LinkedList<>();

    public UndoManager(IfExec plugin) {
        this.plugin = plugin;
    }

    public void push(Trigger trigger) {
        int limit = plugin.getConfig().getInt("undo_limit", 2);
        if (history.size() >= limit) history.poll();
        history.offer(trigger);
    }

    public Trigger pop() {
        return history.poll();
    }

    public boolean isEmpty() {
        return history.isEmpty();
    }
}
