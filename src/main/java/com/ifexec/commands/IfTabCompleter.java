package com.ifexec.commands;

import com.ifexec.IfExec;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.*;
import java.util.stream.Collectors;

public class IfTabCompleter implements TabCompleter {
    private final IfExec plugin;

    public IfTabCompleter(IfExec plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        try {
            if (args.length == 1) {
                List<String> subs = Arrays.asList("help","list","listall","remove","disable","enable","reload","edit","undo","on","isin");
                for (String s : subs) if (s.startsWith(args[0].toLowerCase())) res.add(s);
                for (String n : plugin.getTriggerManager().names()) if (n.startsWith(args[0])) res.add(n);
                return res.stream().distinct().collect(Collectors.toList());
            }

            if (args.length == 2) {
                String first = args[0].toLowerCase();
                if (first.equals("remove") || first.equals("disable") || first.equals("enable") || first.equals("list") || first.equals("edit")) {
                    for (String n : plugin.getTriggerManager().names()) if (n.startsWith(args[1])) res.add(n);
                    return res;
                }
            }

            if (args.length >= 3 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("role")) {
                return Arrays.asList("staff","all");
            }

            if (args.length >= 3 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("silent")) {
                return Arrays.asList("true","false");
            }

            if (args.length >= 3 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("cooldown")) {
                return Arrays.asList("1","3","5","10","30");
            }

            // coords suggestion for player when using 'on'
            if ((args[0].equalsIgnoreCase("on") || (args.length >= 2 && args[1].equalsIgnoreCase("on"))) && sender instanceof Player) {
                Player p = (Player) sender;
                int x = p.getLocation().getBlockX(), y = p.getLocation().getBlockY(), z = p.getLocation().getBlockZ();
                return Arrays.asList(String.valueOf(x), String.valueOf(y), String.valueOf(z), p.getWorld().getName());
            }

            // coords suggestion for player when using 'isin'
            if ((args[0].equalsIgnoreCase("isin") || (args.length >= 2 && args[1].equalsIgnoreCase("isin"))) && sender instanceof Player) {
                Player p = (Player) sender;
                int x = p.getLocation().getBlockX(), y = p.getLocation().getBlockY(), z = p.getLocation().getBlockZ();
                return Arrays.asList(String.valueOf(x), String.valueOf(y), String.valueOf(z));
            }

            // server commands for 'then' / command suggestions
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("then") || args[0].equalsIgnoreCase("commands") || (args.length >= 2 && args[1].equalsIgnoreCase("then"))) {
                    Set<String> cmdSet = new HashSet<>();
                    for (org.bukkit.plugin.Plugin pl : Bukkit.getPluginManager().getPlugins()) {
                        PluginDescriptionFile pdf = pl.getDescription();
                        if (pdf.getCommands() != null) cmdSet.addAll(pdf.getCommands().keySet());
                    }
                    List<String> cmds = cmdSet.stream().filter(s -> !s.toLowerCase().contains("worldedit")).sorted().collect(Collectors.toList());
                    return cmds;
                }
            }

        } catch (Exception ex) {
            plugin.getLogger().warning("TabComplete error: " + ex.getMessage());
        }
        return res;
    }
}
