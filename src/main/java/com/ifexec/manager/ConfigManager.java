package com.ifexec.manager;

import com.ifexec.IfExec;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final IfExec plugin;

    public ConfigManager(IfExec plugin) { this.plugin = plugin; }

    public FileConfiguration getConfig() { return plugin.getConfig(); }
}
