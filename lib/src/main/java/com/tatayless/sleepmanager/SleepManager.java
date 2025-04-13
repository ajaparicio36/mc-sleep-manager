package com.tatayless.sleepmanager;

import org.bukkit.plugin.java.JavaPlugin;

public class SleepManager extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("SleepManager has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("SleepManager has been disabled!");
    }
    
    public boolean someLibraryMethod() {
        return true;
    }
}