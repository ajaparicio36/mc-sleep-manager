package com.tatayless.sleepmanager;

import com.tatayless.sleepmanager.commands.CommandManager;
import com.tatayless.sleepmanager.config.ConfigManager;
import com.tatayless.sleepmanager.listeners.PlayerListener;
import com.tatayless.sleepmanager.managers.VoteManager;
import com.tatayless.sleepmanager.utils.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class SleepManager extends JavaPlugin {

    private ConfigManager configManager;
    private VoteManager voteManager;
    private MessageUtils messageUtils;

    @Override
    public void onEnable() {
        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize message utils for localization
        messageUtils = new MessageUtils(this);

        // Initialize vote manager
        voteManager = new VoteManager(this);

        // Register commands
        CommandManager commandManager = new CommandManager(this);
        getCommand("sleepmanager").setExecutor(commandManager);
        getCommand("sleepmanager").setTabCompleter(commandManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("SleepManager has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save config on disable
        configManager.saveConfig();

        // Clear any ongoing votes
        if (voteManager != null) {
            voteManager.clearAllVotes();
        }

        getLogger().info("SleepManager has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }
}