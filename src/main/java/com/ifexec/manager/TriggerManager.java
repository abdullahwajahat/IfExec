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
            ConfigurationSection s = root.getConfigurationSection(key);
            Trigger t = new Trigger();
            t.setName(key);
            String type = s.getString("type", "block");
            t.setType(type.equalsIgnoreCase("region") ? Trigger.Type.REGION : Trigger.Type.BLOCK);
            t.setWorld(s.getString("world", "world"));
            if (t.getType() == Trigger.Type.BLOCK) {
                t.setX(s.getInt("x")); t.setY(s.getInt("y")); t.setZ(s.getInt("z"));
            } else {
                t.setX1(s.getInt("x1")); t.setY1(s.getInt("y1")); t.setZ1(s.getInt("z1"));
                t.setX2(s.getInt("x2")); t.setY2(s.getInt("y2")); t.setZ2(s.getInt("z2"));
            }
            t.setCommands(s.getStringList("commands"));
            t.setEnabled(s.getBoolean("enabled", true));
            t.setCooldown(s.getInt("cooldown", -1));
            t.setSilent(s.getBoolean("silent", false));
            t.setRole(s.getString("role", "all"));
            if (s.isConfigurationSection("messages")) {
                ConfigurationSection ms = s.getConfigurationSection("messages");
                Map<String,String> map = new HashMap<>();
                for (String mk : ms.getKeys(false)) map.put(mk, ms.getString(mk));
                t.setMessages(map);
            }
            triggers.put(key, t);
        }
    }

    public void saveAll() {
        YamlConfiguration cfg = plugin.getConfigManager().getConfig();
        cfg.set("triggers", null); // clear
        for (Map.Entry<String, Trigger> e : triggers.entrySet()) {
            String base = "triggers." + e.getKey() + ".";
            Trigger t = e.getValue();
            cfg.set(base + "type", t.getType() == Trigger.Type.REGION ? "region" : "block");
            cfg.set(base + "world", t.getWorld());
            if (t.getType() == Trigger.Type.BLOCK) {
                cfg.set(base + "x", t.getX()); cfg.set(base + "y", t.getY()); cfg.set(base + "z", t.getZ());
            } else {
                cfg.set(base + "x1", t.getX1()); cfg.set(base + "y1", t.getY1()); cfg.set(base + "z1", t.getZ1());
                cfg.set(base + "x2", t.getX2()); cfg.set(base + "y2", t.getY2()); cfg.set(base + "z2", t.getZ2());
            }
            cfg.set(base + "commands", t.getCommands());
            cfg.set(base + "enabled", t.isEnabled());
            cfg.set(base + "cooldown", t.getCooldown());
            cfg.set(base + "silent", t.isSilent());
            cfg.set(base + "role", t.getRole());
            if (t.getMessages() != null && !t.getMessages().isEmpty()) {
                for (Map.Entry<String,String> me : t.getMessages().entrySet()) {
                    cfg.set(base + "messages." + me.getKey(), me.getValue());
                }
            }
        }
        plugin.getConfigManager().saveConfig();
    }

    public Collection<Trigger> getAll() { return triggers.values(); }
    public Optional<Trigger> get(String name) { return Optional.ofNullable(triggers.get(name)); }
    public boolean exists(String name) { return triggers.containsKey(name); }

    public void add(Trigger t) { triggers.put(t.getName(), t); saveAll(); }
    public void remove(String name) { triggers.remove(name); saveAll(); }
    // boolean removeTrigger used by UndoManager
    public boolean removeTrigger(String name) {
        boolean removed = triggers.remove(name) != null;
        if (removed) saveAll();
        return removed;
    }

    public Set<String> names() { return triggers.keySet(); }

    public List<Trigger> findByLocation(Location loc) {
        List<Trigger> list = new ArrayList<>();
        for (Trigger t : triggers.values()) {
            if (!t.isEnabled()) continue;
            if (t.getType() == Trigger.Type.BLOCK && t.isInBlock(loc)) list.add(t);
            if (t.getType() == Trigger.Type.REGION && t.isInRegion(loc)) list.add(t);
        }
        return list;
    }
}
