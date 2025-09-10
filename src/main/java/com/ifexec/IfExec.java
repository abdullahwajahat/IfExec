package com.ifexec;

import com.ifexec.commands.IfCommand;
import com.ifexec.commands.IfTabCompleter;
import com.ifexec.listeners.TriggerListener;
import com.ifexec.manager.ConfigManager;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.manager.UndoManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class IfExec extends JavaPlugin {

    private static IfExec instance;
    private ConfigManager configManager;
    private Messages messages;
    private TriggerManager triggerManager;
    private UndoManager undoManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save defaults
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Managers
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.triggerManager = new TriggerManager(this);
        this.undoManager = new UndoManager(this, triggerManager);

        // Commands & tab completer (safe)
        if (getCommand("if") != null) {
            IfCommand cmd = new IfCommand(this);
            getCommand("if").setExecutor(cmd);
            getCommand("if").setTabCompleter(new IfTabCompleter(this));
        } else {
            getLogger().severe("Command 'if' missing from plugin.yml — plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Listener
        getServer().getPluginManager().registerEvents(new TriggerListener(this), this);

        getLogger().info("IfExec enabled");
    }

    @Override
    public void onDisable() {
        if (triggerManager != null) triggerManager.saveAll();
        instance = null;
    }

    public static IfExec getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public Messages getMessages() { return messages; }
    public TriggerManager getTriggerManager() { return triggerManager; }
    public UndoManager getUndoManager() { return undoManager; }
}
