package me.yourname.ifexec;

import me.yourname.ifexec.commands.IfCommand;
import me.yourname.ifexec.managers.TriggerManager;
import me.yourname.ifexec.managers.UndoManager;
import me.yourname.ifexec.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class IfExec extends JavaPlugin {

    private static IfExec instance;
    private TriggerManager triggerManager;
    private UndoManager undoManager;
    private MessageUtil messageUtil;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.messageUtil = new MessageUtil(this);
        this.triggerManager = new TriggerManager(this);
        this.undoManager = new UndoManager(this);

        getCommand("if").setExecutor(new IfCommand(this));
        getCommand("if").setTabCompleter(new IfCommand(this));

        getLogger().info("IfExec enabled!");
    }

    @Override
    public void onDisable() {
        triggerManager.saveTriggers();
        getLogger().info("IfExec disabled!");
    }

    public static IfExec getInstance() {
        return instance;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
}
