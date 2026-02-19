package com.crystalpvp.listeners;

import com.crystalpvp.CrystalPvP;
import com.crystalpvp.enums.EventState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {
    
    private final CrystalPvP plugin;
    
    public CombatListener(CrystalPvP plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        EventState state = plugin.getEventManager().getEventData().getState();
        
        if (state != EventState.RUNNING) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (plugin.getEventManager().getEventData().getState() == EventState.RUNNING) {
            plugin.getEventManager().handleDeath(player);
            event.setKeepInventory(false);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}