package com.cnaude.chairs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Directional;
import org.bukkit.material.Stairs;
import org.bukkit.material.Step;
import org.bukkit.material.WoodenStep;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

public class EventListener implements Listener {

    public Chairs plugin;
    public ChairsIgnoreList ignoreList;

    public EventListener(Chairs plugin, ChairsIgnoreList ignoreList) {
        this.plugin = plugin;
        this.ignoreList = ignoreList;
    }

    public boolean isValidChair(Block block) {
        for (ChairBlock cb : plugin.allowedBlocks) if (cb.getMat().equals(block.getType())) return true;
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getPlayer().getItemInHand().getType().isBlock() && (event.getPlayer().getItemInHand().getTypeId() != 0) && plugin.ignoreIfBlockInHand)
            return;
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        Stairs stairs = null;
        Step step = null;
        WoodenStep wStep = null;
        double sh = plugin.sittingHeight;
        boolean blockOkay = false;
        if (ignoreList.isIgnored(player.getName()) || plugin.permissions && !player.hasPermission("chairs.sit")) return;
        if (plugin.perItemPerms) {
            if (plugin.pm.getPermission("chairs.sit." + block.getTypeId()) == null) {
                plugin.pm.addPermission(new Permission("chairs.sit." + block.getTypeId(),
                        "Allow players to sit on a '" + block.getType().name() + "'",
                        PermissionDefault.FALSE));
            }
            if (plugin.pm.getPermission("chairs.sit." + block.getType().toString()) == null) {
                plugin.pm.addPermission(new Permission("chairs.sit." + block.getType().toString(),
                        "Allow players to sit on a '" + block.getType().name() + "'",
                        PermissionDefault.FALSE));
            }
            if (plugin.pm.getPermission("chairs.sit." + block.getTypeId() + ":" + block.getData()) == null) {
                plugin.pm.addPermission(new Permission("chairs.sit." + block.getTypeId() + ":" + block.getData(),
                        "Allow players to sit on a '" + block.getType().name() + "'",
                        PermissionDefault.FALSE));
            }
            if (plugin.pm.getPermission("chairs.sit." + block.getType().toString() + ":" + block.getData()) == null) {
                plugin.pm.addPermission(new Permission("chairs.sit." + block.getType().toString() + ":" + block.getData(),
                        "Allow players to sit on a '" + block.getType().name() + "'",
                        PermissionDefault.FALSE));
            }
        }

        for (ChairBlock cb : plugin.allowedBlocks) {
            if (cb.getMat().toString().contains("STAIRS")) {
                if (cb.getMat().equals(block.getType())) {
                    blockOkay = true;
                    sh = cb.getSitHeight();
                }
            } else if (cb.getMat().equals(block.getType()) && cb.getDamage() == block.getData()) {
                blockOkay = true;
                sh = cb.getSitHeight();
            }
        }
        if (blockOkay
                || (player.hasPermission("chairs.sit." + block.getTypeId() + ":" + block.getData()) && plugin.perItemPerms)
                || (player.hasPermission("chairs.sit." + block.getType().toString() + ":" + block.getData()) && plugin.perItemPerms)
                || (player.hasPermission("chairs.sit." + block.getTypeId()) && plugin.perItemPerms)
                || (player.hasPermission("chairs.sit." + block.getType().toString()) && plugin.perItemPerms)) {

            if (block.getState().getData() instanceof Stairs) stairs = (Stairs) block.getState().getData();
            else if (block.getState().getData() instanceof Step) step = (Step) block.getState().getData();
            else if (block.getState().getData() instanceof WoodenStep) wStep = (WoodenStep) block.getState().getData();
            else sh += plugin.sittingHeightAdj;

            int chairwidth = 1;

            // Check if block beneath chair is solid.
            if (block.getRelative(BlockFace.DOWN).isLiquid() || block.getRelative(BlockFace.DOWN).isEmpty() || !block.getRelative(BlockFace.DOWN).getType().isSolid())
                return;

            // Check if player is sitting.
            if (plugin.sit.containsKey(player.getName())) {
                plugin.sit.remove(player.getName());
                event.setCancelled(true);
                if (plugin.notifyplayer && !plugin.msgStanding.isEmpty()) player.sendMessage(plugin.msgStanding);
                return;
            }

            // Check for distance distance between player and chair.
            if (plugin.distance > 0 && player.getLocation().distance(block.getLocation().add(0.5, 0, 0.5)) > plugin.distance)
                return;

            if (stairs != null && stairs.isInverted() && plugin.invertedStairCheck || step != null && step.isInverted() && plugin.invertedStepCheck || wStep != null && wStep.isInverted() && plugin.invertedStepCheck)
                return;
            if (plugin.signCheck && stairs != null) {
                boolean sign1 = false;
                boolean sign2 = false;

                if (stairs.getDescendingDirection() == BlockFace.NORTH || stairs.getDescendingDirection() == BlockFace.SOUTH) {
                    sign1 = checkSign(block, BlockFace.EAST) || checkFrame(block, BlockFace.EAST, player);
                    sign2 = checkSign(block, BlockFace.WEST) || checkFrame(block, BlockFace.WEST, player);
                } else if (stairs.getDescendingDirection() == BlockFace.EAST || stairs.getDescendingDirection() == BlockFace.WEST) {
                    sign1 = checkSign(block, BlockFace.NORTH) || checkFrame(block, BlockFace.NORTH, player);
                    sign2 = checkSign(block, BlockFace.SOUTH) || checkFrame(block, BlockFace.SOUTH, player);
                }

                if (!(sign1 && sign2)) return;
            }

            // Check for maximal chair width.
            if (plugin.maxChairWidth > 0 && stairs != null) {
                if (stairs.getDescendingDirection() == BlockFace.NORTH || stairs.getDescendingDirection() == BlockFace.SOUTH) {
                    chairwidth += getChairWidth(block, BlockFace.EAST);
                    chairwidth += getChairWidth(block, BlockFace.WEST);
                } else if (stairs.getDescendingDirection() == BlockFace.EAST || stairs.getDescendingDirection() == BlockFace.WEST) {
                    chairwidth += getChairWidth(block, BlockFace.NORTH);
                    chairwidth += getChairWidth(block, BlockFace.SOUTH);
                }

                if (chairwidth > plugin.maxChairWidth) return;
            }

            // Sit-down process.
            if (!plugin.sneaking || (plugin.sneaking && event.getPlayer().isSneaking())) {
                if (plugin.seatOccupiedCheck && !plugin.sit.isEmpty()) {
                    for (String s : plugin.sit.keySet()) {
                        if (plugin.sit.get(s).equals(block.getLocation()) && !plugin.msgOccupied.isEmpty()) {
                            player.sendMessage(plugin.msgOccupied.replaceAll("%PLAYER%", s));
                            return;
                        }
                    }
                }

                Location chairLoc = event.getClickedBlock().getLocation().add(0.5, 0, 0.5);

                if (plugin.autoRotate && stairs != null) {
                    BlockFace direction = ((Directional) event.getClickedBlock().getState().getData()).getFacing();
                    double dx = direction.getModX();
                    double dy = direction.getModY();
                    double dz = direction.getModZ();
                    if (dx != 0) {
                        if (dx < 0) chairLoc.setYaw((float) (1.5 * Math.PI));
                        else chairLoc.setYaw((float) (0.5 * Math.PI));
                        chairLoc.setYaw((float) (chairLoc.getYaw() - Math.atan(dz / dx)));
                    } else if (dz < 0) chairLoc.setYaw((float) Math.PI);
                    double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));
                    chairLoc.setPitch((float) -Math.atan(dy / dxz));
                    chairLoc.setYaw(-chairLoc.getYaw() * 180f / (float) Math.PI);
                    chairLoc.setPitch(chairLoc.getPitch() * 180f / (float) Math.PI);
                } else {
                    chairLoc.setPitch(player.getPlayer().getLocation().getPitch());
                    chairLoc.setYaw(player.getPlayer().getLocation().getYaw());
                }
                //player.setSneaking(true);
                if (plugin.notifyplayer && !plugin.msgSitting.isEmpty()) player.sendMessage(plugin.msgSitting);
                player.getPlayer().teleport(chairLoc);
                Entity ar = block.getWorld().spawnArrow(getBlockCentre(block).subtract(0, 0.5, 0), new Vector(0, 0, 0), 0, 0);
                player.setSneaking(false);
                ar.setPassenger(player);
                ar.setTicksLived(1);
                plugin.sit.put(player.getName(), ar);
                event.setCancelled(true);
            }
        }
    }

    // https://github.com/sk89q/craftbook/blob/master/src/main/java/com/sk89q/craftbook/util/BlockUtil.java
    public static Location getBlockCentre(Block block) {
        return block.getLocation().add(0.5, 0.5, 0.5);
    }

    private int getChairWidth(Block block, BlockFace face) {
        int width = 0;

        // Go through the blocks next to the clicked block and check if there are any further stairs.
        for (int i = 1; i <= plugin.maxChairWidth; i++) {
            Block relative = block.getRelative(face, i);
            if (!(relative.getState().getData() instanceof Stairs)) continue;
            if (isValidChair(relative) && ((Stairs) relative.getState().getData()).getDescendingDirection() == ((Stairs) block.getState().getData()).getDescendingDirection())
                width++;
            else break;
        }

        return width;
    }

    private boolean checkSign(Block block, BlockFace face) {
        // Go through the blocks next to the clicked block and check if are signs on the end.
        for (int i = 1; i <= 100; i++) {
            Block relative = block.getRelative(face, i);
            if (checkDirection(block, relative)) continue;
            return plugin.validSigns.contains(relative.getType());
        }
        return false;
    }

    private boolean checkDirection(Block block1, Block block2) {
        return !(!(block1.getState().getData() instanceof Stairs) || !(block2.getState().getData() instanceof Stairs)) && ((Stairs) block1.getState().getData()).getDescendingDirection().equals(((Stairs) block2.getState().getData()).getDescendingDirection());
    }

    private boolean checkFrame(Block block, BlockFace face, Player player) {
        for (int i = 1; i <= 100; i++) {
            Block relative = block.getRelative(face, i);
            if (checkDirection(block, relative)) continue;
            if (!relative.getType().equals(Material.AIR)) return false;
            int x = relative.getLocation().getBlockX();
            int y = relative.getLocation().getBlockY();
            int z = relative.getLocation().getBlockZ();
            for (Entity e : player.getNearbyEntities(plugin.distance, plugin.distance, plugin.distance)) {
                if (!(e instanceof ItemFrame) || !plugin.validSigns.contains(Material.ITEM_FRAME)) continue;
                int x2 = e.getLocation().getBlockX();
                int y2 = e.getLocation().getBlockY();
                int z2 = e.getLocation().getBlockZ();
                if (x == x2 && y == y2 && z == z2) return true;
            }
            return false;
        }
        return false;
    }
}
