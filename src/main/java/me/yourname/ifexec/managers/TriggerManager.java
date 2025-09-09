package me.yourname.ifexec.managers;

import me.yourname.ifexec.IfExec;
import me.yourname.ifexec.models.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;

public class TriggerManager {
    private final IfExec plugin;
    private final Map<String, Trigger> triggers = new HashMap<>();

    public TriggerManager(IfExec plugin) {
        this.plugin = plugin;
        loadTriggers();
    }

    public void loadTriggers() {
        // TODO: Load from config.yml
    }

    public void saveTriggers() {
        // TODO: Save triggers to config.yml
    }

    public Trigger getTrigger(String name) {
        return triggers.get(name.toLowerCase());
    }

    public void addTrigger(Trigger trigger) {
        triggers.put(trigger.getName().toLowerCase(), trigger);
        saveTriggers();
    }

    public void removeTrigger(String name) {
        triggers.remove(name.toLowerCase());
        saveTriggers();
    }

    public Collection<Trigger> getAllTriggers() {
        return triggers.values();
    }
}
