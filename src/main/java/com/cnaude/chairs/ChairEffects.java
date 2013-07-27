/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cnaude.chairs;

import org.bukkit.entity.Player;

/**
 * @author cnaude
 */
public class ChairEffects {

    Chairs plugin;
    int taskID;

    public ChairEffects(Chairs plugin) {
        this.plugin = plugin;
        effectsTask();
    }

    public void cancel() {
        plugin.getServer().getScheduler().cancelTask(taskID);
        taskID = 0;
    }

    public void restart() {
        this.cancel();
        this.effectsTask();
    }

    private void effectsTask() {
        taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    String pName = p.getName();
                    if (!plugin.sit.containsKey(pName)) continue;
                    if (!p.hasPermission("chairs.sit.health")) continue;
                    double pHealthPcnt = p.getHealth() / p.getMaxHealth() * 100d;
                    if (pHealthPcnt >= plugin.sitMaxHealth || p.getHealth() >= p.getMaxHealth()) continue;
                    double newHealth = plugin.sitHealthPerInterval + p.getHealth();
                    if (newHealth > p.getMaxHealth()) newHealth = p.getMaxHealth();
                    p.setHealth(newHealth);
                }
            }
        }, plugin.sitEffectInterval, plugin.sitEffectInterval);
    }
}
