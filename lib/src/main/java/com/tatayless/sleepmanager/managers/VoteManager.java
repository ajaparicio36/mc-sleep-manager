package com.tatayless.sleepmanager.managers;

import com.tatayless.sleepmanager.SleepManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoteManager {
    private final SleepManager plugin;

    // Store ongoing votes by world
    private final Map<String, VoteSession> activeVotes;

    // Store revote cooldowns by world
    private final Map<String, Long> revoteCooldowns;

    // Store the sleep enabled status for each world after voting
    private final Map<String, Boolean> sleepEnabledMap;

    // Store the last vote percentage for each world
    private final Map<String, Double> lastVotePercentages = new ConcurrentHashMap<>();

    public VoteManager(SleepManager plugin) {
        this.plugin = plugin;
        this.activeVotes = new ConcurrentHashMap<>();
        this.revoteCooldowns = new ConcurrentHashMap<>();
        this.sleepEnabledMap = new ConcurrentHashMap<>();
    }

    public boolean hasActiveVote(String worldName) {
        return activeVotes.containsKey(worldName);
    }

    public boolean isSleepEnabled(String worldName) {
        return sleepEnabledMap.getOrDefault(worldName, false);
    }

    public boolean canStartVote(String worldName) {
        if (hasActiveVote(worldName)) {
            return false;
        }

        Long lastVoteTime = revoteCooldowns.get(worldName);
        if (lastVoteTime == null) {
            return true;
        }

        long cooldownMillis = plugin.getConfigManager().getRevoteCooldown() * 1000L;
        return (System.currentTimeMillis() - lastVoteTime) > cooldownMillis;
    }

    public boolean startVote(String worldName) {
        if (!canStartVote(worldName)) {
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        // Get all eligible players at the start of voting
        List<Player> eligiblePlayers = getEligiblePlayers(worldName);
        if (eligiblePlayers.isEmpty()) {
            return false;
        }

        VoteSession session = new VoteSession(worldName, eligiblePlayers);
        activeVotes.put(worldName, session);

        // Schedule vote end
        int voteDuration = plugin.getConfigManager().getVoteTimeLimit();
        session.setTask(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endVote(worldName);
        }, voteDuration * 20L));

        // Send vote message to all players in the world
        for (Player player : world.getPlayers()) {
            plugin.getMessageUtils().sendVoteMessage(player, worldName);
        }

        return true;
    }

    public boolean vote(Player player, boolean voteYes) {
        String worldName = player.getWorld().getName();

        if (!hasActiveVote(worldName)) {
            plugin.getMessageUtils().sendMessage(player, "vote.no_active_vote");
            return false;
        }

        VoteSession session = activeVotes.get(worldName);
        session.vote(player.getUniqueId(), voteYes);

        plugin.getMessageUtils().sendMessage(player, voteYes ? "vote.voted_yes" : "vote.voted_no");

        // Check if all players have voted and end vote early if they have
        if (session.haveAllPlayersVoted()) {
            // Cancel the scheduled task
            if (session.getTask() != null && !session.getTask().isCancelled()) {
                session.getTask().cancel();
            }

            // End the vote immediately
            Bukkit.getScheduler().runTask(plugin, () -> endVote(worldName));
        }

        return true;
    }

    public void endVote(String worldName) {
        if (!hasActiveVote(worldName)) {
            return;
        }

        VoteSession session = activeVotes.remove(worldName);
        revoteCooldowns.put(worldName, System.currentTimeMillis());

        // Calculate results
        int yesVotes = session.countYesVotes();
        int totalVotes = session.getTotalVotes();

        // Calculate the percentage of yes votes
        double yesPercentage = totalVotes > 0 ? (yesVotes * 100.0 / totalVotes) : 0;
        lastVotePercentages.put(worldName, yesPercentage);

        // Get the configured threshold percentage
        int thresholdPercentage = plugin.getConfigManager().getVotePercentageThreshold();

        // If threshold is 0, always pass the vote
        boolean passed;
        if (thresholdPercentage == 0) {
            passed = totalVotes > 0; // Only require that someone voted
        } else {
            passed = totalVotes > 0 && yesPercentage >= thresholdPercentage;
        }

        sleepEnabledMap.put(worldName, passed);

        // Announce results to all players in the world
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            for (Player player : world.getPlayers()) {
                plugin.getMessageUtils().sendVoteResult(player, passed, worldName);
            }
        }

        // Cancel the scheduled task if it's still running
        if (session.getTask() != null && !session.getTask().isCancelled()) {
            session.getTask().cancel();
        }
    }

    public void clearVote(String worldName) {
        if (hasActiveVote(worldName)) {
            VoteSession session = activeVotes.remove(worldName);
            if (session.getTask() != null && !session.getTask().isCancelled()) {
                session.getTask().cancel();
            }
        }
    }

    /**
     * Gets the percentage of 'yes' votes from the last vote in the specified world
     *
     * @param worldName The name of the world
     * @return The percentage (0-100) of 'yes' votes, or 0 if no votes occurred
     */
    public double getLastVoteYesPercentage(String worldName) {
        return lastVotePercentages.getOrDefault(worldName, 0.0);
    }

    public void clearAllVotes() {
        for (String worldName : new ArrayList<>(activeVotes.keySet())) {
            clearVote(worldName);
        }
        activeVotes.clear();
        sleepEnabledMap.clear();
    }

    public List<Player> getEligiblePlayers(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Collections.emptyList();
        }

        List<Player> eligiblePlayers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            // Only include players in survival or adventure mode
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                eligiblePlayers.add(player);
            }
        }

        return eligiblePlayers;
    }

    public void handlePlayerSleep(Player player) {
        String worldName = player.getWorld().getName();

        // Check if sleep is enabled for this world after a vote
        if (isSleepEnabled(worldName)) {
            // Reset the sleep enabled flag for this world
            sleepEnabledMap.put(worldName, false);

            // Set the time to day
            World world = player.getWorld();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.setTime(0); // Set to morning
                world.setStorm(false); // Clear weather
                world.setThundering(false);

                // Notify players
                for (Player p : world.getPlayers()) {
                    plugin.getMessageUtils().sendMessage(p, "sleep.night_skipped");
                }
            }, 20L); // Slight delay to let the player actually get in bed
        }
    }

    // Internal class to track voting information
    private class VoteSession {
        @SuppressWarnings("unused")
        private final String worldName;
        private final Map<UUID, Boolean> votes;
        private BukkitTask task;
        private final Set<UUID> eligiblePlayers;

        public VoteSession(String worldName, List<Player> players) {
            this.worldName = worldName;
            this.votes = new HashMap<>();
            this.eligiblePlayers = new HashSet<>();

            // Store UUIDs of all eligible players
            for (Player player : players) {
                eligiblePlayers.add(player.getUniqueId());
            }
        }

        public void vote(UUID playerUuid, boolean voteYes) {
            votes.put(playerUuid, voteYes);
        }

        public boolean haveAllPlayersVoted() {
            // Check if all eligible players have cast their vote
            for (UUID playerId : eligiblePlayers) {
                if (!votes.containsKey(playerId)) {
                    return false;
                }
            }
            return !eligiblePlayers.isEmpty();
        }

        public int countYesVotes() {
            int count = 0;
            for (Boolean vote : votes.values()) {
                if (vote) {
                    count++;
                }
            }
            return count;
        }

        public int getTotalVotes() {
            return votes.size();
        }

        public void setTask(BukkitTask task) {
            this.task = task;
        }

        public BukkitTask getTask() {
            return task;
        }
    }
}
