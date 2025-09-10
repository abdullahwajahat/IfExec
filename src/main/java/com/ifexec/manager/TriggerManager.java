package com.ifexec.manager;


import com.ifexec.IfExec;
import com.ifexec.model.Trigger;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;


import java.util.*;


public class TriggerManager {
private final IfExec plugin;
private final LinkedHashMap<String, Trigger> triggers = new LinkedHashMap<>();


public TriggerManager(IfExec plugin) {
this.plugin = plugin;
loadAll();
}


public void loadAll() {
triggers.clear();
YamlConfiguration cfg = plugin.getConfigManager().getConfig();
ConfigurationSection root = cfg.getConfigurationSection("triggers");
if (root == null) return;
for (String key : root.getKeys(false)) {
ConfigurationSection s =
