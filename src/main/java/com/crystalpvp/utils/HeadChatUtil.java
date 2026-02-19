package com.crystalpvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class HeadChatUtil {
    
    private static final String HEAD_PART_1 = "&6▄▄▄▄▄▄▄▄";
    private static final String HEAD_PART_2 = "&6█&e█████&6█";
    private static final String HEAD_PART_3 = "&6█&e█&0█&e█&0█&e█&6█";
    private static final String HEAD_PART_4 = "&6█&e█████&6█";
    private static final String HEAD_PART_5 = "&6█&e██&0█&e██&6█";
    private static final String HEAD_PART_6 = "&6█&e█&0█&e█&0█&e█&6█";
    private static final String HEAD_PART_7 = "&6█&e█&0███&e█&6█";
    private static final String HEAD_PART_8 = "&6▀▀▀▀▀▀▀▀";
    
    public static void sendWinnerHeadMessage(Player winner, List<String> messages) {
        String winnerName = winner.getName();
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("");
            online.sendMessage(color(HEAD_PART_1 + "  " + processMessage(messages.get(0), winnerName)));
            online.sendMessage(color(HEAD_PART_2 + "  " + processMessage(messages.get(1), winnerName)));
            online.sendMessage(color(HEAD_PART_3 + "  " + processMessage(messages.get(2), winnerName)));
            online.sendMessage(color(HEAD_PART_4 + "  " + (messages.size() > 3 ? processMessage(messages.get(3), winnerName) : "")));
            online.sendMessage(color(HEAD_PART_5 + "  " + (messages.size() > 4 ? processMessage(messages.get(4), winnerName) : "")));
            online.sendMessage(color(HEAD_PART_6 + "  " + (messages.size() > 5 ? processMessage(messages.get(5), winnerName) : "")));
            online.sendMessage(color(HEAD_PART_7 + "  " + (messages.size() > 6 ? processMessage(messages.get(6), winnerName) : "")));
            online.sendMessage(color(HEAD_PART_8 + "  " + (messages.size() > 7 ? processMessage(messages.get(7), winnerName) : "")));
            online.sendMessage("");
        }
    }
    
    private static String processMessage(String message, String winnerName) {
        return message.replace("{winner}", winnerName);
    }
    
    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}