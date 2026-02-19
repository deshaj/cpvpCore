package com.crystalpvp.managers;

import com.crystalpvp.data.Selection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    
    private final Map<UUID, Selection> selections;
    
    public SelectionManager() {
        this.selections = new HashMap<>();
    }
    
    public Selection getSelection(Player player) {
        return selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
    }
    
    public void removeSelection(Player player) {
        selections.remove(player.getUniqueId());
    }
}