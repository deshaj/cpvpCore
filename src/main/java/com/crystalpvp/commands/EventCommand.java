package com.crystalpvp.commands;

import com.cryptomorin.xseries.XMaterial;
import com.crystalpvp.CrystalPvP;
import com.crystalpvp.enums.EventState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EventCommand implements CommandExecutor, TabCompleter {
    
    private final CrystalPvP plugin;
    
    public EventCommand(CrystalPvP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crystalpvp.admin")) {
            plugin.getConfigManager().send(sender, "no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "setup":
                handleSetup(sender, args);
                break;
            case "open":
                handleOpen(sender);
                break;
            case "start":
                handleStart(sender);
                break;
            case "shrink":
                handleShrink(sender, args);
                break;
            case "expand":
                handleExpand(sender, args);
                break;
            case "drop":
                handleDrop(sender);
                break;
            case "cancel":
                handleCancel(sender);
                break;
            case "end":
                handleEnd(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "help":
                sendHelp(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("crystalpvp.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return filterStartingWith(Arrays.asList("setup", "open", "start", "shrink", "expand", "drop", "cancel", "end", "reload", "help"), args[0]);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            return filterStartingWith(Arrays.asList("kit", "wand", "center", "spawn", "lobby", "border"), args[1]);
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("border")) {
            return Arrays.asList("<radius>");
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("shrink") || args[0].equalsIgnoreCase("expand"))) {
            return Arrays.asList("<amount>");
        }
        
        if (args.length == 3 && (args[0].equalsIgnoreCase("shrink") || args[0].equalsIgnoreCase("expand"))) {
            return Arrays.asList("<time>");
        }
        
        return new ArrayList<>();
    }
    
    private List<String> filterStartingWith(List<String> list, String prefix) {
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    private void handleSetup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use setup commands!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            sendSetupHelp(player);
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "kit":
                plugin.getKitManager().saveKit(player);
                plugin.getConfigManager().send(player, "kit-saved");
                break;
            case "wand":
                giveWand(player);
                break;
            case "center":
                plugin.getEventManager().getEventData().setCenter(player.getLocation());
                plugin.getConfigManager().send(player, "center-set");
                break;
            case "spawn":
                plugin.getEventManager().getEventData().setSpawn(player.getLocation());
                plugin.getConfigManager().send(player, "spawn-set");
                break;
            case "lobby":
                plugin.getEventManager().getEventData().setLobby(player.getLocation());
                plugin.getConfigManager().send(player, "lobby-set");
                break;
            case "border":
                if (args.length < 3) {
                    plugin.getConfigManager().send(player, "usage-border");
                    return;
                }
                try {
                    double radius = Double.parseDouble(args[2]);
                    if (radius <= 0) {
                        plugin.getConfigManager().send(player, "invalid-number");
                        return;
                    }
                    plugin.getEventManager().getEventData().setBorderRadius(radius);
                    plugin.getConfigManager().send(player, "border-set", "{radius}", String.valueOf(radius));
                } catch (NumberFormatException e) {
                    plugin.getConfigManager().send(player, "usage-border");
                }
                break;
            default:
                sendSetupHelp(player);
                break;
        }
    }
    
    private void giveWand(Player player) {
        String materialName = plugin.getConfigManager().getString("wand.material");
        ItemStack wand = XMaterial.matchXMaterial(materialName).map(XMaterial::parseItem).orElse(null);
        
        if (wand == null) {
            return;
        }
        
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            String name = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getString("wand.name"));
            meta.setDisplayName(name);
            
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("wand.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            
            wand.setItemMeta(meta);
        }
        
        player.getInventory().addItem(wand);
        plugin.getConfigManager().send(player, "wand-given");
    }
    
    private void handleOpen(CommandSender sender) {
        if (plugin.getEventManager().getEventData().getState() != EventState.CLOSED) {
            plugin.getConfigManager().send(sender, "event-already-active");
            return;
        }
        
        if (!plugin.getEventManager().validateSetup()) {
            plugin.getConfigManager().send(sender, "setup-incomplete");
            return;
        }
        
        plugin.getEventManager().openEvent();
        plugin.getConfigManager().send(sender, "admin-event-opened");
    }
    
    private void handleStart(CommandSender sender) {
        if (plugin.getEventManager().getEventData().getState() != EventState.OPEN) {
            plugin.getConfigManager().send(sender, "event-must-be-open");
            return;
        }
        
        plugin.getEventManager().startEvent();
        plugin.getConfigManager().send(sender, "admin-countdown-started");
    }
    
    private void handleShrink(CommandSender sender, String[] args) {
        if (plugin.getEventManager().getEventData().getState() != EventState.RUNNING) {
            plugin.getConfigManager().send(sender, "event-not-running");
            return;
        }
        
        if (args.length < 3) {
            plugin.getConfigManager().send(sender, "usage-shrink");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            int time = Integer.parseInt(args[2]);
            
            if (amount <= 0 || time <= 0) {
                plugin.getConfigManager().send(sender, "invalid-number");
                return;
            }
            
            plugin.getBorderManager().shrinkBorder(amount, time);
            plugin.getConfigManager().send(sender, "admin-border-shrinking");
        } catch (NumberFormatException e) {
            plugin.getConfigManager().send(sender, "usage-shrink");
        }
    }
    
    private void handleExpand(CommandSender sender, String[] args) {
        if (plugin.getEventManager().getEventData().getState() != EventState.RUNNING) {
            plugin.getConfigManager().send(sender, "event-not-running");
            return;
        }
        
        if (args.length < 3) {
            plugin.getConfigManager().send(sender, "usage-expand");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            int time = Integer.parseInt(args[2]);
            
            if (amount <= 0 || time <= 0) {
                plugin.getConfigManager().send(sender, "invalid-number");
                return;
            }
            
            plugin.getBorderManager().expandBorder(amount, time);
            plugin.getConfigManager().send(sender, "admin-border-expanding");
        } catch (NumberFormatException e) {
            plugin.getConfigManager().send(sender, "usage-expand");
        }
    }
    
    private void handleDrop(CommandSender sender) {
        if (plugin.getEventManager().getEventData().getState() != EventState.RUNNING) {
            plugin.getConfigManager().send(sender, "event-not-running");
            return;
        }
        
        plugin.getBorderManager().startDropPhase();
        plugin.getConfigManager().send(sender, "admin-drop-activated");
    }
    
    private void handleCancel(CommandSender sender) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state == EventState.CLOSED) {
            plugin.getConfigManager().send(sender, "no-event-active");
            return;
        }
        
        plugin.getEventManager().cancelEvent();
        plugin.getConfigManager().send(sender, "admin-event-cancelled");
    }
    
    private void handleEnd(CommandSender sender) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state == EventState.CLOSED) {
            plugin.getConfigManager().send(sender, "no-event-active");
            return;
        }
        
        plugin.getEventManager().forceEndEvent();
        plugin.getConfigManager().send(sender, "admin-event-ended");
    }
    
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getKitManager().reload();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lSuccess! &7Configuration reloaded."));
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━&r &b&lCrystalPvP Event Commands &8&m━━━━━━━━━━━"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event setup &8- &7Configure event settings"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event open &8- &7Open event for players to join"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event start &8- &7Start the event countdown"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event shrink <amount> <time> &8- &7Shrink border"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event expand <amount> <time> &8- &7Expand border"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event drop &8- &7Activate drop phase"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event cancel &8- &7Cancel current event"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event end &8- &7Force end event"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event reload &8- &7Reload configuration"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b/event help &8- &7Show this help menu"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage("");
    }
    
    private void sendSetupHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━&r &6&lEvent Setup Commands &8&m━━━━━━━━━━━"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/event setup kit &8- &7Save current inventory as kit"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/event setup wand &8- &7Get selection wand"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/event setup center &8- &7Set arena center point"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/event setup spawn &8- &7Set spawn location"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/event setup lobby &8- &7Set lobby location"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/event setup border <radius> &8- &7Set border radius"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Tip: &fUse the wand to select the arena boundary"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage("");
    }
}