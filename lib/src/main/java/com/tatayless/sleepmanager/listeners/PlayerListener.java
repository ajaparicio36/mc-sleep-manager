package com.tatayless.sleepmanager.listeners;

import com.tatayless.sleepmanager.SleepManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerListener implements Listener {
    private final SleepManager plugin;

    public PlayerListener(SleepManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();

        // Check if the world is eligible (not nether or end)
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        // Check if voting is enabled for this world
        if (!plugin.getConfigManager().isWorldEnabled(worldName)) {
            return;
        }

        // Check if it's actually night time
        long time = world.getTime();
        if (time < 12541 || time > 23458) {
            return; // Not night time
        }

        // If no active vote and sleep not already enabled, start a vote
        if (!plugin.getVoteManager().hasActiveVote(worldName) && !plugin.getVoteManager().isSleepEnabled(worldName)) {
            // Check if there are multiple players in the world
            List<Player> eligiblePlayers = plugin.getVoteManager().getEligiblePlayers(worldName);

            if (eligiblePlayers.size() > 1) {
                // Start a vote since there are multiple players
                plugin.getVoteManager().startVote(worldName);

                // We want the player to be able to get in bed, but we don't want to skip night
                // yet
                return;
            } else {
                // Only one player, automatically enable sleep
                plugin.getVoteManager().handlePlayerSleep(player);
            }
        } else if (plugin.getVoteManager().isSleepEnabled(worldName)) {
            // Sleep is already enabled, let the player sleep and skip the night
            plugin.getVoteManager().handlePlayerSleep(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if there's an active vote in this world
        String worldName = player.getWorld().getName();
        if (plugin.getVoteManager().hasActiveVote(worldName)) {
            // Send the vote message to the player
            plugin.getMessageUtils().sendVoteMessage(player, worldName);
        }
    }
}
