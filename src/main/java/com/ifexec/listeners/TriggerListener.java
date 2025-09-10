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

import java.util.HashMap;
import java.util.Map;

public class TriggerListener implements Listener {
    private final IfExec plugin;
    private final TriggerManager triggerManager;
    private final Messages messages;
    private final Map<String,Long> cooldowns = new HashMap<>();

    public TriggerListener(IfExec plugin) {
        this.plugin = plugin;
        this.triggerManager = plugin.getTriggerManager();
        this.messages = plugin.getMessages();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        for (Trigger t : triggerManager.getAll()) {
            if (!t.isEnabled()) continue;

            if (t.matches(p.getLocation())) {
                long now = System.currentTimeMillis();
                int cd = plugin.getConfig().getInt("default-cooldown",3)*1000;
                if (cooldowns.containsKey(p.getName()) && now - cooldowns.get(p.getName()) < cd) continue;
                cooldowns.put(p.getName(), now);

                for (String cmd : t.getCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("@s", p.getName()));
                    Map<String,String> ph = new HashMap<>();
                    ph.put("command", cmd);
                    p.sendMessage(messages.get("plugin_prefix") + " " + messages.get("command_run").replace("{command}", cmd));
                }
            }
        }
    }
}
