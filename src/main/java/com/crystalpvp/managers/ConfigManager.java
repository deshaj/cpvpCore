package com.crystalpvp.managers;

import com.crystalpvp.CrystalPvP;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class ConfigManager {

    private final CrystalPvP plugin;
    private FileConfiguration config;

    public ConfigManager(CrystalPvP plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getMessage(String path) {
        String prefix = config.getString("prefix", "&8[&bCrystalPvP&8] &7");
        if (config.isList("messages." + path)) {
            List<String> lines = config.getStringList("messages." + path);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(prefix).append(lines.get(i));
            }
            return ChatColor.translateAlternateColorCodes('&', sb.toString());
        }
        String message = config.getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public String getMessageRaw(String path) {
        if (config.isList("messages." + path)) {
            List<String> lines = config.getStringList("messages." + path);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(lines.get(i));
            }
            return ChatColor.translateAlternateColorCodes('&', sb.toString());
        }
        return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + path, ""));
    }

    public void send(CommandSender sender, String path) {
        String msg = getMessage(path);
        for (String line : msg.split("\n")) {
            sender.sendMessage(line);
        }
    }

    public void send(CommandSender sender, String path, String placeholder, String value) {
        String msg = getMessage(path).replace(placeholder, value);
        for (String line : msg.split("\n")) {
            sender.sendMessage(line);
        }
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath) {
        String title = getMessageRaw(titlePath);
        String subtitle = getMessageRaw(subtitlePath);
        player.sendTitle(title, subtitle, 10, 60, 20);
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath, String placeholder, String value) {
        String title = getMessageRaw(titlePath).replace(placeholder, value);
        String subtitle = getMessageRaw(subtitlePath).replace(placeholder, value);
        player.sendTitle(title, subtitle, 10, 60, 20);
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath, int fadeIn, int stay, int fadeOut) {
        String title = getMessageRaw(titlePath);
        String subtitle = getMessageRaw(subtitlePath);
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public String getString(String path) {
        return config.getString(path, "");
    }

    public int getInt(String path) {
        return config.getInt(path, 0);
    }
}