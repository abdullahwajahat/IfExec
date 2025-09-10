package com.ifexec.manager;


import com.ifexec.IfExec;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


import java.io.File;


public class Messages {
private final IfExec plugin;
private final File messagesFile;
private final FileConfiguration messagesConfig;


public Messages(IfExec plugin) {
this.plugin = plugin;
messagesFile = new File(plugin.getDataFolder(), "messages.yml");
if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
}


// Returns the raw message (no automatic prefix)
public String get(String path) {
String msg = messagesConfig.getString(path, "");
if (msg == null) msg = "";
return ChatColor.translateAlternateColorCodes('&', msg);
}


// Returns message with prefix prepended once
public String getWithPrefix(String path) {
String prefix = messagesConfig.getString("plugin_prefix", "");
String msg = messagesConfig.getString(path, "");
if (msg == null) msg = "";
String combined = (prefix == null ? "" : prefix) + msg;
return ChatColor.translateAlternateColorCodes('&', combined);
}
}
