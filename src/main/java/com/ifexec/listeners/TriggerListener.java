package com.ifexec.listeners;

import com.ifexec.IfExec;
import com.ifexec.manager.Messages;
import com.ifexec.manager.TriggerManager;
import com.ifexec.model.Trigger;
import org.bukkit.Bukkit;
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
        if (e.getFrom() == null || e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockY() == e.getTo().getBlockY() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return; // only when block position changed

        Player p = e.getPlayer();
        List<Trigger> found = triggerManager.findByLocation(p.getLocation());
        if (found.isEmpty()) return;

        int defaultCd = plugin.getConfigManager().getConfig().getInt("default_cooldown", 3);

        for (Trigger t : found) {
            // role check
            if ("staff".equalsIgnoreCase(t.getRole()) && !p.hasPermission("ifexec.staff")) {
                if (!t.isSilent()) p.sendMessage(messages.get("staff_only"));
                continue;
            }

            // cooldown
            if (!t.canTrigger(p.getUniqueId(), defaultCd)) {
                if (!t.isSilent()) p.sendMessage(messages.get("cooldown_active"));
                continue;
            }

            // execute commands (as console), replacing selectors/placeholders
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
            }

            t.setTriggered(p.getUniqueId());

            if (!t.isSilent()) {
                String sendMsg = null;
                if (p.hasPermission("ifexec.staff") && t.getMessages().containsKey("staff")) sendMsg = t.getMessages().get("staff");
                if (sendMsg == null && t.getMessages().containsKey("all")) sendMsg = t.getMessages().get("all");
                if (sendMsg == null) sendMsg = messages.get("trigger_created");
                p.sendMessage(MessagesUtil.color(messages.get("plugin_prefix") + sendMsg.replace("{name}", t.getName())));
            }
        }
    }
}
