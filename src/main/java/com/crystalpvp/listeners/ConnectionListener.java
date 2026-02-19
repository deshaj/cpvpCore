package com.crystalpvp.listeners;

import com.crystalpvp.CrystalPvP;
import com.crystalpvp.enums.EventState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {
    
    private final CrystalPvP plugin;
    
    public ConnectionListener(CrystalPvP plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state == EventState.OPEN) {
            plugin.getKitManager().giveKit(event.getPlayer());
            event.getPlayer().setGameMode(GameMode.SURVIVAL);
        } else if (state == EventState.COUNTDOWN || state == EventState.RUNNING) {
            if (Bukkit.hasWhitelist()) {
                event.getPlayer().kickPlayer(plugin.getConfigManager().getMessage("whitelist-enabled"));
            }
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEventManager().getParticipants().remove(event.getPlayer().getUniqueId());
        
        if (plugin.getEventManager().getEventData().getState() == EventState.RUNNING) {
            if (plugin.getEventManager().getParticipants().size() == 1) {
                plugin.getEventManager().handleDeath(event.getPlayer());
            }
        }
    }
}