package com.crystalpvp.managers;

import com.cryptomorin.xseries.XMaterial;
import com.crystalpvp.CrystalPvP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KitManager {
    
    private final CrystalPvP plugin;
    private final File kitFile;
    private FileConfiguration kitConfig;
    
    public KitManager(CrystalPvP plugin) {
        this.plugin = plugin;
        this.kitFile = new File(plugin.getDataFolder(), "kit.yml");
        loadKit();
    }
    
    private void loadKit() {
        if (!kitFile.exists()) {
            plugin.saveResource("kit.yml", false);
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitFile);
    }
    
    public void saveKit(Player player) {
        PlayerInventory inv = player.getInventory();
        
        kitConfig.set("kit", null);
        
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                saveItem("kit.inventory." + i, item);
            }
        }
        
        for (int i = 0; i < 4; i++) {
            ItemStack item = inv.getArmorContents()[i];
            if (item != null) {
                saveItem("kit.armor." + i, item);
            }
        }
        
        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null) {
            saveItem("kit.offhand", offhand);
        }
        
        try {
            kitConfig.save(kitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveItem(String path, ItemStack item) {
        XMaterial xMat = XMaterial.matchXMaterial(item.getType());
        kitConfig.set(path + ".material", xMat.name());
        kitConfig.set(path + ".amount", item.getAmount());
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            kitConfig.set(path + ".name", item.getItemMeta().getDisplayName());
        }
    }
    
    public void giveKit(Player player) {
        if (!kitConfig.contains("kit")) {
            return;
        }
        
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        
        ConfigurationSection invSection = kitConfig.getConfigurationSection("kit.inventory");
        if (invSection != null) {
            for (String key : invSection.getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = loadItem("kit.inventory." + key);
                if (item != null) {
                    player.getInventory().setItem(slot, item);
                }
            }
        }
        
        ItemStack[] armor = new ItemStack[4];
        ConfigurationSection armorSection = kitConfig.getConfigurationSection("kit.armor");
        if (armorSection != null) {
            for (String key : armorSection.getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = loadItem("kit.armor." + key);
                if (item != null && slot < 4) {
                    armor[slot] = item;
                }
            }
        }
        player.getInventory().setArmorContents(armor);
        
        ItemStack offhand = loadItem("kit.offhand");
        if (offhand != null) {
            player.getInventory().setItemInOffHand(offhand);
        }
        
        player.updateInventory();
    }
    
    private ItemStack loadItem(String path) {
        if (!kitConfig.contains(path + ".material")) {
            return null;
        }
        
        String materialName = kitConfig.getString(path + ".material");
        int amount = kitConfig.getInt(path + ".amount", 1);
        
        return XMaterial.matchXMaterial(materialName)
            .map(xMat -> {
                ItemStack item = xMat.parseItem();
                if (item != null) {
                    item.setAmount(amount);
                    
                    if (kitConfig.contains(path + ".name")) {
                        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(kitConfig.getString(path + ".name"));
                            item.setItemMeta(meta);
                        }
                    }
                }
                return item;
            })
            .orElse(null);
    }
}