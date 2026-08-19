package com.ifexec.commands;

import com.ifexec.IfExec;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.World;

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
                List<String> selectors = Arrays.asList("@s", "@p", "@a");
                for (String s : selectors) if (s.startsWith(a)) list.add(s);
                list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(a)).collect(Collectors.toList()));
                List<String> subs = Arrays.asList("on", "isin", "help", "list", "listall", "remove", "edit", "enable", "disable", "undo", "reload");
                for (String s : subs) if (s.startsWith(a)) list.add(s);
                for (String n : plugin.getTriggerManager().names()) if (n.toLowerCase().startsWith(a)) list.add(n);
                return list.stream().distinct().collect(Collectors.toList());
            }

            if (args.length == 2) {
                String first = args[0].toLowerCase(), a = args[1].toLowerCase();
                if (first.equals("remove") || first.equals("disable") || first.equals("enable") || first.equals("list") || first.equals("edit")) {
                    return plugin.getTriggerManager().names().stream().filter(n -> n.toLowerCase().startsWith(a)).collect(Collectors.toList());
                }
                if (first.startsWith("@") || Bukkit.getPlayerExact(args[0]) != null) {
                    return Arrays.asList("on", "isin").stream().filter(s -> s.startsWith(a)).collect(Collectors.toList());
                }
            }

            if (args.length >= 3 && args[0].equalsIgnoreCase("edit")) {
                if (args.length == 3) return Arrays.asList("coords", "command", "role", "silent", "cooldown", "message", "world").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("role")) return Arrays.asList("staff", "all").stream().filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("silent")) return Arrays.asList("true", "false").stream().filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("cooldown")) return Arrays.asList("1", "3", "5", "10", "30").stream().filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                if (args.length == 4 && args[2].equalsIgnoreCase("world")) return Bukkit.getWorlds().stream().map(World::getName).filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            }

            // Coordinates autocompletion for player location
            if (sender instanceof Player p) {
                String prev = args[args.length - 2].toLowerCase();
                if (prev.equals("on")) {
                    return Collections.singletonList(p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ());
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("TabComplete error: " + ex.getMessage());
        }
        return Collections.emptyList();
    }
}
