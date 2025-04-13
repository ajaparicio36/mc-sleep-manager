package com.tatayless.sleepmanager;

import com.tatayless.sleepmanager.managers.VoteManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SleepManagerTest {

    private SleepManager sleepManager;

    @Mock
    private PluginManager pluginManager;
    @Mock
    private PluginCommand sleepCommand;
    @Mock
    private VoteManager voteManager;

    @BeforeEach
    void setUp() {
        sleepManager = spy(new SleepManager());

        // Mock the necessary methods
        doReturn(pluginManager).when(sleepManager).getServer();
        doReturn(pluginManager).when(sleepManager).getServer().getPluginManager();
        when(sleepManager.getCommand("sleepmanager")).thenReturn(sleepCommand);
    }

    @Test
    void onEnable_registersListenersAndCommands() {
        doNothing().when(sleepManager).saveDefaultConfig();

        sleepManager.onEnable();

        verify(pluginManager).registerEvents(any(), eq(sleepManager));
        verify(sleepCommand).setExecutor(any());
        verify(sleepCommand).setTabCompleter(any());
    }

    @Test
    void onDisable_cleansUpResources() {
        // Set up the vote manager mock
        doReturn(voteManager).when(sleepManager).getVoteManager();

        sleepManager.onDisable();

        // Verify that clearAllVotes is called
        verify(voteManager).clearAllVotes();
    }

    @Test
    void gettersReturnInitializedManagers() {
        doNothing().when(sleepManager).saveDefaultConfig();

        sleepManager.onEnable();

        // Just verify that managers are not null
        assertNotNull(sleepManager.getVoteManager());
        assertNotNull(sleepManager.getConfigManager());
        assertNotNull(sleepManager.getMessageUtils());
    }
}
