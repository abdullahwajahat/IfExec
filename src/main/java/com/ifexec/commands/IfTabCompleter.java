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
        try {
            if (args.length == 1) {
                String a = args[0].toLowerCase();
                List<String> list = new ArrayList<>();
                // selectors first
                List<String> selectors = Arrays.asList("@s","@p","@a");
                for (String s : selectors) if (s.startsWith(a)) list.add(s);
                // player names
                list.addAll(Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).filter(n -> n.toLowerCase().startsWith(a)).collect(Collectors.toList()));
                // subcommands
                List<String> subs = Arrays.asList("on","isin","help","list","listall","remove","edit","enable","disable","undo","reload","config");
                for (String s : subs) if (s.startsWith(a)) list.add(s);
                // trigger names
                for (String n : plugin.getTriggerManager().names()) if (n.toLowerCase().startsWith(a)) list.add(n);
                return list.stream().distinct().collect(Collectors.toList());
            }

            if (args.length == 2) {
                String first = args[0].toLowerCase(), a = args[1].toLowerCase();
                if (first.equals("remove") || first.equals("disable") || first.equals("enable") || first.equals("list") || first.equals("edit"))
                    return plugin.getTriggerManager().names().stream().filter(n -> n.toLowerCase().startsWith(a)).collect(Collectors.toList());
            }

            if (args.length >= 3 && args[0].equalsIgnoreCase("edit")) {
                if (args.length == 3) return Arrays.asList("coords","commands","role","silent","cooldown","message").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("role")) return Arrays.asList("staff","all").stream().filter(s->s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("silent")) return Arrays.asList("true","false").stream().filter(s->s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("cooldown")) return Arrays.asList("1","3","5","10","30").stream().filter(s->s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("on") && sender instanceof Player p) {
                    String coordsFull = p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ() + " " + p.getWorld().getName();
                    return Collections.singletonList(coordsFull);
                }
                if (args[i].equalsIgnoreCase("isin") && sender instanceof Player p) {
                    String coordsFull = p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ();
                    return Collections.singletonList(coordsFull);
                }
            }

            // after 'then' suggest server commands
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("then") || (args.length >= 2 && args[1].equalsIgnoreCase("then"))) {
                    Set<String> cmdSet = new HashSet<>();
                    for (org.bukkit.plugin.Plugin pl : Bukkit.getPluginManager().getPlugins()) {
                        if (pl.getDescription().getCommands() != null) cmdSet.addAll(pl.getDescription().getCommands().keySet());
                    }
                    return cmdSet.stream().filter(s -> !s.toLowerCase().contains("worldedit")).sorted().collect(Collectors.toList());
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("TabComplete error: " + ex.getMessage());
        }
        return Collections.emptyList();
    }
}
