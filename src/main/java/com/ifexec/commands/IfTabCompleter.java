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

    // Returns the prefix from messages.yml
    private String prefix() {
        return messages.get("plugin_prefix");
    }

    // Sends a message with prefix
    private void send(CommandSender sender, String key) {
        String msg = messages.get(key).replace(prefix(), "");
        sender.sendMessage(prefix() + msg);
    }

    // Sends a message with prefix and placeholders
    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = messages.get(key).replace(prefix(), "");
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        }
        sender.sendMessage(prefix() + msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(prefix() + "§eIfExec: use /if help");
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "help":
                    sender.sendMessage(prefix() + "§6IfExec commands: /if <selector> on <coords> then \"cmd\" ... [name <name>] [role staff|all], /if listall, /if list <name>, /if edit <name> ..., /if remove <name>, /if undo");
                    break;
                case "on":
                case "isin":
                    handleCreate(sender, args);
                    break;
                case "list":
                    if (args.length == 1) listAll(sender);
                    else listOne(sender, args[1]);
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
                    send(sender, "trigger_edited");
                    break;
                case "edit":
                    handleEdit(sender, args);
                    break;
                case "undo":
                    handleUndo(sender);
                    break;
                default:
                    if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("isin"))) {
                        handleCreate(sender, args);
                    } else {
                        sender.sendMessage(prefix() + "§cUnknown subcommand. Use /if help");
                    }
            }
        } catch (Exception ex) {
            sender.sendMessage(prefix() + "§cError: " + ex.getMessage());
            plugin.getLogger().severe("IfExec command error: " + ex);
        }

        return true;
    }

    // ---------------- CREATE ----------------
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
                send(sender, "created_usage");
                return;
            }
        }

        if (!mode.equals("on") && !mode.equals("isin")) {
            send(sender, "created_usage");
            return;
        }

        int thenIndex = -1;
        for (int i = idx + 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("then")) {
                thenIndex = i;
                break;
            }
        }

        if (thenIndex == -1) {
            sender.sendMessage(prefix() + "§cMissing 'then' in command.");
            return;
        }

        List<String> coordTokens = new ArrayList<>();
        for (int i = idx + 1; i < thenIndex; i++) coordTokens.add(args[i]);

        List<String> tail = new ArrayList<>();
        for (int i = thenIndex + 1; i < args.length; i++) tail.add(args[i]);

        String name = null;
        String role = "all";
        int nameIdx = -1, roleIdx = -1;

        for (int i = 0; i < tail.size(); i++) {
            if (tail.get(i).equalsIgnoreCase("name")) nameIdx = i;
            if (tail.get(i).equalsIgnoreCase("role")) roleIdx = i;
        }

        int optionStart = tail.size();
        if (nameIdx != -1) optionStart = Math.min(optionStart, nameIdx);
        if (roleIdx != -1) optionStart = Math.min(optionStart, roleIdx);

        List<String> commandTokens = new ArrayList<>();
        for (int i = 0; i < optionStart; i++) commandTokens.add(tail.get(i));

        if (nameIdx != -1 && nameIdx + 1 < tail.size()) name = tail.get(nameIdx + 1);
        if (roleIdx != -1 && roleIdx + 1 < tail.size()) role = tail.get(roleIdx + 1).toLowerCase();
        if (!role.equals("staff") && !role.equals("all")) role = "all";

        List<String> commands = commandTokens.stream().map(this::stripQuotes).filter(s -> !s.isBlank()).collect(Collectors.toList());
        if (commands.isEmpty()) {
            sender.sendMessage(prefix() + "§cNo commands provided.");
            return;
        }

        Trigger t = new Trigger();
        if (name == null || name.isBlank()) name = generateName();
        t.setName(name);
        t.setRole(role);
        t.setCommands(commands);

        // Set coordinates and world
        if (mode.equals("on")) {
            if (coordTokens.size() < 3) {
                sender.sendMessage(prefix() + "§cNot enough coordinates.");
                return;
            }
            try {
                int x = Integer.parseInt(coordTokens.get(0));
                int y = Integer.parseInt(coordTokens.get(1));
                int z = Integer.parseInt(coordTokens.get(2));
                t.setType(Trigger.Type.BLOCK);
                t.setX(x);
                t.setY(y);
                t.setZ(z);

                if (coordTokens.size() >= 4) t.setWorld(coordTokens.get(3));
                else if (sender instanceof Player) t.setWorld(((Player) sender).getWorld().getName());
                else { sender.sendMessage(prefix() + "§cWorld required when run from console."); return; }
            } catch (NumberFormatException ex) {
                sender.sendMessage(prefix() + "§cInvalid coordinate.");
                return;
            }
        } else {
            if (coordTokens.size() < 6) {
                sender.sendMessage(prefix() + "§cNot enough region coordinates.");
                return;
            }
            try {
                t.setType(Trigger.Type.REGION);
                t.setX1(Integer.parseInt(coordTokens.get(0)));
                t.setY1(Integer.parseInt(coordTokens.get(1)));
                t.setZ1(Integer.parseInt(coordTokens.get(2)));
                t.setX2(Integer.parseInt(coordTokens.get(3)));
                t.setY2(Integer.parseInt(coordTokens.get(4)));
                t.setZ2(Integer.parseInt(coordTokens.get(5)));

                if (coordTokens.size() >= 7) t.setWorld(coordTokens.get(6));
                else if (sender instanceof Player) t.setWorld(((Player) sender).getWorld().getName());
                else { sender.sendMessage(prefix() + "§cWorld required when run from console."); return; }
            } catch (NumberFormatException ex) {
                sender.sendMessage(prefix() + "§cInvalid coordinate.");
                return;
            }
        }

        t.setEnabled(true);
        t.setCooldown(-1);
        t.setSilent(false);
        t.setMessages(new HashMap<>());

        triggerManager.add(t);

        Map<String, String> ph = new HashMap<>();
        ph.put("name", t.getName());
        send(sender, "trigger_created", ph);
    }

    private String generateName() {
        int i = 1;
        while (true) {
            String n = "trigger_" + i;
            if (!triggerManager.exists(n)) return n;
            i++;
        }
    }

    private String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length() - 1);
        return s;
    }

    // ---------------- LIST ALL ----------------
    private void listAll(CommandSender sender) {
        Collection<Trigger> all = triggerManager.getAll();
        sender.sendMessage(prefix() + "§eTriggers: §f(click a name to view details, shift+click to paste)");
        if (all.isEmpty()) {
            sender.sendMessage("§7- §c(no triggers)");
            return;
        }
        for (Trigger t : all) {
            TextComponent comp = new TextComponent("§7- §a[" + t.getName() + "]");
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if list " + t.getName()));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to run /if list " + t.getName() + "\nShift-Click to paste").create()));
            sender.spigot().sendMessage(comp);
        }
    }

    // ---------------- LIST ONE ----------------
    private void listOne(CommandSender sender, String name) {
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("name", name);
            send(sender, "no_trigger", ph);
            return;
        }

        Trigger t = opt.get();
        sender.sendMessage(prefix() + "§eName: §f" + t.getName());
        sender.sendMessage(prefix() + "§eWorld: §f" + t.getWorld());
        sender.sendMessage(prefix() + "§eType: §f" + (t.getType() == Trigger.Type.BLOCK ? "block" : "region"));

        if (t.getType() == Trigger.Type.BLOCK)
            sender.sendMessage(prefix() + "§eCoords: §f" + t.getX() + " " + t.getY() + " " + t.getZ());
        else
            sender.sendMessage(prefix() + "§eCoords: §f" + t.getX1() + " " + t.getY1() + " " + t.getZ1() + "  -  " + t.getX2() + " " + t.getY2() + " " + t.getZ2());

        sender.sendMessage(prefix() + "§eRole: §f" + t.getRole());
        int cd = (t.getCooldown() >= 0) ? t.getCooldown() : plugin.getConfigManager().getConfig().getInt("default_cooldown", 3);
        sender.sendMessage(prefix() + "§eCooldown: §f" + cd + "s");
        sender.sendMessage(prefix() + "§eCommands:");
        for (String c : t.getCommands()) sender.sendMessage(prefix() + " §7- §f" + c);
    }

    // ---------------- REMOVE ----------------
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) { send(sender, "remove_usage"); return; }
        String name = args[1];
        if (!triggerManager.exists(name)) { send(sender, "no_trigger", Map.of("name", name)); return; }
        triggerManager.remove(name);
        send(sender, "trigger_removed", Map.of("name", name));
    }

    // ---------------- ENABLE / DISABLE ----------------
    private void handleEnableDisable(CommandSender sender, String[] args, boolean enable) {
        if (args.length < 2) { send(sender, "enable_usage"); return; }
        String name = args[1];
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { send(sender, "no_trigger", Map.of("name", name)); return; }
        Trigger t = opt.get();
        t.setEnabled(enable);
        triggerManager.save(t);
        send(sender, enable ? "trigger_enabled" : "trigger_disabled", Map.of("name", name));
    }

    // ---------------- UNDO ----------------
    private void handleUndo(CommandSender sender) {
        if (!undoManager.hasUndo()) { send(sender, "undo_empty"); return; }
        undoManager.undo();
        send(sender, "undo_success");
    }

    // ---------------- EDIT ----------------
    private void handleEdit(CommandSender sender, String[] args) {
        if (args.length < 2) { send(sender, "edit_usage"); return; }
        String name = args[1];
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { send(sender, "no_trigger", Map.of("name", name)); return; }
        Trigger t = opt.get();

        // Only allow editing commands for simplicity
        if (args.length < 4) { send(sender, "edit_usage"); return; }
        if (!args[2].equalsIgnoreCase("cmd")) { send(sender, "edit_usage"); return; }

        List<String> newCommands = new ArrayList<>();
        for (int i = 3; i < args.length; i++) newCommands.add(stripQuotes(args[i]));
        t.setCommands(newCommands);
        triggerManager.save(t);

        send(sender, "trigger_edited", Map.of("name", t.getName()));
    }
}
