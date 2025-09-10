package com.ifexec.commands;

import com.ifexec.IfExec;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
            if (args.length == 0) return Collections.emptyList();

            String sub = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            // ---------- Subcommand suggestions ----------
            if (args.length == 1) {
                List<String> subs = Arrays.asList(
                        "help", "list", "listall", "remove", "disable", "enable", "reload", "edit", "undo", "on", "isin"
                );
                completions.addAll(subs.stream().filter(s -> s.startsWith(sub)).collect(Collectors.toList()));

                // Also suggest existing trigger names
                completions.addAll(plugin.getTriggerManager().names().stream()
                        .filter(name -> name.startsWith(sub)).collect(Collectors.toList()));

                return completions.stream().distinct().sorted().collect(Collectors.toList());
            }

            // ---------- Second argument ----------
            if (args.length == 2) {
                if (Arrays.asList("remove", "disable", "enable", "list", "edit").contains(sub)) {
                    completions.addAll(plugin.getTriggerManager().names().stream()
                            .filter(name -> name.startsWith(args[1])).collect(Collectors.toList()));
                    return completions;
                }

                // If using /if on or /if isin, suggest coordinates for player
                if (Arrays.asList("on", "isin").contains(sub) && sender instanceof Player) {
                    Player p = (Player) sender;
                    completions.addAll(Arrays.asList(
                            String.valueOf(p.getLocation().getBlockX()),
                            String.valueOf(p.getLocation().getBlockY()),
                            String.valueOf(p.getLocation().getBlockZ()),
                            sub.equals("on") ? p.getWorld().getName() : ""
                    ));
                    return completions;
                }
            }

            // ---------- Role / Silent / Cooldown ----------
            if (args.length >= 3 && sub.equalsIgnoreCase("edit")) {
                String field = args[2].toLowerCase();
                switch (field) {
                    case "role":
                        return Arrays.asList("staff", "all");
                    case "silent":
                        return Arrays.asList("true", "false");
                    case "cooldown":
                        return Arrays.asList("1", "3", "5", "10", "30");
                }
            }

            // ---------- Coordinate suggestions ----------
            if ((sub.equalsIgnoreCase("on") || sub.equalsIgnoreCase("isin")) && sender instanceof Player) {
                Player p = (Player) sender;
                // Automatically fill all coordinates at once if player is creating a trigger
                if (args.length >= 2 && args.length <= (sub.equals("on") ? 4 : 6)) {
                    List<String> coords = new ArrayList<>();
                    coords.add(String.valueOf(p.getLocation().getBlockX()));
                    coords.add(String.valueOf(p.getLocation().getBlockY()));
                    coords.add(String.valueOf(p.getLocation().getBlockZ()));
                    if (sub.equals("on")) coords.add(p.getWorld().getName());
                    return coords;
                }
            }

            // ---------- Suggest server commands after "then" ----------
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("then") || args[0].equalsIgnoreCase("commands") || 
                    (args.length >= 2 && args[1].equalsIgnoreCase("then"))) {
                    Set<String> cmdSet = new HashSet<>();
                    for (org.bukkit.plugin.Plugin pl : Bukkit.getPluginManager().getPlugins()) {
                        if (pl.getDescription().getCommands() != null) {
                            cmdSet.addAll(pl.getDescription().getCommands().keySet());
                        }
                    }
                    return cmdSet.stream()
                            .filter(cmd -> !cmd.toLowerCase().contains("worldedit"))
                            .sorted().collect(Collectors.toList());
                }
            }

        } catch (Exception ex) {
            plugin.getLogger().warning("TabComplete error: " + ex.getMessage());
        }

        return Collections.emptyList();
    }
}
