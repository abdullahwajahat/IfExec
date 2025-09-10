package com.ifexec.commands;

import com.ifexec.IfExec;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class IfTabCompleter implements TabCompleter {
    private final IfExec plugin;

    public IfTabCompleter(IfExec plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> res = new ArrayList<>();

        try {
            // first argument suggestions
            if (args.length == 1) {
                List<String> subs = Arrays.asList("help","list","listall","remove","disable","enable","reload","edit","undo","on","isin");
                for (String s : subs) if (s.startsWith(args[0].toLowerCase())) res.add(s);
                // add trigger names
                for (String n : plugin.getTriggerManager().names()) if (n.startsWith(args[0])) res.add(n);
                return res.stream().distinct().collect(Collectors.toList());
            }

            // second argument suggestions
            if (args.length == 2) {
                String first = args[0].toLowerCase();
                if (first.equals("remove") || first.equals("disable") || first.equals("enable") || first.equals("list") || first.equals("edit")) {
                    for (String n : plugin.getTriggerManager().names()) if (n.startsWith(args[1])) res.add(n);
                    return res;
                }
            }

            // edit field suggestions
            if (args.length >= 3 && args[0].equalsIgnoreCase("edit")) {
                switch (args[2].toLowerCase()) {
                    case "role": return Arrays.asList("staff","all");
                    case "silent": return Arrays.asList("true","false");
                    case "cooldown": return Arrays.asList("1","3","5","10","30");
                }
            }

            // coords suggestion for "on" or "isin"
            if ((args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("isin")) && sender instanceof Player) {
                Player p = (Player) sender;
                int x = p.getLocation().getBlockX();
                int y = p.getLocation().getBlockY();
                int z = p.getLocation().getBlockZ();
                if (args[0].equalsIgnoreCase("on")) {
                    return Arrays.asList(String.valueOf(x), String.valueOf(y), String.valueOf(z), p.getWorld().getName());
                } else {
                    return Arrays.asList(String.valueOf(x), String.valueOf(y), String.valueOf(z));
                }
            }

            // server command suggestions after "then"
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("then") || args[0].equalsIgnoreCase("commands")) {
                    Set<String> cmdSet = new HashSet<>();
                    for (org.bukkit.plugin.Plugin pl : Bukkit.getPluginManager().getPlugins()) {
                        if (pl.getDescription().getCommands() != null)
                            cmdSet.addAll(pl.getDescription().getCommands().keySet());
                    }
                    return cmdSet.stream().filter(s -> !s.toLowerCase().contains("worldedit")).sorted().collect(Collectors.toList());
                }
            }

        } catch (Exception ex) {
            plugin.getLogger().warning("TabComplete error: " + ex.getMessage());
        }

        return res;
    }
}
