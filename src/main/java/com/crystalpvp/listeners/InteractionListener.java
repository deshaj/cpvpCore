package com.crystalpvp.listeners;

import com.crystalpvp.CrystalPvP;
import com.crystalpvp.data.Selection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractionListener implements Listener {
    
    private final CrystalPvP plugin;
    
    public InteractionListener(CrystalPvP plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String wandName = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getString("wand.name"));
        
        if (!item.getItemMeta().getDisplayName().equals(wandName)) {
            return;
        }
        
        event.setCancelled(true);
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(event.getClickedBlock().getLocation());
            plugin.getEventManager().getEventData().setPos1(event.getClickedBlock().getLocation());
            
            String msg = plugin.getConfigManager().getMessage("pos1-set")
                .replace("{x}", String.valueOf(event.getClickedBlock().getX()))
                .replace("{y}", String.valueOf(event.getClickedBlock().getY()))
                .replace("{z}", String.valueOf(event.getClickedBlock().getZ()));
            player.sendMessage(msg);
            
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPos2(event.getClickedBlock().getLocation());
            plugin.getEventManager().getEventData().setPos2(event.getClickedBlock().getLocation());
            
            String msg = plugin.getConfigManager().getMessage("pos2-set")
                .replace("{x}", String.valueOf(event.getClickedBlock().getX()))
                .replace("{y}", String.valueOf(event.getClickedBlock().getY()))
                .replace("{z}", String.valueOf(event.getClickedBlock().getZ()));
            player.sendMessage(msg);
        }
    }
}