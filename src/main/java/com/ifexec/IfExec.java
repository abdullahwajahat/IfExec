package com.ifexec;

import com.ifexec.commands.IfCommand;
import com.ifexec.commands.IfTabCompleter;
import com.ifexec.listeners.TriggerListener;
import com.ifexec.manager.ConfigManager;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.manager.UndoManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Filter;
import java.util.logging.Logger;

public final class IfExec extends JavaPlugin {

    private static IfExec instance;
    public static volatile boolean isDispatching = false;

    private ConfigManager configManager;
    private Messages messages;
    private TriggerManager triggerManager;
    private UndoManager undoManager;

    @Override
    public void onEnable() {
        instance = this;

        // Aggressively filter out ALL console logs while trigger commands are being dispatched
        try {
            Logger rootLogger = Logger.getLogger("");
            Filter previousFilter = rootLogger.getFilter();
            rootLogger.setFilter(record -> {
                // If the plugin is currently running a command, block the log from printing
                if (isDispatching) {
                    return false;
                }
                return previousFilter == null || previousFilter.isLoggable(record);
            });
        } catch (Exception ignored) {}

        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.triggerManager = new TriggerManager(this);
        this.undoManager = new UndoManager(this, triggerManager);

        IfCommand cmd = new IfCommand(this);
        if (getCommand("if") != null) {
            getCommand("if").setExecutor(cmd);
            getCommand("if").setTabCompleter(new IfTabCompleter(this));
        } else {
            getLogger().severe("Command 'if' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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
