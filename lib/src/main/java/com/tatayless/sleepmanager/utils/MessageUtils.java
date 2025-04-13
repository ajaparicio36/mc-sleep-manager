package com.tatayless.sleepmanager.utils;

import com.tatayless.sleepmanager.SleepManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageUtils {
    private final SleepManager plugin;
    private YamlConfiguration langConfig;

    public MessageUtils(SleepManager plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        String lang = plugin.getConfigManager().getLanguage();

        // Check if the language file exists in the plugin data folder
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        } else {
            // If not, use the default one from resources
            InputStream defaultLangStream = plugin.getResource("lang/" + lang + ".yml");

            // If the specified language doesn't exist, default to English
            if (defaultLangStream == null) {
                defaultLangStream = plugin.getResource("lang/en.yml");
                plugin.getLogger().warning("Language " + lang + " not found, defaulting to English");
            }

            if (defaultLangStream != null) {
                langConfig = YamlConfiguration
                        .loadConfiguration(new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8));

                // Save the default language file to the plugin data folder
                try {
                    plugin.saveResource("lang/"
                            + (defaultLangStream == plugin.getResource("lang/en.yml") ? "en.yml" : lang + ".yml"),
                            false);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save default language file");
                }
            } else {
                // Create an empty config if no language file is found
                langConfig = new YamlConfiguration();
                plugin.getLogger().severe("No language files found!");
            }
        }
    }

    public String getMessage(String key) {
        String message = langConfig.getString(key);
        if (message == null) {
            return "Missing translation for: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(key));
    }

    @SuppressWarnings("deprecation")
    public void sendVoteMessage(Player player, String worldName) {
        TextComponent message = new TextComponent(getMessage("vote.prompt").replace("{world}", worldName));

        TextComponent yesButton = new TextComponent(getMessage("vote.yes_button"));
        yesButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sleepmanager yes"));
        yesButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(getMessage("vote.yes_hover")).create()));

        TextComponent separator = new TextComponent(" | ");

        TextComponent noButton = new TextComponent(getMessage("vote.no_button"));
        noButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sleepmanager no"));
        noButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(getMessage("vote.no_hover")).create()));

        player.spigot().sendMessage(message, yesButton, separator, noButton);
    }

    public void sendVoteResult(Player player, boolean passed, String worldName) {
        String key = passed ? "vote.passed" : "vote.failed";
        player.sendMessage(getMessage(key)
                .replace("{world}", worldName)
                .replace("{yes_percent}",
                        String.format("%.0f", plugin.getVoteManager().getLastVoteYesPercentage(worldName))));
    }
}
