package com.crystalpvp.managers;

import com.cryptomorin.xseries.XEntityType;
import com.crystalpvp.CrystalPvP;
import com.crystalpvp.data.EventData;
import com.crystalpvp.data.WinnerData;
import com.crystalpvp.enums.EventState;
import com.crystalpvp.utils.HeadChatUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
public class EventManager {
    
    private final CrystalPvP plugin;
    private final EventData eventData;
    private final Set<UUID> participants;
    private WinnerData lastWinner;
    private int countdownTask = -1;
    private BossBar countdownBar;
    
    public EventManager(CrystalPvP plugin) {
        this.plugin = plugin;
        this.eventData = new EventData();
        this.participants = new HashSet<>();
    }
    
    public boolean validateSetup() {
        return eventData.getCenter() != null &&
               eventData.getSpawn() != null &&
               eventData.getLobby() != null &&
               eventData.getBorderRadius() > 0;
    }
    
    public void openEvent() {
        if (!validateSetup()) {
            plugin.getLogger().warning("Cannot open event: setup is incomplete!");
            return;
        }
        
        eventData.setState(EventState.OPEN);
        Bukkit.setWhitelist(false);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getKitManager().giveKit(player);
        }
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("event-opened"));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getConfigManager().sendTitle(player, "event-opened-title", "event-opened-subtitle", 10, 80, 20);
        }
    }
    
    public void startEvent() {
        if (eventData.getState() != EventState.OPEN) {
            plugin.getLogger().warning("Cannot start event from state: " + eventData.getState());
            return;
        }
        
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.getLogger().warning("Cannot start event: no players online!");
            return;
        }
        
        eventData.setState(EventState.COUNTDOWN);
        Bukkit.setWhitelist(true);
        
        participants.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                participants.add(player.getUniqueId());
            }
        }
        
        plugin.getLogger().info("Event starting with " + participants.size() + " participants");
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("event-started"));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getConfigManager().sendTitle(player, "event-started-title", "event-started-subtitle", 10, 80, 20);
        }
        
        startCountdown();
    }
    
    private void startCountdown() {
        countdownBar = Bukkit.createBossBar(
            ChatColor.translateAlternateColorCodes('&', "&e&lEvent Starting in 30s"),
            BarColor.YELLOW,
            BarStyle.SOLID
        );
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            countdownBar.addPlayer(player);
        }
        
        countdownTask = new BukkitRunnable() {
            int time = 30;
            final int maxTime = 30;
            
            @Override
            public void run() {
                if (time == 10) {
                    teleportAllToSpawn();
                }
                
                double progress = (double) time / maxTime;
                countdownBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                
                String barTitle = ChatColor.translateAlternateColorCodes('&', 
                    "&e&lEvent Starting in &f" + time + "s");
                countdownBar.setTitle(barTitle);
                
                if (time <= 10) {
                    countdownBar.setColor(BarColor.RED);
                } else if (time <= 20) {
                    countdownBar.setColor(BarColor.YELLOW);
                }
                
                if (time <= 10 && time > 0) {
                    String title = plugin.getConfigManager().getMessageRaw("countdown")
                        .replace("{time}", String.valueOf(time));
                    String subtitle = plugin.getConfigManager().getMessageRaw("countdown-subtitle");
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(title, subtitle, 0, 25, 5);
                    }
                }
                
                if (time == 0) {
                    beginEvent();
                    removeBossBar();
                    cancel();
                }
                
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }
    
    private void removeBossBar() {
        if (countdownBar != null) {
            countdownBar.removeAll();
            countdownBar = null;
        }
    }
    
    private void teleportAllToSpawn() {
        if (eventData.getSpawn() == null) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participants.contains(player.getUniqueId())) {
                player.teleport(eventData.getSpawn());
            }
        }
    }
    
    private void beginEvent() {
        eventData.setState(EventState.RUNNING);
        
        String broadcast = plugin.getConfigManager().getMessage("event-begin-broadcast")
            .replace("{players}", String.valueOf(participants.size()));
        Bukkit.broadcastMessage(broadcast);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getConfigManager().sendTitle(player, "event-begin", "event-begin-subtitle", 10, 50, 10);
        }
        
        if (eventData.getCenter() != null && eventData.getBorderRadius() > 0) {
            plugin.getBorderManager().resetBorder();
            plugin.getLogger().info("Event started - border initialized");
        } else {
            plugin.getLogger().warning("Event started but border could not be initialized!");
        }
    }
    
    public void handleDeath(Player player) {
        if (eventData.getState() != EventState.RUNNING) {
            return;
        }
        
        if (!participants.contains(player.getUniqueId())) {
            return;
        }
        
        participants.remove(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        
        int remaining = participants.size();
        
        String msg = plugin.getConfigManager().getMessage("player-eliminated")
            .replace("{player}", player.getName())
            .replace("{remaining}", String.valueOf(remaining));
        Bukkit.broadcastMessage(msg);
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            String title = plugin.getConfigManager().getMessageRaw("player-eliminated-title")
                .replace("{player}", player.getName());
            String subtitle = plugin.getConfigManager().getMessageRaw("player-eliminated-subtitle");
            online.sendTitle(title, subtitle, 10, 40, 10);
        }
        
        if (remaining == 1) {
            declareWinner();
        }
    }
    
    private void declareWinner() {
        UUID winnerId = participants.iterator().next();
        Player winner = Bukkit.getPlayer(winnerId);
        
        if (winner == null) {
            endEvent();
            return;
        }
        
        lastWinner = new WinnerData(winner.getName(), winnerId.toString());
        
        List<String> headMessages = plugin.getConfig().getStringList("winner-head-chat");
        if (!headMessages.isEmpty()) {
            HeadChatUtil.sendWinnerHeadMessage(winner, headMessages);
        } else {
            String msg = plugin.getConfigManager().getMessage("winner-announced")
                .replace("{winner}", winner.getName());
            Bukkit.broadcastMessage(msg);
        }
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            String title = plugin.getConfigManager().getMessageRaw("winner-title")
                .replace("{winner}", winner.getName());
            String subtitle = plugin.getConfigManager().getMessageRaw("winner-subtitle");
            online.sendTitle(title, subtitle, 20, 100, 20);
        }
        
        spawnFireworks(winner);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                endEvent();
            }
        }.runTaskLater(plugin, 100L);
    }
    
    private void spawnFireworks(Player player) {
        Location loc = player.getLocation();
        int count = plugin.getConfigManager().getInt("firework-count");
        
        new BukkitRunnable() {
            int spawned = 0;
            
            @Override
            public void run() {
                if (spawned >= count) {
                    cancel();
                    return;
                }
                
                Firework firework = (Firework) loc.getWorld().spawnEntity(loc, XEntityType.FIREWORK_ROCKET.get());
                FireworkMeta meta = firework.getFireworkMeta();
                meta.setPower(1);
                meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.YELLOW, Color.ORANGE)
                    .withFade(Color.RED)
                    .trail(true)
                    .build());
                firework.setFireworkMeta(meta);
                
                spawned++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    private void endEvent() {
        eventData.setState(EventState.FINISHED);
        
        plugin.getBorderManager().removeBorder();
        plugin.getBorderManager().restoreArena();
        
        if (eventData.getLobby() != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(eventData.getLobby());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }
        
        participants.clear();
        eventData.setState(EventState.CLOSED);
        Bukkit.setWhitelist(false);
        
        plugin.getLogger().info("Event ended successfully");
    }
    
    public void cancelEvent() {
        cancelCountdown();
        
        plugin.getBorderManager().removeBorder();
        plugin.getBorderManager().restoreArena();
        
        if (eventData.getLobby() != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(eventData.getLobby());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }
        
        participants.clear();
        eventData.setState(EventState.CLOSED);
        Bukkit.setWhitelist(false);
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("event-cancelled"));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getConfigManager().sendTitle(player, "event-cancelled-title", "event-cancelled-subtitle", 10, 60, 20);
        }
        
        plugin.getLogger().info("Event cancelled by admin");
    }
    
    public void forceEndEvent() {
        cancelCountdown();
        
        plugin.getBorderManager().removeBorder();
        plugin.getBorderManager().restoreArena();
        
        if (participants.size() == 1) {
            declareWinner();
        } else {
            if (eventData.getLobby() != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.teleport(eventData.getLobby());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                }
            }
            
            participants.clear();
            eventData.setState(EventState.CLOSED);
            Bukkit.setWhitelist(false);
            
            Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("event-force-ended"));
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getConfigManager().sendTitle(player, "event-force-ended-title", "event-force-ended-subtitle", 10, 60, 20);
            }
        }
        
        plugin.getLogger().info("Event force-ended by admin");
    }
    
    public boolean isParticipant(UUID uuid) {
        return participants.contains(uuid);
    }
    
    public void cancelCountdown() {
        if (countdownTask != -1) {
            Bukkit.getScheduler().cancelTask(countdownTask);
            countdownTask = -1;
        }
        removeBossBar();
    }
}