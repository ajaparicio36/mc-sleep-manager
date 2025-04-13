package com.tatayless.sleepmanager.managers;

import com.tatayless.sleepmanager.SleepManager;
import com.tatayless.sleepmanager.config.ConfigManager;
import com.tatayless.sleepmanager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoteManagerTest {

    private VoteManager voteManager;

    @Mock
    private SleepManager plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private MessageUtils messageUtils;
    @Mock
    private Server server;
    @Mock
    private World world;
    @Mock
    private Player player1;
    @Mock
    private Player player2;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private BukkitTask task;
    @Mock
    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getMessageUtils()).thenReturn(messageUtils);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenReturn(task);

        voteManager = new VoteManager(plugin);
    }

    @Test
    void hasActiveVote_noVote_returnsFalse() {
        assertFalse(voteManager.hasActiveVote("world"));
    }

    @Test
    void isSleepEnabled_defaultFalse() {
        assertFalse(voteManager.isSleepEnabled("world"));
    }

    @Test
    void getEligiblePlayers_returnsCorrectPlayers() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

            List<Player> worldPlayers = new ArrayList<>();
            worldPlayers.add(player1);
            worldPlayers.add(player2);

            when(world.getPlayers()).thenReturn(worldPlayers);
            when(player1.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
            when(player2.getGameMode()).thenReturn(org.bukkit.GameMode.CREATIVE); // Not eligible

            List<Player> eligiblePlayers = voteManager.getEligiblePlayers("world");

            assertEquals(1, eligiblePlayers.size());
            assertTrue(eligiblePlayers.contains(player1));
            assertFalse(eligiblePlayers.contains(player2));
        }
    }

    @Test
    void startVote_startsCorrectly() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

            List<Player> worldPlayers = new ArrayList<>();
            worldPlayers.add(player1);
            when(world.getPlayers()).thenReturn(worldPlayers);
            when(player1.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
            when(configManager.getVoteTimeLimit()).thenReturn(30);

            boolean result = voteManager.startVote("world");

            assertTrue(result);
            assertTrue(voteManager.hasActiveVote("world"));
            verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(30 * 20L));
            verify(messageUtils).sendVoteMessage(player1, "world");
        }
    }

    @Test
    void handlePlayerSleep_enablesSleep() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);

            when(player1.getWorld()).thenReturn(world);
            when(world.getName()).thenReturn("world");

            // First mark sleep as enabled
            voteManager.startVote("world"); // Setup active vote
            voteManager.vote(player1, true); // Make a vote to change state

            // Now simulate sleep
            voteManager.handlePlayerSleep(player1);

            // Test should verify the setTime call happens at right time
            verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(20L));
        }
    }

    @Test
    void clearAllVotes_clearsActiveVotes() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

            // Setup eligible players
            List<Player> worldPlayers = new ArrayList<>();
            worldPlayers.add(player1);
            when(world.getPlayers()).thenReturn(worldPlayers);
            when(player1.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);

            // Start a vote
            voteManager.startVote("world");
            assertTrue(voteManager.hasActiveVote("world"));

            // Clear votes
            voteManager.clearAllVotes();

            // Verify vote was cleared
            assertFalse(voteManager.hasActiveVote("world"));
        }
    }
}
