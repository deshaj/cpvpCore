package com.crystalpvp.managers;

import com.crystalpvp.CrystalPvP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BorderManager {
    
    private final CrystalPvP plugin;
    private BukkitRunnable currentShrinkTask;
    private final Map<Location, BlockData> removedBlocks;
    private final Random random;
    private BukkitRunnable borderCheckTask;
    private final Map<UUID, Integer> outsideBorderTime;
    
    public BorderManager(CrystalPvP plugin) {
        this.plugin = plugin;
        this.removedBlocks = new HashMap<>();
        this.random = new Random();
        this.outsideBorderTime = new HashMap<>();
    }
    
    public void resetBorder() {
        Location center = plugin.getEventManager().getEventData().getCenter();
        double radius = plugin.getEventManager().getEventData().getBorderRadius();
        
        if (center == null || radius <= 0) {
            plugin.getLogger().warning("Cannot reset border: center is null or radius is invalid!");
            return;
        }
        
        WorldBorder border = center.getWorld().getWorldBorder();
        border.setCenter(center);
        border.setSize(radius * 2);
        border.setWarningDistance(0);
        border.setWarningTime(15);
        border.setDamageAmount(0.2);
        border.setDamageBuffer(5.0);
        
        plugin.getEventManager().getEventData().setCurrentBorderSize(radius * 2);
        
        plugin.getLogger().info("Border reset to center: " + center.getBlockX() + ", " + center.getBlockZ() + " with diameter: " + (radius * 2));
        
        startBorderCheck();
    }
    
    public void startBorderCheck() {
        if (borderCheckTask != null) {
            borderCheckTask.cancel();
        }
        
        outsideBorderTime.clear();
        
        borderCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getEventManager().getEventData().getState() != com.crystalpvp.enums.EventState.RUNNING) {
                    outsideBorderTime.clear();
                    cancel();
                    return;
                }
                
                Location center = plugin.getEventManager().getEventData().getCenter();
                if (center == null) {
                    return;
                }
                
                WorldBorder border = center.getWorld().getWorldBorder();
                double borderSize = border.getSize() / 2.0;
                Location borderCenter = border.getCenter();
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getEventManager().isParticipant(player.getUniqueId())) {
                        continue;
                    }
                    
                    Location playerLoc = player.getLocation();
                    double distance = Math.sqrt(
                        Math.pow(playerLoc.getX() - borderCenter.getX(), 2) +
                        Math.pow(playerLoc.getZ() - borderCenter.getZ(), 2)
                    );
                    
                    if (distance > borderSize) {
                        UUID playerId = player.getUniqueId();
                        int timeOutside = outsideBorderTime.getOrDefault(playerId, 0) + 1;
                        outsideBorderTime.put(playerId, timeOutside);
                        
                        int timeRemaining = 10 - timeOutside;
                        
                        if (timeRemaining > 0) {
                            String title = ChatColor.translateAlternateColorCodes('&', "&c&l⚠ WARNING ⚠");
                            String subtitle = ChatColor.translateAlternateColorCodes('&', 
                                "&7Return to the border in &c&l" + timeRemaining + "s &7or die!");
                            player.sendTitle(title, subtitle, 0, 25, 5);
                        } else {
                            player.setHealth(0.0);
                            outsideBorderTime.remove(playerId);
                            
                            String deathMsg = ChatColor.translateAlternateColorCodes('&', 
                                "&c" + player.getName() + " &7died outside the border!");
                            Bukkit.broadcastMessage(deathMsg);
                        }
                    } else {
                        outsideBorderTime.remove(player.getUniqueId());
                    }
                }
            }
        };
        borderCheckTask.runTaskTimer(plugin, 20L, 20L);
    }
    
    public void stopBorderCheck() {
        if (borderCheckTask != null) {
            borderCheckTask.cancel();
            borderCheckTask = null;
        }
        outsideBorderTime.clear();
    }
    
    public void shrinkBorder(double amount, int time) {
        Location center = plugin.getEventManager().getEventData().getCenter();
        double currentSize = plugin.getEventManager().getEventData().getCurrentBorderSize();
        
        if (center == null || currentSize <= 0) {
            plugin.getLogger().warning("Cannot shrink border: center is null or current size is invalid!");
            return;
        }
        
        double newSize = currentSize - amount;
        if (newSize < 0) {
            newSize = 0;
        }
        
        WorldBorder border = center.getWorld().getWorldBorder();
        border.setWarningDistance((int) (currentSize / 2));
        border.setSize(newSize, time);
        
        plugin.getEventManager().getEventData().setCurrentBorderSize(newSize);
        
        String msg = plugin.getConfigManager().getMessage("border-shrinking")
            .replace("{size}", String.format("%.1f", newSize))
            .replace("{time}", String.valueOf(time));
        Bukkit.broadcastMessage(msg);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String title = plugin.getConfigManager().getMessageRaw("border-shrinking-title");
            String subtitle = plugin.getConfigManager().getMessageRaw("border-shrinking-subtitle")
                .replace("{size}", String.format("%.1f", newSize))
                .replace("{time}", String.valueOf(time));
            player.sendTitle(title, subtitle, 10, 60, 20);
        }
    }
    
    public void startDropPhase() {
        if (plugin.getEventManager().getEventData().getState() != com.crystalpvp.enums.EventState.RUNNING) {
            plugin.getLogger().warning("Cannot start drop phase: event is not running!");
            return;
        }
        
        if (currentShrinkTask != null) {
            currentShrinkTask.cancel();
        }
        
        Location center = plugin.getEventManager().getEventData().getCenter();
        if (center == null) {
            plugin.getLogger().warning("Cannot start drop phase: center is not set!");
            return;
        }
        
        int duration = plugin.getConfigManager().getInt("drop-phase-duration");
        int finalDiameter = plugin.getConfigManager().getInt("drop-phase-final-diameter");
        
        WorldBorder border = center.getWorld().getWorldBorder();
        border.setWarningDistance(finalDiameter);
        border.setSize(finalDiameter, duration);
        
        plugin.getEventManager().getEventData().setCurrentBorderSize(finalDiameter);
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("drop-phase"));
        
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            plugin.getConfigManager().sendTitle(player, "drop-phase-title", "drop-phase-subtitle", 10, 80, 20);
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                removeBlocksAboveBedrock();
                dropPlayers();
            }
        }.runTaskLater(plugin, duration * 20L);
    }
    
    private void removeBlocksAboveBedrock() {
        Location center = plugin.getEventManager().getEventData().getCenter();
        int radius = plugin.getConfigManager().getInt("drop-phase-final-diameter") / 2;
        
        if (center == null) {
            return;
        }
        
        removedBlocks.clear();
        
        new BukkitRunnable() {
            int y = center.getWorld().getMaxHeight();
            
            @Override
            public void run() {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            y,
                            center.getBlockZ() + z
                        );
                        
                        if (block.getY() > -62 && block.getType() != Material.BEDROCK && block.getType() != Material.AIR) {
                            removedBlocks.put(block.getLocation(), block.getBlockData());
                            block.setType(Material.AIR);
                        }
                    }
                }
                
                y--;
                
                if (y <= -62) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void dropPlayers() {
        Location center = plugin.getEventManager().getEventData().getCenter();
        if (center == null) {
            return;
        }
        
        int radius = plugin.getConfigManager().getInt("drop-phase-final-diameter") / 2;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getEventManager().isParticipant(player.getUniqueId())) {
                int xOffset = random.nextInt(radius * 2) - radius;
                int zOffset = random.nextInt(radius * 2) - radius;
                
                Location dropLoc = center.clone();
                dropLoc.add(xOffset, 0, zOffset);
                dropLoc.setY(-62);
                player.teleport(dropLoc);
            }
        }
    }
    
    public void expandBorder(double amount, int time) {
        Location center = plugin.getEventManager().getEventData().getCenter();
        double currentSize = plugin.getEventManager().getEventData().getCurrentBorderSize();
        
        if (center == null || currentSize <= 0) {
            plugin.getLogger().warning("Cannot expand border: center is null or current size is invalid!");
            return;
        }
        
        double newSize = currentSize + amount;
        
        WorldBorder border = center.getWorld().getWorldBorder();
        border.setWarningDistance(0);
        border.setSize(newSize, time);
        
        plugin.getEventManager().getEventData().setCurrentBorderSize(newSize);
        
        String msg = plugin.getConfigManager().getMessage("border-expanding")
            .replace("{size}", String.format("%.1f", newSize))
            .replace("{time}", String.valueOf(time));
        Bukkit.broadcastMessage(msg);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String title = plugin.getConfigManager().getMessageRaw("border-expanding-title");
            String subtitle = plugin.getConfigManager().getMessageRaw("border-expanding-subtitle")
                .replace("{size}", String.format("%.1f", newSize))
                .replace("{time}", String.valueOf(time));
            player.sendTitle(title, subtitle, 10, 60, 20);
        }
    }
    
    public void restoreArena() {
        if (removedBlocks.isEmpty()) {
            return;
        }
        
        new BukkitRunnable() {
            int restored = 0;
            final int batchSize = 100;
            final java.util.Iterator<Map.Entry<Location, BlockData>> iterator = removedBlocks.entrySet().iterator();
            
            @Override
            public void run() {
                int count = 0;
                while (iterator.hasNext() && count < batchSize) {
                    Map.Entry<Location, BlockData> entry = iterator.next();
                    Block block = entry.getKey().getBlock();
                    block.setBlockData(entry.getValue());
                    iterator.remove();
                    count++;
                    restored++;
                }
                
                if (!iterator.hasNext()) {
                    plugin.getLogger().info("Arena restored: " + restored + " blocks");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    public void removeBorder() {
        Location center = plugin.getEventManager().getEventData().getCenter();
        if (center == null) {
            return;
        }
        
        WorldBorder border = center.getWorld().getWorldBorder();
        border.setSize(60000000);
        border.setCenter(0, 0);
        
        stopBorderCheck();
    }
    
    public void shutdown() {
        if (currentShrinkTask != null) {
            currentShrinkTask.cancel();
        }
        stopBorderCheck();
    }
}