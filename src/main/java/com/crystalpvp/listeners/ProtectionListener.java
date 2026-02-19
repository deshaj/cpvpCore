package com.crystalpvp.listeners;

import com.crystalpvp.CrystalPvP;
import com.crystalpvp.enums.EventState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ProtectionListener implements Listener {
    
    private final CrystalPvP plugin;
    
    public ProtectionListener(CrystalPvP plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state == EventState.OPEN || state == EventState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state == EventState.OPEN || state == EventState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state == EventState.OPEN || state == EventState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state != EventState.RUNNING) {
            event.setCancelled(true);
        }
    }
}