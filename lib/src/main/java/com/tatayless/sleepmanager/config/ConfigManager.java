package com.tatayless.sleepmanager.config;

import com.tatayless.sleepmanager.SleepManager;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final SleepManager plugin;
    private FileConfiguration config;
    private File configFile;

    private int revoteCooldown;
    private int voteTimeLimit;
    private String language;
    private int votePercentageThreshold;
    private Map<String, Boolean> worldToggles;

    public ConfigManager(SleepManager plugin) {
        this.plugin = plugin;
        this.worldToggles = new HashMap<>();
    }

    public void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load settings
        revoteCooldown = config.getInt("revote-cooldown", 60); // Default 60 seconds
        voteTimeLimit = config.getInt("vote-time-limit", 30); // Default 30 seconds
        language = config.getString("language", "en");
        votePercentageThreshold = config.getInt("vote-percentage-threshold", 50); // Default 50%

        // Validate percentage is within 0-100 range
        if (votePercentageThreshold < 0) {
            votePercentageThreshold = 0;
            plugin.getLogger().warning("vote-percentage-threshold was set below 0, defaulting to 0");
        } else if (votePercentageThreshold > 100) {
            votePercentageThreshold = 100;
            plugin.getLogger().warning("vote-percentage-threshold was set above 100, defaulting to 100");
        }

        // Load world toggles
        if (config.contains("worlds")) {
            for (String worldName : config.getConfigurationSection("worlds").getKeys(false)) {
                worldToggles.put(worldName, config.getBoolean("worlds." + worldName));
            }
        }

        // Initialize any missing worlds
        for (World world : plugin.getServer().getWorlds()) {
            if (!worldToggles.containsKey(world.getName())) {
                worldToggles.put(world.getName(), true); // Enabled by default
            }
        }
    }

    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        // Save settings
        config.set("revote-cooldown", revoteCooldown);
        config.set("vote-time-limit", voteTimeLimit);
        config.set("language", language);
        config.set("vote-percentage-threshold", votePercentageThreshold);

        // Save world toggles
        for (Map.Entry<String, Boolean> entry : worldToggles.entrySet()) {
            config.set("worlds." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config to " + configFile);
            e.printStackTrace();
        }
    }

    public int getRevoteCooldown() {
        return revoteCooldown;
    }

    public int getVoteTimeLimit() {
        return voteTimeLimit;
    }

    public String getLanguage() {
        return language;
    }

    public int getVotePercentageThreshold() {
        return votePercentageThreshold;
    }

    public boolean isWorldEnabled(String worldName) {
        return worldToggles.getOrDefault(worldName, true);
    }

    public void toggleWorld(String worldName) {
        boolean currentValue = isWorldEnabled(worldName);
        worldToggles.put(worldName, !currentValue);
    }

    public void toggleAllWorlds(boolean enabled) {
        for (String worldName : worldToggles.keySet()) {
            worldToggles.put(worldName, enabled);
        }
    }
}
