package com.crystalpvp.placeholders;

import com.crystalpvp.CrystalPvP;
import com.crystalpvp.data.WinnerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CrystalPvPExpansion extends PlaceholderExpansion {
    
    private final CrystalPvP plugin;
    
    public CrystalPvPExpansion(CrystalPvP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "crystalpvp";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        WinnerData winner = plugin.getEventManager().getLastWinner();
        
        if (winner == null) {
            return "None";
        }
        
        if (params.equalsIgnoreCase("name")) {
            return winner.getName();
        }
        
        if (params.equalsIgnoreCase("skin")) {
            return winner.getUuid();
        }
        
        return null;
    }
}