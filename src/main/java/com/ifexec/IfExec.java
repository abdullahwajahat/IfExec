package com.ifexec;
// Save bundled defaults if missing
saveDefaultConfig();
saveResource("messages.yml", false);


try {
// Managers (order matters)
this.configManager = new ConfigManager(this);
this.messages = new Messages(this);
this.triggerManager = new TriggerManager(this);
this.undoManager = new UndoManager(this);


// Register command and tab completer (safe-check)
if (getCommand("if") != null) {
IfCommand cmd = new IfCommand(this);
getCommand("if").setExecutor(cmd);
getCommand("if").setTabCompleter(new IfTabCompleter(this));
} else {
getLogger().severe("Command 'if' not found in plugin.yml — disabling plugin.");
getServer().getPluginManager().disablePlugin(this);
return;
}


// Listener
getServer().getPluginManager().registerEvents(new TriggerListener(this), this);


getLogger().info("IfExec enabled");
} catch (Exception ex) {
getLogger().severe("Failed to enable IfExec: " + ex.getMessage());
ex.printStackTrace();
getServer().getPluginManager().disablePlugin(this);
}
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
