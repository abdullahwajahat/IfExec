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
import org.bukkit.Location;

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
        s.sendMessage(messages.getWithPrefix(key));
    }

    private void sendPref(CommandSender s, String key, Map<String,String> ph) {
        String msg = messages.get(key);
        for (Map.Entry<String,String> e : ph.entrySet()) msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        s.sendMessage(messages.getWithPrefix("") + " " + org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendPref(sender, "usage"); return true; }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "help": sendPref(sender, "help"); break;
                case "on":
                case "isin":
                    handleCreate(sender, args);
                    break;
                case "list":
                    if (args.length == 1) triggerManager.listAll(sender); else triggerManager.listOne(sender, args[1]);
                    break;
                case "listall":
                    triggerManager.listAll(sender);
                    break;
                case "remove":
                    triggerManager.handleRemove(sender, args);
                    break;
                case "disable":
                    triggerManager.handleEnableDisable(sender, args, false);
                    break;
                case "enable":
                    triggerManager.handleEnableDisable(sender, args, true);
                    break;
                case "reload":
                    plugin.getConfig().options().copyDefaults(true);
                    plugin.saveConfig();
                    plugin.getMessages().reload();
                    triggerManager.loadAll();
                    sendPref(sender, "reload_success");
                    break;
                case "edit":
                    triggerManager.handleEdit(sender, args);
                    break;
                case "undo":
                    undoManager.handleUndo(sender);
                    break;
                default:
                    if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("isin"))) {
                        handleCreate(sender, args);
                    } else {
                        sendPref(sender, "unknown_subcommand");
                    }
            }
        } catch (Exception ex) {
            sender.sendMessage(messages.getWithPrefix("") + " §cError: " + ex.getMessage());
            plugin.getLogger().severe("IfExec command error: " + ex);
        }
        return true;
    }

    // ---------- CREATE ----------
    private void handleCreate(CommandSender sender, String[] args) {
        // Parse creation syntax:
        // /if <selector> on <x y z [world]> then "cmd" ... [name <name>] [role staff|all] [silent true|false] [cooldown N] [message "text"]
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
                sendPref(sender, "created_usage");
                return;
            }
        }
        if (!mode.equals("on") && !mode.equals("isin")) { sendPref(sender, "created_usage"); return; }

        int thenIndex = -1;
        for (int i = idx+1; i < args.length; i++) if (args[i].equalsIgnoreCase("then")) { thenIndex = i; break; }
        if (thenIndex == -1) { sender.sendMessage(messages.getWithPrefix("") + "§cMissing 'then' in command."); return; }

        List<String> coordTokens = new ArrayList<>();
        for (int i = idx+1; i < thenIndex; i++) coordTokens.add(args[i]);

        List<String> tail = new ArrayList<>();
        for (int i = thenIndex+1; i < args.length; i++) tail.add(args[i]);

        // options parsing
        String name = null; String role = "all"; boolean silent = false; int cooldown = -1;
        Map<String,String> msgs = new HashMap<>();
        int nameIdx=-1, roleIdx=-1, silentIdx=-1, cdIdx=-1, msgIdx=-1;
        for (int i=0;i<tail.size();i++) {
            String t = tail.get(i).toLowerCase();
            if (t.equals("name")) nameIdx = i;
            if (t.equals("role")) roleIdx = i;
            if (t.equals("silent")) silentIdx = i;
            if (t.equals("cooldown")) cdIdx = i;
            if (t.equals("message")) msgIdx = i;
        }
        int optionStart = tail.size();
        if (nameIdx!=-1) optionStart = Math.min(optionStart, nameIdx);
        if (roleIdx!=-1) optionStart = Math.min(optionStart, roleIdx);
        if (silentIdx!=-1) optionStart = Math.min(optionStart, silentIdx);
        if (cdIdx!=-1) optionStart = Math.min(optionStart, cdIdx);
        if (msgIdx!=-1) optionStart = Math.min(optionStart, msgIdx);

        List<String> commandTokens = new ArrayList<>();
        for (int i=0;i<optionStart;i++) commandTokens.add(tail.get(i));

        if (nameIdx!=-1 && nameIdx+1<tail.size()) name = tail.get(nameIdx+1);
        if (roleIdx!=-1 && roleIdx+1<tail.size()) role = tail.get(roleIdx+1).toLowerCase();
        if (silentIdx!=-1 && silentIdx+1<tail.size()) silent = Boolean.parseBoolean(tail.get(silentIdx+1));
        if (cdIdx!=-1 && cdIdx+1<tail.size()) {
            try { cooldown = Integer.parseInt(tail.get(cdIdx+1)); } catch (NumberFormatException ignored) {}
        }
        if (msgIdx!=-1 && msgIdx+1<tail.size()) {
            String m = tail.get(msgIdx+1);
            if (m.startsWith("\"") && m.endsWith("\"") && m.length()>=2) m = m.substring(1,m.length()-1);
            msgs.put("all", m);
        }
        if (!role.equals("staff") && !role.equals("all")) role = "all";

        List<String> commands = commandTokens.stream().map(this::stripQuotes).filter(s->!s.isBlank()).collect(Collectors.toList());
        if (commands.isEmpty()) { sender.sendMessage(messages.getWithPrefix("") + "§cNo commands provided."); return; }

        Trigger t = new Trigger();
        if (name == null || name.isBlank()) name = generateName();
        t.setName(name);
        t.setRole(role);
        t.setCommands(commands);

        if (mode.equals("on")) {
            if (coordTokens.size() < 3) { sender.sendMessage(messages.getWithPrefix("") + "§cNot enough coordinates."); return; }
            try {
                int x = Integer.parseInt(coordTokens.get(0));
                int y = Integer.parseInt(coordTokens.get(1));
                int z = Integer.parseInt(coordTokens.get(2));
                t.setType(Trigger.Type.BLOCK);
                t.setX(x); t.setY(y); t.setZ(z);
                if (coordTokens.size() >= 4) t.setWorld(coordTokens.get(3));
                else if (sender instanceof Player) t.setWorld(((Player)sender).getWorld().getName());
                else { sender.sendMessage(messages.getWithPrefix("") + "§cWorld required when run from console."); return; }
            } catch (NumberFormatException ex) { sender.sendMessage(messages.getWithPrefix("") + "§cInvalid coordinate."); return; }
        } else { // region
            if (coordTokens.size() < 6) { sender.sendMessage(messages.getWithPrefix("") + "§cNot enough region coordinates."); return; }
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
                else { sender.sendMessage(messages.getWithPrefix("") + "§cWorld required when run from console."); return; }
            } catch (NumberFormatException ex) { sender.sendMessage(messages.getWithPrefix("") + "§cInvalid coordinate."); return; }
        }

        t.setEnabled(true);
        t.setCooldown(cooldown);
        t.setSilent(silent);
        t.setMessages(msgs);

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

    private String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length()>=2) return s.substring(1,s.length()-1);
        return s;
    }
}
