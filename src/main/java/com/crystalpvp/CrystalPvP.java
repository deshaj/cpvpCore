package com.crystalpvp;

import com.crystalpvp.commands.EventCommand;
import com.crystalpvp.listeners.CombatListener;
import com.crystalpvp.listeners.ConnectionListener;
import com.crystalpvp.listeners.InteractionListener;
import com.crystalpvp.listeners.ProtectionListener;
import com.crystalpvp.managers.*;
import com.crystalpvp.placeholders.CrystalPvPExpansion;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CrystalPvP extends JavaPlugin {
    
    private ConfigManager configManager;
    private EventManager eventManager;
    private KitManager kitManager;
    private SelectionManager selectionManager;
    private BorderManager borderManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.eventManager = new EventManager(this);
        this.kitManager = new KitManager(this);
        this.selectionManager = new SelectionManager();
        this.borderManager = new BorderManager(this);
        
        EventCommand eventCommand = new EventCommand(this);
        getCommand("event").setExecutor(eventCommand);
        getCommand("event").setTabCompleter(eventCommand);
        
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InteractionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CrystalPvPExpansion(this).register();
        }
    }
    
    @Override
    public void onDisable() {
        if (borderManager != null) {
            borderManager.shutdown();
        }
    }
}