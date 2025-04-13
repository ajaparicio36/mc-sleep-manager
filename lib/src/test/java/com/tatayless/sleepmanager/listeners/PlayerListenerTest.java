package com.tatayless.sleepmanager.listeners;

import com.tatayless.sleepmanager.SleepManager;
import com.tatayless.sleepmanager.config.ConfigManager;
import com.tatayless.sleepmanager.managers.VoteManager;
import com.tatayless.sleepmanager.utils.MessageUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayerListenerTest {

    private PlayerListener playerListener;

    @Mock
    private SleepManager plugin;
    @Mock
    private VoteManager voteManager;
    @Mock
    private ConfigManager configManager;
    @Mock
    private MessageUtils messageUtils;
    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private PlayerBedEnterEvent bedEnterEvent;
    @Mock
    private PlayerJoinEvent joinEvent;

    @BeforeEach
    void setUp() {
        when(plugin.getVoteManager()).thenReturn(voteManager);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getMessageUtils()).thenReturn(messageUtils);
        playerListener = new PlayerListener(plugin);
    }

    @Test
    void onPlayerBedEnter_cancelledEvent_doNothing() {
        when(bedEnterEvent.isCancelled()).thenReturn(true);

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager, never()).hasActiveVote(anyString());
        verify(voteManager, never()).startVote(anyString());
    }

    @Test
    void onPlayerBedEnter_nonOverworldEnvironment_doNothing() {
        when(bedEnterEvent.isCancelled()).thenReturn(false);
        when(bedEnterEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(World.Environment.NETHER);

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager, never()).hasActiveVote(anyString());
        verify(voteManager, never()).startVote(anyString());
    }

    @Test
    void onPlayerBedEnter_worldDisabled_doNothing() {
        when(bedEnterEvent.isCancelled()).thenReturn(false);
        when(bedEnterEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getName()).thenReturn("world");
        when(configManager.isWorldEnabled("world")).thenReturn(false);

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager, never()).hasActiveVote(anyString());
        verify(voteManager, never()).startVote(anyString());
    }

    @Test
    void onPlayerBedEnter_notNightTime_doNothing() {
        when(bedEnterEvent.isCancelled()).thenReturn(false);
        when(bedEnterEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getName()).thenReturn("world");
        when(configManager.isWorldEnabled("world")).thenReturn(true);
        when(world.getTime()).thenReturn(6000L); // Day time

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager, never()).hasActiveVote(anyString());
        verify(voteManager, never()).startVote(anyString());
    }

    @Test
    void onPlayerBedEnter_startVoteMultiplePlayers() {
        setupValidBedEnterEvent();
        when(voteManager.hasActiveVote("world")).thenReturn(false);
        when(voteManager.isSleepEnabled("world")).thenReturn(false);

        List<Player> players = new ArrayList<>();
        players.add(player);
        players.add(mock(Player.class));
        when(voteManager.getEligiblePlayers("world")).thenReturn(players);

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager).startVote("world");
        verify(voteManager, never()).handlePlayerSleep(any(Player.class));
    }

    @Test
    void onPlayerBedEnter_singlePlayerAutoSleep() {
        setupValidBedEnterEvent();
        when(voteManager.hasActiveVote("world")).thenReturn(false);
        when(voteManager.isSleepEnabled("world")).thenReturn(false);

        List<Player> players = new ArrayList<>();
        players.add(player);
        when(voteManager.getEligiblePlayers("world")).thenReturn(players);

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager).handlePlayerSleep(player);
    }

    @Test
    void onPlayerBedEnter_sleepAlreadyEnabled() {
        setupValidBedEnterEvent();
        when(voteManager.isSleepEnabled("world")).thenReturn(true);

        playerListener.onPlayerBedEnter(bedEnterEvent);

        verify(voteManager).handlePlayerSleep(player);
    }

    @Test
    void onPlayerJoin_activeVote_sendMessage() {
        when(joinEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(voteManager.hasActiveVote("world")).thenReturn(true);

        playerListener.onPlayerJoin(joinEvent);

        verify(messageUtils).sendVoteMessage(player, "world");
    }

    @Test
    void onPlayerJoin_noActiveVote_noMessage() {
        when(joinEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(voteManager.hasActiveVote("world")).thenReturn(false);

        playerListener.onPlayerJoin(joinEvent);

        verify(messageUtils, never()).sendVoteMessage(any(Player.class), anyString());
    }

    private void setupValidBedEnterEvent() {
        when(bedEnterEvent.isCancelled()).thenReturn(false);
        when(bedEnterEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getName()).thenReturn("world");
        when(configManager.isWorldEnabled("world")).thenReturn(true);
        when(world.getTime()).thenReturn(13000L); // Night time
    }
}
