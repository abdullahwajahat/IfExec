package com.ifexec.commands;

import com.ifexec.IfExec;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.manager.UndoManager;
import com.ifexec.model.Trigger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class IfCommand implements CommandExecutor {

    private final IfExec plugin;
    private final TriggerManager triggerManager;
    private final Messages messages;
    private final UndoManager undoManager;

    public IfCommand(IfExec plugin) {
        this.plugin = plugin;
        this.triggerManager = plugin.getTriggerManager();
        this.messages = plugin.getMessages();
        this.undoManager = plugin.getUndoManager();
    }

    private void sendPref(CommandSender s, String key) {
        s.sendMessage(messages.get(key));
    }

    private void sendPref(CommandSender s, String key, Map<String,String> ph) {
        String out = messages.get(key);
        for (Map.Entry<String,String> e : ph.entrySet()) out = out.replace("{" + e.getKey() + "}", e.getValue());
        s.sendMessage(out);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messages.get("plugin_prefix") + "§eIfExec: use /if help");
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "help":
                    sender.sendMessage(messages.get("plugin_prefix") + "§6IfExec commands: /if <selector> on <coords> then \"cmd\" ... [name <name>] [role staff|all], /if listall, /if list <name>, /if edit <name> ..., /if remove <name>, /if undo");
                    break;
                case "on":
                case "isin":
                    handleCreate(sender, args);
                    break;
                case "list":
                    if (args.length == 1) listAll(sender); else listOne(sender, args[1]);
                    break;
                case "listall":
                    listAll(sender);
                    break;
                case "remove":
                    handleRemove(sender, args);
                    break;
                case "disable":
                    handleEnableDisable(sender, args, false);
                    break;
                case "enable":
                    handleEnableDisable(sender, args, true);
                    break;
                case "reload":
                    plugin.getConfigManager().getConfig().options().copyDefaults(true);
                    plugin.getConfigManager().saveConfig();
                    plugin.getTriggerManager().loadAll();
                    sender.sendMessage(messages.get("plugin_prefix") + messages.get("trigger_edited"));
                    break;
                case "edit":
                    handleEdit(sender, args);
                    break;
                case "undo":
                    handleUndo(sender);
                    break;
                default:
                    // maybe creation with selector omitted: /if @p on ...
                    if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("isin"))) {
                        handleCreate(sender, args);
                    } else {
                        sender.sendMessage(messages.get("plugin_prefix") + "§cUnknown subcommand. Use /if help");
                    }
            }
        } catch (Exception ex) {
            sender.sendMessage(messages.get("plugin_prefix") + "§cError: " + ex.getMessage());
            plugin.getLogger().severe("IfExec command error: " + ex);
        }
        return true;
    }

    // ---------- CREATE ----------
    private void handleCreate(CommandSender sender, String[] args) {
        int idx = 0;
        String selector;
        String mode;
        if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("isin"))) {
            selector = (sender instanceof Player) ? "@s" : "@p";
            mode = args[1].toLowerCase();
            idx = 1;
        } else {
            selector = args[0];
            if (args.length >= 2) {
                mode = args[1].toLowerCase();
                idx = 1;
            } else {
                sender.sendMessage(messages.get("plugin_prefix") + messages.get("created_usage"));
                return;
            }
        }
        if (!mode.equals("on") && !mode.equals("isin")) {
            sender.sendMessage(messages.get("plugin_prefix") + messages.get("created_usage"));
            return;
        }

        int thenIndex = -1;
        for (int i = idx+1; i < args.length; i++) if (args[i].equalsIgnoreCase("then")) { thenIndex = i; break; }
        if (thenIndex == -1) {
            sender.sendMessage(messages.get("plugin_prefix") + "§cMissing 'then' in command.");
            return;
        }

        List<String> coordTokens = new ArrayList<>();
        for (int i = idx+1; i < thenIndex; i++) coordTokens.add(args[i]);

        List<String> tail = new ArrayList<>();
        for (int i = thenIndex+1; i < args.length; i++) tail.add(args[i]);

        String name = null; String role = "all";
        int nameIdx = -1, roleIdx = -1;
        for (int i=0;i<tail.size();i++) {
            if (tail.get(i).equalsIgnoreCase("name")) nameIdx = i;
            if (tail.get(i).equalsIgnoreCase("role")) roleIdx = i;
        }
        int optionStart = tail.size();
        if (nameIdx != -1) optionStart = Math.min(optionStart, nameIdx);
        if (roleIdx != -1) optionStart = Math.min(optionStart, roleIdx);

        List<String> commandTokens = new ArrayList<>();
        for (int i=0;i<optionStart;i++) commandTokens.add(tail.get(i));

        if (nameIdx != -1 && nameIdx +1 < tail.size()) name = tail.get(nameIdx +1);
        if (roleIdx != -1 && roleIdx +1 < tail.size()) role = tail.get(roleIdx +1).toLowerCase();
        if (!role.equals("staff") && !role.equals("all")) role = "all";

        List<String> commands = commandTokens.stream().map(this::stripQuotes).filter(s -> !s.isBlank()).collect(Collectors.toList());
        if (commands.isEmpty()) { sender.sendMessage(messages.get("plugin_prefix") + "§cNo commands provided."); return; }

        Trigger t = new Trigger();
        if (name == null || name.isBlank()) name = generateName();
        t.setName(name);
        t.setRole(role);
        t.setCommands(commands);

        if (mode.equals("on")) {
            if (coordTokens.size() < 3) { sender.sendMessage(messages.get("plugin_prefix") + "§cNot enough coordinates."); return; }
            try {
                int x = Integer.parseInt(coordTokens.get(0));
                int y = Integer.parseInt(coordTokens.get(1));
                int z = Integer.parseInt(coordTokens.get(2));
                t.setType(Trigger.Type.BLOCK);
                t.setX(x); t.setY(y); t.setZ(z);
                if (coordTokens.size() >= 4) t.setWorld(coordTokens.get(3));
                else if (sender instanceof Player) t.setWorld(((Player)sender).getWorld().getName());
                else { sender.sendMessage(messages.get("plugin_prefix") + "§cWorld required when run from console."); return; }
            } catch (NumberFormatException ex) { sender.sendMessage(messages.get("plugin_prefix") + "§cInvalid coordinate."); return; }
        } else {
            if (coordTokens.size() < 6) { sender.sendMessage(messages.get("plugin_prefix") + "§cNot enough region coordinates."); return; }
            try {
                int x1 = Integer.parseInt(coordTokens.get(0));
                int y1 = Integer.parseInt(coordTokens.get(1));
                int z1 = Integer.parseInt(coordTokens.get(2));
                int x2 = Integer.parseInt(coordTokens.get(3));
                int y2 = Integer.parseInt(coordTokens.get(4));
                int z2 = Integer.parseInt(coordTokens.get(5));
                t.setType(Trigger.Type.REGION);
                t.setX1(x1); t.setY1(y1); t.setZ1(z1);
                t.setX2(x2); t.setY2(y2); t.setZ2(z2);
                if (coordTokens.size() >= 7) t.setWorld(coordTokens.get(6));
                else if (sender instanceof Player) t.setWorld(((Player)sender).getWorld().getName());
                else { sender.sendMessage(messages.get("plugin_prefix") + "§cWorld required when run from console."); return; }
            } catch (NumberFormatException ex) { sender.sendMessage(messages.get("plugin_prefix") + "§cInvalid coordinate."); return; }
        }

        t.setEnabled(true);
        t.setCooldown(-1);
        t.setSilent(false);
        t.setMessages(new HashMap<>());

        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>(); ph.put("name", t.getName());
        sendPref(sender, "trigger_created", ph);
    }

    private String generateName() {
        int i=1;
        while (true) {
            String n = "trigger_" + i;
            if (!triggerManager.exists(n)) return n;
            i++;
        }
    }

    // ---------- LIST ----------
    private void listAll(CommandSender sender) {
        Collection<Trigger> all = triggerManager.getAll();
        sender.sendMessage(messages.get("plugin_prefix") + "§eTriggers: §f(click a name to view details, shift+click to paste)");
        if (all.isEmpty()) { sender.sendMessage("§7- §c(no triggers)"); return; }
        for (Trigger t : all) {
            TextComponent comp = new TextComponent("§7- §a[" + t.getName() + "]");
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if list " + t.getName()));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to run /if list " + t.getName() + "\nShift-Click to paste").create()));
            sender.spigot().sendMessage(comp);
        }
    }

    private void listOne(CommandSender sender, String name) {
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }
        Trigger t = opt.get();
        sender.sendMessage("§eName: §f" + t.getName());
        sender.sendMessage("§eWorld: §f" + t.getWorld());
        sender.sendMessage("§eType: §f" + (t.getType() == Trigger.Type.BLOCK ? "block" : "region"));
        if (t.getType() == Trigger.Type.BLOCK) sender.sendMessage("§eCoords: §f" + t.getX() + " " + t.getY() + " " + t.getZ());
        else sender.sendMessage("§eCoords: §f" + t.getX1() + " " + t.getY1() + " " + t.getZ1() + "  -  " + t.getX2() + " " + t.getY2() + " " + t.getZ2());
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

    // ---------- REMOVE ----------
    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ifexec.admin")) { sendPref(sender, "no_permission"); return; }
        if (args.length == 1) {
            sender.sendMessage(messages.get("plugin_prefix") + "§eClick a trigger below to remove it (Shift-Click to paste):");
            for (Trigger t : triggerManager.getAll()) {
                TextComponent comp = new TextComponent("§7- §c[" + t.getName() + "]");
                comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if remove " + t.getName()));
                comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to remove " + t.getName() + "\nShift-Click to paste").create()));
                sender.spigot().sendMessage(comp);
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
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }

        sender.sendMessage(messages.get("plugin_prefix") + "§cAre you sure you want to remove trigger §f" + name + "§c?");
        TextComponent confirm = new TextComponent("§7[Confirm]");
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if remove confirm " + name));
        confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to permanently delete " + name).create()));
        TextComponent cancel = new TextComponent("§7[Cancel]");
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/if remove " + name));
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to cancel").create()));
        sender.spigot().sendMessage(confirm);
        sender.spigot().sendMessage(cancel);
    }

    private void doRemoveConfirmed(CommandSender sender, String name) {
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }
        Trigger t = opt.get();
        undoManager.push(t);
        triggerManager.remove(name);
        Map<String,String> ph = new HashMap<>(); ph.put("name", name);
        sendPref(sender, "trigger_removed", ph);
        sender.sendMessage(messages.get("plugin_prefix") + "§7Type /if undo to restore. (expires in " + plugin.getConfigManager().getConfig().getInt("undo_timeout", 30) + "s)");
    }

    // ---------- ENABLE/DISABLE ----------
    private void handleEnableDisable(CommandSender sender, String[] args, boolean enable) {
        if (!sender.hasPermission("ifexec.admin")) { sendPref(sender, "no_permission"); return; }
        if (args.length < 2) { sender.sendMessage(messages.get("plugin_prefix") + "§eUsage: /if " + (enable ? "enable <name>" : "disable <name>")); return; }
        String name = args[1];
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }
        Trigger t = opt.get();
        t.setEnabled(enable);
        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "trigger_edited", ph);
    }

    // ---------- EDIT ----------
    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ifexec.admin")) { sendPref(sender, "no_permission"); return; }
        if (args.length < 3) { sender.sendMessage(messages.get("plugin_prefix") + "§eUsage: /if edit <name> <field> <value>"); return; }
        String name = args[1];
        String field = args[2].toLowerCase();
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }
        Trigger t = opt.get();

        switch (field) {
            case "coords":
                if (t.getType() == Trigger.Type.BLOCK) {
                    if (args.length < 6) { sender.sendMessage(messages.get("plugin_prefix") + "§cUsage: /if edit <name> coords <x> <y> <z> [world]"); return; }
                    try {
                        int x = Integer.parseInt(args[3]);
                        int y = Integer.parseInt(args[4]);
                        int z = Integer.parseInt(args[5]);
                        t.setX(x); t.setY(y); t.setZ(z);
                        if (args.length >= 7) t.setWorld(args[6]);
                    } catch (NumberFormatException ex) { sender.sendMessage(messages.get("plugin_prefix") + "§cInvalid number."); return; }
                } else {
                    if (args.length < 9) { sender.sendMessage(messages.get("plugin_prefix") + "§cUsage: /if edit <name> coords <x1> <y1> <z1> <x2> <y2> <z2> [world]"); return; }
                    try {
                        t.setX1(Integer.parseInt(args[3])); t.setY1(Integer.parseInt(args[4])); t.setZ1(Integer.parseInt(args[5]));
                        t.setX2(Integer.parseInt(args[6])); t.setY2(Integer.parseInt(args[7])); t.setZ2(Integer.parseInt(args[8]));
                        if (args.length >= 10) t.setWorld(args[9]);
                    } catch (NumberFormatException ex) { sender.sendMessage(messages.get("plugin_prefix") + "§cInvalid number."); return; }
                }
                break;
            case "role":
                if (args.length < 4) { sender.sendMessage(messages.get("plugin_prefix") + "§cUsage: /if edit <name> role <staff|all>"); return; }
                String r = args[3].toLowerCase();
                if (!r.equals("staff") && !r.equals("all")) r = "all";
                t.setRole(r);
                break;
            case "command":
            case "commands":
                if (args.length < 4) { sender.sendMessage(messages.get("plugin_prefix") + "§cUsage: /if edit <name> command <cmd1>; <cmd2>; ..."); return; }
                String joined = String.join(" ", Arrays.copyOfRange(args,3,args.length));
                List<String> cmds = Arrays.stream(joined.split(";")).map(this::stripQuotes).filter(s -> !s.isBlank()).collect(Collectors.toList());
                if (cmds.isEmpty()) { sender.sendMessage(messages.get("plugin_prefix") + "§cNo commands provided."); return; }
                t.setCommands(cmds);
                break;
            case "cooldown":
                if (args.length < 4) { sender.sendMessage(messages.get("plugin_prefix") + "§cUsage: /if edit <name> cooldown <seconds>"); return; }
                try { t.setCooldown(Integer.parseInt(args[3])); } catch (NumberFormatException ex) { sender.sendMessage(messages.get("plugin_prefix") + "§cInvalid number."); return; }
                break;
            case "silent":
                if (args.length < 4) { sender.sendMessage(messages.get("plugin_prefix") + "§cUsage: /if edit <name> silent <true|false>"); return; }
                t.setSilent(Boolean.parseBoolean(args[3]));
                break;
            default:
                sender.sendMessage(messages.get("plugin_prefix") + "§cUnknown field: " + field);
                return;
        }
        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "trigger_edited", ph);
    }

    // ---------- UNDO ----------
    private void handleUndo(CommandSender sender) {
        Optional<Trigger> tOpt = undoManager.pop();
        if (tOpt.isEmpty()) { sender.sendMessage(messages.get("plugin_prefix") + "§cNothing to undo."); return; }
        Trigger t = tOpt.get();
        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>(); ph.put("name", t.getName());
        sendPref(sender, "trigger_restored", ph);
    }

    private String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length()>=2) return s.substring(1,s.length()-1);
        return s;
    }
}
