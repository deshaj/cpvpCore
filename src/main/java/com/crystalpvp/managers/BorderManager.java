package com.crystalpvp.managers;

import com.cryptomorin.xseries.XMaterial;
import com.crystalpvp.CrystalPvP;
import com.crystalpvp.enums.EventState;
import org.bukkit.Bukkit;
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
    private final Map<UUID, Location> frozenPlayerLocations;
    private BukkitRunnable freezeTask;

    public BorderManager(CrystalPvP plugin) {
        this.plugin = plugin;
        this.removedBlocks = new HashMap<>();
        this.random = new Random();
        this.outsideBorderTime = new HashMap<>();
        this.frozenPlayerLocations = new HashMap<>();
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
        border.setDamageAmount(0.0);
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
                if (plugin.getEventManager().getEventData().getState() != EventState.RUNNING) {
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
                    double dx = playerLoc.getX() - borderCenter.getX();
                    double dz = playerLoc.getZ() - borderCenter.getZ();

                    boolean outside = Math.abs(dx) > borderSize || Math.abs(dz) > borderSize;

                    if (outside) {
                        UUID playerId = player.getUniqueId();
                        int timeOutside = outsideBorderTime.getOrDefault(playerId, 0) + 1;
                        outsideBorderTime.put(playerId, timeOutside);

                        int timeRemaining = 10 - timeOutside;

                        if (timeRemaining > 0) {
                            String title = plugin.getConfigManager().getMessageRaw("border-warning-title");
                            String subtitle = plugin.getConfigManager().getMessageRaw("border-warning-subtitle")
                                .replace("{time}", String.valueOf(timeRemaining));
                            player.sendTitle(title, subtitle, 0, 25, 5);
                        } else {
                            player.setHealth(0.0);
                            outsideBorderTime.remove(playerId);

                            String deathMsg = plugin.getConfigManager().getMessage("border-death")
                                .replace("{player}", player.getName());
                            for (String line : deathMsg.split("\n")) {
                                Bukkit.broadcastMessage(line);
                            }
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
        for (String line : msg.split("\n")) {
            Bukkit.broadcastMessage(line);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String title = plugin.getConfigManager().getMessageRaw("border-shrinking-title");
            String subtitle = plugin.getConfigManager().getMessageRaw("border-shrinking-subtitle")
                .replace("{size}", String.format("%.1f", newSize))
                .replace("{time}", String.valueOf(time));
            player.sendTitle(title, subtitle, 10, 60, 20);
        }
    }

    public void startDropPhase() {
        if (plugin.getEventManager().getEventData().getState() != EventState.RUNNING) {
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

        String dropMsg = plugin.getConfigManager().getMessage("drop-phase");
        for (String line : dropMsg.split("\n")) {
            Bukkit.broadcastMessage(line);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getConfigManager().sendTitle(player, "drop-phase-title", "drop-phase-subtitle", 10, 80, 20);
        }

        freezePlayers();

        int freezeTime = plugin.getConfigManager().getInt("drop-phase-freeze-time");

        new BukkitRunnable() {
            @Override
            public void run() {
                unfreezePlayersAndDrop();
            }
        }.runTaskLater(plugin, freezeTime * 20L);
    }

    private void freezePlayers() {
        frozenPlayerLocations.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getEventManager().isParticipant(player.getUniqueId())) {
                frozenPlayerLocations.put(player.getUniqueId(), player.getLocation().clone());
            }
        }

        if (freezeTask != null) {
            freezeTask.cancel();
        }

        freezeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (frozenPlayerLocations.isEmpty()) {
                    cancel();
                    return;
                }

                for (Map.Entry<UUID, Location> entry : frozenPlayerLocations.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.teleport(entry.getValue());
                    }
                }
            }
        };
        freezeTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void unfreezePlayersAndDrop() {
        if (freezeTask != null) {
            freezeTask.cancel();
            freezeTask = null;
        }
        frozenPlayerLocations.clear();

        removeAllBlocksAndPlaceBedrock();
    }

    private void removeAllBlocksAndPlaceBedrock() {
        Location center = plugin.getEventManager().getEventData().getCenter();
        int radius = plugin.getConfigManager().getInt("drop-phase-final-diameter") / 2;

        if (center == null) {
            return;
        }

        removedBlocks.clear();

        final Material bedrockMaterial = XMaterial.matchXMaterial("BEDROCK").map(XMaterial::parseMaterial).orElse(Material.BEDROCK);

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

                        if (y > -58) {
                            if (block.getType() != Material.AIR && block.getType() != bedrockMaterial) {
                                removedBlocks.put(block.getLocation(), block.getBlockData());
                                block.setType(Material.AIR);
                            }
                        } else {
                            if (block.getType() != bedrockMaterial) {
                                removedBlocks.put(block.getLocation(), block.getBlockData());
                                block.setType(bedrockMaterial);
                            }
                        }
                    }
                }

                y--;

                if (y < center.getWorld().getMinHeight()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
        for (String line : msg.split("\n")) {
            Bukkit.broadcastMessage(line);
        }

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
        try {
            Location center = plugin.getEventManager().getEventData().getCenter();
            if (center == null) {
                plugin.getLogger().warning("Cannot remove border: center is null!");
                return;
            }

            WorldBorder border = center.getWorld().getWorldBorder();
            border.setSize(59999968);
            border.setCenter(0, 0);

            stopBorderCheck();
            plugin.getLogger().info("Border removed successfully");
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing border: " + e.getMessage());
            stopBorderCheck();
        }
    }

    public void shutdown() {
        if (currentShrinkTask != null) {
            currentShrinkTask.cancel();
        }
        if (freezeTask != null) {
            freezeTask.cancel();
        }
        stopBorderCheck();
    }
}