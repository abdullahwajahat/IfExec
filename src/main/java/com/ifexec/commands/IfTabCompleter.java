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
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> res = new ArrayList<>();

        if (args.length == 1) {
            res.addAll(Arrays.asList("help","reload","list","listall","remove","enable","disable","edit","undo","on","isin"));
            res.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            res.addAll(Arrays.asList("@p","@a","@s"));
        } else if (args[0].equalsIgnoreCase("edit") && args.length == 3) {
            res.addAll(Arrays.asList("coords","role","commands","cooldown","silent","message"));
        } else if (args[0].equalsIgnoreCase("edit") && args.length == 4 && args[2].equalsIgnoreCase("role")) {
            res.addAll(Arrays.asList("staff","all"));
        } else if (args[0].equalsIgnoreCase("edit") && args.length == 4 && args[2].equalsIgnoreCase("silent")) {
            res.addAll(Arrays.asList("true","false"));
        } else if (args[0].equalsIgnoreCase("on") && sender instanceof Player p) {
            res.addAll(Arrays.asList(String.valueOf(p.getLocation().getBlockX()),
                                     String.valueOf(p.getLocation().getBlockY()),
                                     String.valueOf(p.getLocation().getBlockZ()),
                                     p.getWorld().getName()));
        }

        return res.stream().filter(s -> s.toLowerCase().startsWith(args[args.length-1].toLowerCase())).distinct().collect(Collectors.toList());
    }
}
