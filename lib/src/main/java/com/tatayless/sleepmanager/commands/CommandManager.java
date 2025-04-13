package com.tatayless.sleepmanager.commands;

import com.tatayless.sleepmanager.SleepManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final SleepManager plugin;

    public CommandManager(SleepManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "version":
                if (hasPermission(sender, "sleepmanager.version")) {
                    sender.sendMessage("§aSleep Manager version: §f" + plugin.getDescription().getVersion());
                }
                break;

            case "revote":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }

                if (hasPermission(sender, "sleepmanager.revote")) {
                    Player player = (Player) sender;
                    String worldName = player.getWorld().getName();

                    if (!plugin.getConfigManager().isWorldEnabled(worldName)) {
                        plugin.getMessageUtils().sendMessage(player, "command.world_disabled");
                        return true;
                    }

                    if (!plugin.getVoteManager().canStartVote(worldName)) {
                        plugin.getMessageUtils().sendMessage(player, "command.revote_cooldown");
                        return true;
                    }

                    plugin.getVoteManager().startVote(worldName);
                }
                break;

            case "yes":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }

                if (hasPermission(sender, "sleepmanager.vote")) {
                    Player player = (Player) sender;
                    plugin.getVoteManager().vote(player, true);
                }
                break;

            case "no":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }

                if (hasPermission(sender, "sleepmanager.vote")) {
                    Player player = (Player) sender;
                    plugin.getVoteManager().vote(player, false);
                }
                break;

            case "toggle":
                if (hasPermission(sender, "sleepmanager.toggle")) {
                    if (args.length > 1) {
                        // Toggle specific world
                        String worldName = args[1];
                        World world = Bukkit.getWorld(worldName);

                        if (world == null) {
                            sender.sendMessage("§cWorld '" + worldName + "' not found.");
                            return true;
                        }

                        plugin.getConfigManager().toggleWorld(worldName);
                        boolean isEnabled = plugin.getConfigManager().isWorldEnabled(worldName);
                        sender.sendMessage("§aSleep voting for world '" + worldName + "' is now " +
                                (isEnabled ? "§aenabled" : "§cdisabled") + "§a.");

                        // Clear any active votes in this world if we're disabling it
                        if (!isEnabled) {
                            plugin.getVoteManager().clearVote(worldName);
                        }
                    } else {
                        // Toggle all worlds
                        boolean allEnabled = true;

                        // Check if all worlds are already enabled
                        for (World world : Bukkit.getWorlds()) {
                            if (!plugin.getConfigManager().isWorldEnabled(world.getName())) {
                                allEnabled = false;
                                break;
                            }
                        }

                        // Toggle to the opposite state
                        plugin.getConfigManager().toggleAllWorlds(!allEnabled);

                        sender.sendMessage("§aSleep voting for all worlds is now " +
                                (!allEnabled ? "§aenabled" : "§cdisabled") + "§a.");

                        // Clear all active votes if we're disabling all worlds
                        if (allEnabled) {
                            plugin.getVoteManager().clearAllVotes();
                        }
                    }

                    // Save config after toggling
                    plugin.getConfigManager().saveConfig();
                }
                break;

            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList("version", "revote", "yes", "no", "toggle");

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    if (subCommand.equals("version") && hasPermission(sender, "sleepmanager.version")) {
                        completions.add(subCommand);
                    } else if (subCommand.equals("revote") && hasPermission(sender, "sleepmanager.revote")) {
                        completions.add(subCommand);
                    } else if ((subCommand.equals("yes") || subCommand.equals("no"))
                            && hasPermission(sender, "sleepmanager.vote")) {
                        completions.add(subCommand);
                    } else if (subCommand.equals("toggle") && hasPermission(sender, "sleepmanager.toggle")) {
                        completions.add(subCommand);
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")
                && hasPermission(sender, "sleepmanager.toggle")) {
            String partial = args[1].toLowerCase();
            completions = Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return completions;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6===== Sleep Manager Commands =====");

        if (hasPermission(sender, "sleepmanager.version")) {
            sender.sendMessage("§e/sleepmanager version §7- Show plugin version");
        }

        if (hasPermission(sender, "sleepmanager.revote")) {
            sender.sendMessage("§e/sleepmanager revote §7- Start a new sleep vote");
        }

        if (hasPermission(sender, "sleepmanager.vote")) {
            sender.sendMessage("§e/sleepmanager yes §7- Vote yes to skip the night");
            sender.sendMessage("§e/sleepmanager no §7- Vote no to skip the night");
        }

        if (hasPermission(sender, "sleepmanager.toggle")) {
            sender.sendMessage("§e/sleepmanager toggle [world] §7- Toggle sleep voting for a world");
        }
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) ||
                (sender.isOp() && permission.equals("sleepmanager.toggle")) ||
                (!permission.equals("sleepmanager.toggle"));
    }
}
