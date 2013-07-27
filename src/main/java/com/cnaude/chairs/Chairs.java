package com.cnaude.chairs;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Chairs extends JavaPlugin {
    private static Chairs instance = null;
    public static ChairEffects chairEffects;
    public List<ChairBlock> allowedBlocks;
    public List<Material> validSigns;
    public boolean sneaking, autoRotate, signCheck, permissions, notifyplayer, opsOverridePerms;
    public boolean invertedStairCheck, seatOccupiedCheck, invertedStepCheck, perItemPerms, ignoreIfBlockInHand;
    public boolean sitEffectsEnabled;
    public double sittingHeight, sittingHeightAdj, distance;
    public int maxChairWidth;
    public int sitMaxHealth;
    public int sitHealthPerInterval;
    public int sitEffectInterval;
    private File pluginFolder;
    private File configFile;
    public byte sitByte;
    public HashMap<String, Entity> sit = new HashMap<String, Entity>();
    public static final String PLUGIN_NAME = "Chairs";
    public static final String LOG_HEADER = "[" + PLUGIN_NAME + "]";
    static final Logger log = Logger.getLogger("Minecraft");
    public PluginManager pm;
    public static ChairsIgnoreList ignoreList;
    public String msgSitting, msgStanding, msgOccupied, msgNoPerm, msgReloaded, msgDisabled, msgEnabled;

    @Override
    public void onEnable() {
        instance = this;
        ignoreList = new ChairsIgnoreList();
        ignoreList.load();
        pm = this.getServer().getPluginManager();
        pluginFolder = getDataFolder();
        configFile = new File(pluginFolder, "config.yml");
        createConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(new EventListener(this, ignoreList), this);
        getCommand("chairs").setExecutor(new ChairsCommand(this, ignoreList));
        if (sitEffectsEnabled) {
            logInfo("Enabling sitting effects.");
            chairEffects = new ChairEffects(this);
        }
    }

    @Override
    public void onDisable() {
        for (String pName : sit.keySet()) {
            Player player = getServer().getPlayer(pName);
            Location loc = player.getLocation().clone();
            loc.setY(loc.getY() + 1);
            player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);

        }
        if (ignoreList != null) ignoreList.save();
        if (chairEffects != null) chairEffects.cancel();
    }

    public void restartEffectsTask() {
        if (chairEffects != null) chairEffects.restart();
    }

    private void createConfig() {
        if (!pluginFolder.exists()) {
            try {
                pluginFolder.mkdir();
            } catch (Exception e) {
                logInfo("ERROR: " + e.getMessage());
            }
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                logInfo("ERROR: " + e.getMessage());
            }
        }
    }

    public void loadConfig() {
        sitByte = Byte.parseByte(getConfig().getString("packet"));
        logInfo("Sitting packet byte: " + sitByte);
        autoRotate = getConfig().getBoolean("auto-rotate");
        sneaking = getConfig().getBoolean("sneaking");
        signCheck = getConfig().getBoolean("sign-check");
        sittingHeight = getConfig().getDouble("sitting-height");
        sittingHeightAdj = getConfig().getDouble("sitting-height-adj");
        distance = getConfig().getDouble("distance");
        maxChairWidth = getConfig().getInt("max-chair-width");
        permissions = getConfig().getBoolean("permissions");
        notifyplayer = getConfig().getBoolean("notify-player");
        invertedStairCheck = getConfig().getBoolean("upside-down-check");
        seatOccupiedCheck = getConfig().getBoolean("seat-occupied-check");
        invertedStepCheck = getConfig().getBoolean("upper-step-check");
        perItemPerms = getConfig().getBoolean("per-item-perms");
        opsOverridePerms = getConfig().getBoolean("ops-override-perms");
        ignoreIfBlockInHand = getConfig().getBoolean("ignore-if-block-in-hand");

        sitEffectsEnabled = getConfig().getBoolean("sit-effects.enabled", false);
        sitEffectInterval = getConfig().getInt("sit-effects.interval", 20);
        sitMaxHealth = getConfig().getInt("sit-effects.healing.max-percent", 100);
        sitHealthPerInterval = getConfig().getInt("sit-effects.healing.amount", 1);

        msgSitting = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.sitting"));
        msgStanding = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.standing"));
        msgOccupied = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.occupied"));
        msgNoPerm = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-permission"));
        msgEnabled = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.enabled"));
        msgDisabled = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.disabled"));
        msgReloaded = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reloaded"));

        allowedBlocks = new ArrayList<ChairBlock>();
        for (String s : getConfig().getStringList("allowed-blocks")) {
            String type;
            double sh = sittingHeight;
            String d = "0";
            if (s.contains(":")) {
                String tmp[] = s.split(":", 3);
                type = tmp[0];
                if (!tmp[1].isEmpty()) {
                    sh = Double.parseDouble(tmp[1]);
                }
                if (tmp.length == 3) {
                    d = tmp[2];
                }
            } else {
                type = s;
            }
            try {
                Material mat;
                if (type.matches("\\d+")) {
                    mat = Material.getMaterial(Integer.parseInt(type));
                } else {
                    mat = Material.matchMaterial(type);
                }
                if (mat != null) {
                    logInfo("Allowed block: " + mat.toString() + " => " + sh + " => " + d);
                    allowedBlocks.add(new ChairBlock(mat, sh, d));
                } else {
                    logError("Invalid block: " + type);
                }
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }

        validSigns = new ArrayList<Material>();
        for (String type : getConfig().getStringList("valid-signs")) {
            try {
                if (type.matches("\\d+")) {
                    validSigns.add(Material.getMaterial(Integer.parseInt(type)));
                } else {
                    validSigns.add(Material.matchMaterial(type));
                }
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }

        ArrayList<String> perms = new ArrayList<String>();
        perms.add("chairs.sit");
        perms.add("chairs.reload");
        perms.add("chairs.self");
        perms.add("chairs.sit.health");
        for (String s : perms) {
            if (pm.getPermission(s) != null) {
                pm.removePermission(s);
            }
        }
        PermissionDefault pd;
        if (opsOverridePerms) {
            pd = PermissionDefault.OP;
        } else {
            pd = PermissionDefault.FALSE;
        }

        pm.addPermission(new Permission("chairs.sit", "Allow player to sit on a block.", pd));
        pm.addPermission(new Permission("chairs.reload", "Allow player to reload the Chairs configuration.", pd));
        pm.addPermission(new Permission("chairs.self", "Allow player to self disable or enable sitting.", pd));
    }

    public void logInfo(String _message) {
        log.log(Level.INFO, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logError(String _message) {
        log.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, _message));
    }

    public static Chairs get() {
        return instance;
    }

}
