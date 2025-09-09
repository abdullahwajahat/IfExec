package me.yourname.ifexec.utils;

import me.yourname.ifexec.IfExec;
import org.bukkit.ChatColor;

public class MessageUtil {
    private final IfExec plugin;
    private final String prefix;

    public MessageUtil(IfExec plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("plugin_prefix", "");
    }

    public String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    public String get(String key) {
        String msg = plugin.getConfig().getString(key, key);
        return format(msg);
    }
}
