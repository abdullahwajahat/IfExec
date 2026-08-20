package com.ifexec.commands;

import com.ifexec.IfExec;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.manager.UndoManager;
import com.ifexec.model.Trigger;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
        s.sendMessage(messages.getWithPrefix("") + " " + ChatColor.translateAlternateColorCodes('&', msg));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendPref(sender, "usage"); return true; }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "help": sendPref(sender, "help"); break;
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
                    // Properly reload the config from the disk
                    plugin.getConfigManager().reloadConfig();
                    plugin.getMessages().reload();
                    plugin.getTriggerManager().loadAll();
                    sendPref(sender, "reload_success");
                    break;
                case "edit":
                    handleEdit(sender, args);
                    break;
                case "undo":
                    handleUndo(sender);
                    break;
                default:
                    // Matches /if <selector> <on|isin> ... or /if <on|isin> ...
                    handleCreate(sender, args);
                    break;
            }
        } catch (Exception ex) {
            sender.sendMessage(messages.getWithPrefix("") + " §cError: " + ex.getMessage());
            plugin.getLogger().severe("IfExec command error: " + ex);
        }
        return true;
    }

    // ---------- CREATE ----------
    private void handleCreate(CommandSender sender, String[] args) {
        int modeIndex = 1;
        String mode;

        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("isin")) {
            mode = args[0].toLowerCase();
            modeIndex = 0;
        } else if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("isin"))) {
            mode = args[1].toLowerCase();
            modeIndex = 1;
        } else {
            sendPref(sender, "unknown_subcommand");
            return;
        }

        Trigger t = new Trigger();
        t.setRole("all");
        int nameIdx = -1;
        List<String> coordTokens = new ArrayList<>();

        if (mode.equals("on")) {
            // /if <selector> on <x> <y> <z> <name> <cmd>
            int requiredArgs = (modeIndex == 0) ? 5 : 6;
            if (args.length < requiredArgs) {
                sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if <selector> on <x> <y> <z> <name> \"<command>\"");
                return;
            }
            coordTokens.add(args[modeIndex + 1]);
            coordTokens.add(args[modeIndex + 2]);
            coordTokens.add(args[modeIndex + 3]);
            nameIdx = modeIndex + 4;
        } else {
            // mode is "isin"
            int checkIdx = modeIndex + 1;
            if (args.length <= checkIdx) {
                sendPref(sender, "created_usage");
                return;
            }

            // Check if coordinates were manually supplied or if WorldEdit is being used
            if (args[checkIdx].matches("-?\\d+")) {
                int requiredArgs = (modeIndex == 0) ? 8 : 9;
                if (args.length < requiredArgs) {
                    sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if <selector> isin <x1> <y1> <z1> <x2> <y2> <z2> <name> \"<command>\"");
                    return;
                }
                for (int i = 1; i <= 6; i++) {
                    coordTokens.add(args[modeIndex + i]);
                }
                nameIdx = modeIndex + 7;
            } else {
                // WorldEdit selection hook
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(messages.getWithPrefix("") + " §cConsole must provide manual coordinates.");
                    return;
                }

                Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
                if (wePlugin == null || !(wePlugin instanceof WorldEditPlugin we)) {
                    sender.sendMessage(messages.getWithPrefix("") + " §cWorldEdit is not installed! Please provide coordinates manually.");
                    return;
                }

                try {
                    Region sel = we.getSession(p).getSelection(we.getSession(p).getSelectionWorld());
                    if (sel == null) throw new NullPointerException();
                    t.setType(Trigger.Type.REGION);
                    t.setX1(sel.getMinimumPoint().getBlockX());
                    t.setY1(sel.getMinimumPoint().getBlockY());
                    t.setZ1(sel.getMinimumPoint().getBlockZ());
                    t.setX2(sel.getMaximumPoint().getBlockX());
                    t.setY2(sel.getMaximumPoint().getBlockY());
                    t.setZ2(sel.getMaximumPoint().getBlockZ());
                    t.setWorld(p.getWorld().getName());
                } catch (Exception e) {
                    sender.sendMessage(messages.getWithPrefix("") + " §cPlease make a WorldEdit selection first, or specify coordinates manually.");
                    return;
                }
                nameIdx = modeIndex + 1;
            }
        }

        if (nameIdx >= args.length) {
            sender.sendMessage(messages.getWithPrefix("") + " §cPlease provide a trigger name and command.");
            return;
        }

        String name = args[nameIdx];
        t.setName(name);

        StringBuilder cmdBuilder = new StringBuilder();
        for (int i = nameIdx + 1; i < args.length; i++) {
            cmdBuilder.append(args[i]).append(" ");
        }

        String rawCmd = cmdBuilder.toString().trim();
        if (rawCmd.isEmpty()) {
            sender.sendMessage(messages.getWithPrefix("") + " §cNo command provided.");
            return;
        }
        t.setCommands(Collections.singletonList(stripQuotes(rawCmd)));

        // Parse manual coordinates if present
        if (!coordTokens.isEmpty()) {
            try {
                if (mode.equals("on")) {
                    t.setType(Trigger.Type.BLOCK);
                    t.setX(Integer.parseInt(coordTokens.get(0)));
                    t.setY(Integer.parseInt(coordTokens.get(1)));
                    t.setZ(Integer.parseInt(coordTokens.get(2)));
                    if (sender instanceof Player p) t.setWorld(p.getWorld().getName());
                    else t.setWorld("world");
                } else {
                    t.setType(Trigger.Type.REGION);
                    t.setX1(Integer.parseInt(coordTokens.get(0)));
                    t.setY1(Integer.parseInt(coordTokens.get(1)));
                    t.setZ1(Integer.parseInt(coordTokens.get(2)));
                    t.setX2(Integer.parseInt(coordTokens.get(3)));
                    t.setY2(Integer.parseInt(coordTokens.get(4)));
                    t.setZ2(Integer.parseInt(coordTokens.get(5)));
                    if (sender instanceof Player p) t.setWorld(p.getWorld().getName());
                    else t.setWorld("world");
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(messages.getWithPrefix("") + " §cInvalid coordinates.");
                return;
            }
        }

        t.setEnabled(true);
        t.setCooldown(-1);
        t.setSilent(false);
        t.setMessages(new HashMap<>());

        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>();
        ph.put("name", t.getName());
        sendPref(sender, "trigger_created", ph);
    }

    // ---------- LIST ----------
    private void listAll(CommandSender sender) {
        Collection<Trigger> all = triggerManager.getAll();
        sender.sendMessage(messages.getWithPrefix("") + " §6Trigger List:");
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
            sender.sendMessage(messages.getWithPrefix("") + " §eClick a trigger below to remove it (Shift-Click to paste):");
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

        sender.sendMessage(messages.getWithPrefix("") + " §cAre you sure you want to remove trigger §f" + name + "§c?");
        TextComponent confirm = new TextComponent("§a[Confirm] ");
        confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/if remove confirm " + name));
        confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to permanently delete " + name).create()));
        TextComponent cancel = new TextComponent("§c[Cancel]");
        cancel.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/if list"));
        cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to cancel").create()));
        sender.spigot().sendMessage(confirm, cancel);
    }

    private void doRemoveConfirmed(CommandSender sender, String name) {
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }
        Trigger t = opt.get();
        UUID remover = (sender instanceof Player p) ? p.getUniqueId() : null;
        plugin.getUndoManager().push(remover, t);
        triggerManager.remove(name);
        Map<String,String> ph = new HashMap<>(); ph.put("name", name);
        sendPref(sender, "trigger_removed", ph);
        sender.sendMessage(messages.getWithPrefix("") + " §7Type /if undo to restore. (expires in " + plugin.getConfigManager().getConfig().getInt("undo_timeout", 30) + "s)");
    }

    // ---------- ENABLE/DISABLE ----------
    private void handleEnableDisable(CommandSender sender, String[] args, boolean enable) {
        if (!sender.hasPermission("ifexec.admin")) { sendPref(sender, "no_permission"); return; }
        if (args.length < 2) { sender.sendMessage(messages.getWithPrefix("") + " §eUsage: /if " + (enable ? "enable <name>" : "disable <name>")); return; }
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
        if (args.length < 3) { sender.sendMessage(messages.getWithPrefix("") + " §eUsage: /if edit <name> <field> <value>"); return; }
        String name = args[1];
        String field = args[2].toLowerCase();
        Optional<Trigger> opt = triggerManager.get(name);
        if (opt.isEmpty()) { Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "no_trigger", ph); return; }
        Trigger t = opt.get();

        try {
            switch (field) {
                case "coords":
                    if (t.getType() == Trigger.Type.BLOCK) {
                        if (args.length < 6) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> coords <x> <y> <z> [world]"); return; }
                        t.setX(Integer.parseInt(args[3])); t.setY(Integer.parseInt(args[4])); t.setZ(Integer.parseInt(args[5]));
                        if (args.length >= 7) t.setWorld(args[6]);
                    } else {
                        if (args.length < 9) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> coords <x1> <y1> <z1> <x2> <y2> <z2> [world]"); return; }
                        t.setX1(Integer.parseInt(args[3])); t.setY1(Integer.parseInt(args[4])); t.setZ1(Integer.parseInt(args[5]));
                        t.setX2(Integer.parseInt(args[6])); t.setY2(Integer.parseInt(args[7])); t.setZ2(Integer.parseInt(args[8]));
                        if (args.length >= 10) t.setWorld(args[9]);
                    }
                    break;
                case "world":
                    if (args.length < 4) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> world <worldname>"); return; }
                    t.setWorld(args[3]);
                    break;
                case "role":
                    if (args.length < 4) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> role <staff|all>"); return; }
                    String r = args[3].toLowerCase();
                    if (!r.equals("staff") && !r.equals("all")) r = "all";
                    t.setRole(r);
                    break;
                case "command":
                case "commands":
                    if (args.length < 4) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> command <cmd1>; <cmd2>; ..."); return; }
                    String joined = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    List<String> cmds = Arrays.stream(joined.split(";")).map(this::stripQuotes).filter(s -> !s.isBlank()).collect(Collectors.toList());
                    if (cmds.isEmpty()) { sender.sendMessage(messages.getWithPrefix("") + " §cNo commands provided."); return; }
                    t.setCommands(cmds);
                    break;
                case "cooldown":
                    if (args.length < 4) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> cooldown <seconds>"); return; }
                    t.setCooldown(Integer.parseInt(args[3]));
                    break;
                case "silent":
                    if (args.length < 4) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> silent <true|false>"); return; }
                    t.setSilent(Boolean.parseBoolean(args[3]));
                    break;
                case "message":
                    if (args.length < 5) { sender.sendMessage(messages.getWithPrefix("") + " §cUsage: /if edit <name> message <all|staff> \"<text>\""); return; }
                    String target = args[3].toLowerCase();
                    String msg = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    if (msg.startsWith("\"") && msg.endsWith("\"") && msg.length() >= 2) msg = msg.substring(1, msg.length()-1);
                    Map<String,String> mm = t.getMessages(); mm.put(target, msg); t.setMessages(mm);
                    break;
                default:
                    sender.sendMessage(messages.getWithPrefix("") + " §cUnknown field: " + field);
                    return;
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(messages.getWithPrefix("") + " §cInvalid number.");
            return;
        }

        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>(); ph.put("name", name); sendPref(sender, "trigger_edited", ph);
    }

    // ---------- UNDO ----------
    private void handleUndo(CommandSender sender) {
        if (!undoManager.hasUndo(sender)) {
            sender.sendMessage(messages.getWithPrefix("undo_empty"));
            return;
        }
        Optional<Trigger> tOpt = undoManager.pop(sender);
        if (tOpt.isEmpty()) { sender.sendMessage(messages.getWithPrefix("undo_expired")); return; }
        Trigger t = tOpt.get();
        triggerManager.add(t);
        Map<String,String> ph = new HashMap<>(); ph.put("name", t.getName());
        sendPref(sender, "undo_success", ph);
    }

    private String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length() - 1);
        return s;
    }
}
