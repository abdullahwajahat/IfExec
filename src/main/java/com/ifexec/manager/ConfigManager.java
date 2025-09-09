package com.ifexec.manager;

import com.ifexec.IfExec;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final IfExec plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    public ConfigManager(IfExec plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "config.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public YamlConfiguration getConfig() { return dataConfig; }

    public void saveConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
        }
    }
}
