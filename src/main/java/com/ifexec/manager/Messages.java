package com.ifexec.manager;

import com.ifexec.IfExec;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Messages {
    private final IfExec plugin;
    private File file;
    private YamlConfiguration config;

    public Messages(IfExec plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        return config.getString(key, "&cMissing message: " + key);
    }
}
