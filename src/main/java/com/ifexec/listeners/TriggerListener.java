package com.ifexec.listeners;

import com.ifexec.IfExec;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.model.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class TriggerListener implements Listener {
    private final IfExec plugin;
    private final TriggerManager triggerManager;
    private final Messages messages;

    public TriggerListener(IfExec plugin) {
        this.plugin = plugin;
        this.triggerManager = plugin.getTriggerManager();
        this.messages = plugin.getMessages();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        // only when block changed
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Player p = e.getPlayer();

        List<Trigger> toCheck = triggerManager.getAll().stream().toList();
        for (Trigger t : toCheck) {
            if (!t.isEnabled()) continue;

            boolean wasIn = (t.getType() == Trigger.Type.BLOCK) ? t.isInBlock(from) : t.isInRegion(from);
            boolean nowIn  = (t.getType() == Trigger.Type.BLOCK) ? t.isInBlock(to)   : t.isInRegion(to);

            // trigger only on entry (wasOut -> nowIn)
            if (!nowIn || wasIn) continue;

            // role check
            if ("staff".equalsIgnoreCase(t.getRole()) && !p.hasPermission("ifexec.staff")) {
                if (!t.isSilent()) p.sendMessage(messages.getWithPrefix("staff_only"));
                continue;
            }

            // cooldown check (per-trigger)
            int defaultCd = plugin.getConfigManager().getConfig().getInt("default_cooldown", 3);
            if (!t.canTrigger(p.getUniqueId(), defaultCd)) {
                if (!t.isSilent()) p.sendMessage(messages.getWithPrefix("cooldown_active"));
                continue;
            }

            // execute commands
            for (String raw : t.getCommands()) {
                if (raw == null || raw.isBlank()) continue;
                String cmd = raw.replace("@p", p.getName()).replace("@s", p.getName());
                if (cmd.contains("@a")) {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        String forCmd = cmd.replace("@a", pl.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), forCmd);
                    }
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                if (!t.isSilent()) {
                    String sendMsg = null;
                    if (p.hasPermission("ifexec.staff") && t.getMessages().containsKey("staff")) sendMsg = t.getMessages().get("staff");
                    if (sendMsg == null && t.getMessages().containsKey("all")) sendMsg = t.getMessages().get("all");
                    if (sendMsg == null) sendMsg = messages.get("trigger_executed");
                    sendMsg = sendMsg.replace("{name}", t.getName()).replace("{player}", p.getName());
                    p.sendMessage(messages.getWithPrefix("") + " " + org.bukkit.ChatColor.translateAlternateColorCodes('&', sendMsg));
                }
            }

            t.setTriggered(p.getUniqueId());
        }
    }
}
