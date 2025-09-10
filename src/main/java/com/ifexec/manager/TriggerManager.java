package com.ifexec.manager;

import com.ifexec.IfExec;
import com.ifexec.model.Trigger;
import org.bukkit.command.CommandSender;

import java.util.*;

public class TriggerManager {
    private final IfExec plugin;
    private final Map<String,Trigger> triggers = new HashMap<>();

    public TriggerManager(IfExec plugin) { this.plugin = plugin; }

    public void add(Trigger t) { triggers.put(t.getName(), t); }
    public Collection<Trigger> getAll() { return triggers.values(); }
    public Optional<Trigger> get(String n) { return Optional.ofNullable(triggers.get(n)); }

    // TODO implement handleCreate, handleRemove, handleEdit etc
    public void handleCreate(CommandSender sender, String[] args) {}
    public void handleRemove(CommandSender sender, String[] args) {}
    public void handleEnableDisable(CommandSender sender, String[] args, boolean enable) {}
    public void handleEdit(CommandSender sender, String[] args) {}
    public void listAll(CommandSender sender) {}
    public void listOne(CommandSender sender, String name) {}

    public void saveAll() {}
}
