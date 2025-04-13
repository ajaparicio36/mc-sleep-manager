package com.tatayless.sleepmanager.utils;

import com.tatayless.sleepmanager.SleepManager;
import com.tatayless.sleepmanager.config.ConfigManager;
import com.tatayless.sleepmanager.managers.VoteManager;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageUtilsTest {

    private MessageUtils messageUtils;

    @Mock
    private SleepManager plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private VoteManager voteManager;
    @Mock
    private YamlConfiguration langConfig;
    @Mock
    private File dataFolder;
    @Mock
    private World world;
    @Mock
    private Player player;

    @BeforeEach
    void setUp() {
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getVoteManager()).thenReturn(voteManager);
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        // Create a spy on MessageUtils to avoid actual file operations
        messageUtils = spy(new MessageUtils(plugin));

        // Mock loadLanguage
        doNothing().when(messageUtils).loadLanguage();
    }

    @Test
    void getMessage_returnsFormattedMessage() {
        // Setup the test using reflection to set langConfig
        try {
            java.lang.reflect.Field langConfigField = MessageUtils.class.getDeclaredField("langConfig");
            langConfigField.setAccessible(true);
            langConfigField.set(messageUtils, langConfig);

            when(langConfig.getString("test.key")).thenReturn("&aTest message");

            String result = messageUtils.getMessage("test.key");
            assertEquals("§aTest message", result);
        } catch (Exception e) {
            // Handle the exception or fail the test
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendMessage_sendsFormattedMessage() {
        // Setup MessageUtils to return a known message
        doReturn("§aTest message").when(messageUtils).getMessage(anyString());

        messageUtils.sendMessage(player, "test.key");

        verify(player).sendMessage("§aTest message");
    }

    @Test
    void sendVoteMessage_sendsInteractiveMessage() {
        // Setup MessageUtils to return known messages
        doReturn("Vote for night skip").when(messageUtils).getMessage("vote.prompt");
        doReturn("Yes").when(messageUtils).getMessage("vote.yes_button");
        doReturn("Hover for yes").when(messageUtils).getMessage("vote.yes_hover");
        doReturn("No").when(messageUtils).getMessage("vote.no_button");
        doReturn("Hover for no").when(messageUtils).getMessage("vote.no_hover");

        // Mock player.spigot() for interactive components
        @SuppressWarnings("unused")
        net.md_5.bungee.api.chat.BaseComponent[] components = new net.md_5.bungee.api.chat.BaseComponent[0];
        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);

        // Test the method
        messageUtils.sendVoteMessage(player, "testworld");

        // Verify spigot().sendMessage was called (can't verify exact components easily)
        verify(player.spigot()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent[].class));
    }

    @Test
    void sendVoteResult_sendsPassed() {
        // Setup the test data
        when(voteManager.getLastVoteYesPercentage("testworld")).thenReturn(75.0);
        doReturn("Vote passed with 75% approval").when(messageUtils).getMessage("vote.passed");

        messageUtils.sendVoteResult(player, true, "testworld");

        verify(player).sendMessage(anyString());
    }

    @Test
    void sendVoteResult_sendsFailed() {
        // Setup the test data
        when(voteManager.getLastVoteYesPercentage("testworld")).thenReturn(25.0);
        doReturn("Vote failed with only 25% approval").when(messageUtils).getMessage("vote.failed");

        messageUtils.sendVoteResult(player, false, "testworld");

        verify(player).sendMessage(anyString());
    }
}
