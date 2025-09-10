package com.ifexec.manager;

import com.ifexec.IfExec;
import com.ifexec.model.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.*;
import java.util.stream.Collectors;

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

    public boolean removeTrigger(String name) {
        if (triggers.containsKey(name)) {
            triggers.remove(name);
            return true;
        }
        return false;
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

    // ---- UI / command methods used from IfCommand ----

    public void listAll(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().getWithPrefix("list_header"));
        if (triggers.isEmpty()) { sender.sendMessage(plugin.getMessages().get("list_empty")); return; }
        for (Trigger t : triggers.values()) {
            TextComponent comp = new TextComponent("§7- §a[" + t.getName() + "]");
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if list " + t.getName()));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to view " + t.getName()).create()));
            if (sender instanceof org.bukkit.command.CommandSender cs) cs.spigot().sendMessage(comp);
        }
    }

    public void listOne(CommandSender sender, String name) {
        Optional<Trigger> opt = get(name);
        if (opt.isEmpty()) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_trigger").replace("{name}", name)); return; }
        Trigger t = opt.get();
        sender.sendMessage("§eName: §f" + t.getName());
        sender.sendMessage("§eWorld: §f" + t.getWorld());
        sender.sendMessage("§eType: §f" + (t.getType() == Trigger.Type.BLOCK ? "block" : "region"));
        if (t.getType() == Trigger.Type.BLOCK) sender.sendMessage("§eCoords: §f" + t.getX() + " " + t.getY() + " " + t.getZ());
        else sender.sendMessage("§eCoords: §f" + t.getX1() + " " + t.getY1() + " " + t.getZ1() + " - " + t.getX2() + " " + t.getY2() + " " + t.getZ2());
        sender.sendMessage("§eRole: §f" + t.getRole());
        int cd = (t.getCooldown() >= 0) ? t.getCooldown() : plugin.getConfigManager().getConfig().getInt("default_cooldown", 3);
        sender.sendMessage("§eCooldown: §f" + cd + "s");
        sender.sendMessage("§eSilent: §f" + t.isSilent());
        sender.sendMessage("§eCommands:");
        List<String> cmds = t.getCommands();
        for (int i=0;i<cmds.size();i++) sender.sendMessage("  " + (i+1) + ". " + cmds.get(i));
        sender.sendMessage("§eMessages:");
        Map<String,String> ms = t.getMessages();
        sender.sendMessage("  all: \"" + ms.getOrDefault("all","") + "\"");
        sender.sendMessage("  staff: \"" + ms.getOrDefault("staff","") + "\"");
    }

    // handle create: matches the IfCommand create signature
    public void handleCreate(CommandSender sender, String[] args) {
        // parse and create trigger (mirrors IfCommand.create logic)
        // to keep single-responsibility we delegate parsing to IfCommand; TriggerManager provides storage + helpers
        // but for convenience we include a tiny helper: IfCommand will call IfCommand.handleCreate which uses triggerManager.add()
        // In this implementation the heavy parsing is in IfCommand (provided later)
    }

    public void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ifexec.admin")) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_permission")); return; }
        if (args.length == 1) {
            sender.sendMessage(plugin.getMessages().getWithPrefix("remove_prompt"));
            for (Trigger t : triggers.values()) {
                TextComponent comp = new TextComponent("§7- §c[" + t.getName() + "]");
                comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if remove confirm " + t.getName()));
                comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to remove " + t.getName()).create()));
                if (sender instanceof org.bukkit.command.CommandSender cs) cs.spigot().sendMessage(comp);
            }
            return;
        }

        String arg1 = args[1];
        if (arg1.equalsIgnoreCase("confirm") && args.length >= 3) {
            String name = args[2];
            doRemoveConfirmed(sender, name);
            return;
        }

        String name = arg1;
        Optional<Trigger> opt = get(name);
        if (opt.isEmpty()) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_trigger").replace("{name}", name)); return; }

        sender.sendMessage(plugin.getMessages().getWithPrefix("") + "§cAre you sure you want to remove trigger §f" + name + "§c?");
        TextComponent confirm = new TextComponent("§7[Confirm]");
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if remove confirm " + name));
        TextComponent cancel = new TextComponent("§7[Cancel]");
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/if remove " + name));
        if (sender instanceof org.bukkit.command.CommandSender cs) {
            cs.spigot().sendMessage(confirm);
            cs.spigot().sendMessage(cancel);
        }
    }

    private void doRemoveConfirmed(CommandSender sender, String name) {
        Optional<Trigger> opt = get(name);
        if (opt.isEmpty()) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_trigger").replace("{name}", name)); return; }
        Trigger t = opt.get();
        plugin.getUndoManager().push(t);
        remove(name);
        sender.sendMessage(plugin.getMessages().getWithPrefix("trigger_removed").replace("{name}", name));
        sender.sendMessage(plugin.getMessages().getWithPrefix("") + "§7Type /if undo to restore. (expires in " + plugin.getConfigManager().getConfig().getInt("undo_timeout", 30) + "s)");
    }

    public void handleEnableDisable(CommandSender sender, String[] args, boolean enable) {
        if (!sender.hasPermission("ifexec.admin")) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_permission")); return; }
        if (args.length < 2) { sender.sendMessage(plugin.getMessages().getWithPrefix("") + "§eUsage: /if " + (enable ? "enable <name>" : "disable <name>")); return; }
        String name = args[1];
        Optional<Trigger> opt = get(name);
        if (opt.isEmpty()) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_trigger").replace("{name}", name)); return; }
        Trigger t = opt.get();
        t.setEnabled(enable);
        add(t);
        sender.sendMessage(plugin.getMessages().getWithPrefix("trigger_edited").replace("{name}", name));
    }

    public void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ifexec.admin")) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_permission")); return; }
        if (args.length < 3) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_usage")); return; }
        String name = args[1];
        String field = args[2].toLowerCase();
        Optional<Trigger> opt = get(name);
        if (opt.isEmpty()) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_trigger").replace("{name}", name)); return; }
        Trigger t = opt.get();

        try {
            switch (field) {
                case "coords":
                    if (t.getType() == Trigger.Type.BLOCK) {
                        if (args.length < 6) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_coords_block")); return; }
                        t.setX(Integer.parseInt(args[3])); t.setY(Integer.parseInt(args[4])); t.setZ(Integer.parseInt(args[5]));
                        if (args.length >= 7) t.setWorld(args[6]);
                    } else {
                        if (args.length < 9) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_coords_region")); return; }
                        t.setX1(Integer.parseInt(args[3])); t.setY1(Integer.parseInt(args[4])); t.setZ1(Integer.parseInt(args[5]));
                        t.setX2(Integer.parseInt(args[6])); t.setY2(Integer.parseInt(args[7])); t.setZ2(Integer.parseInt(args[8]));
                        if (args.length >= 10) t.setWorld(args[9]);
                    }
                    break;
                case "role":
                    if (args.length < 4) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_role_usage")); return; }
                    String r = args[3].toLowerCase(); if (!r.equals("staff") && !r.equals("all")) r = "all"; t.setRole(r);
                    break;
                case "command":
                case "commands":
                    if (args.length < 4) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_commands_usage")); return; }
                    String joined = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    List<String> cmds = Arrays.stream(joined.split(";")).map(s -> {
                        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length()-1);
                        return s;
                    }).filter(s->!s.isBlank()).collect(Collectors.toList());
                    if (cmds.isEmpty()) { sender.sendMessage(plugin.getMessages().getWithPrefix("no_commands")); return; }
                    t.setCommands(cmds);
                    break;
                case "cooldown":
                    if (args.length < 4) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_cd_usage")); return; }
                    t.setCooldown(Integer.parseInt(args[3])); break;
                case "silent":
                    if (args.length < 4) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_silent_usage")); return; }
                    t.setSilent(Boolean.parseBoolean(args[3])); break;
                case "message":
                    if (args.length < 5) { sender.sendMessage(plugin.getMessages().getWithPrefix("edit_message_usage")); return; }
                    String target = args[3].toLowerCase();
                    String msg = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    if (msg.startsWith("\"") && msg.endsWith("\"") && msg.length() >= 2) msg = msg.substring(1, msg.length()-1);
                    Map<String,String> mm = t.getMessages(); mm.put(target, msg); t.setMessages(mm);
                    break;
                default:
                    sender.sendMessage(plugin.getMessages().getWithPrefix("") + "§cUnknown field: " + field);
                    return;
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.getMessages().getWithPrefix("") + "§cInvalid number.");
            return;
        }

        add(t);
        sender.sendMessage(plugin.getMessages().getWithPrefix("trigger_edited").replace("{name}", name));
    }
}
