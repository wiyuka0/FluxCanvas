package com.wiyuka.fluxCanvas.tools;

import com.wiyuka.fluxCanvas.api.UiLogic;
import com.wiyuka.fluxCanvas.config.CfgData;
import com.wiyuka.fluxCanvas.config.ConfigManager;
import com.wiyuka.fluxCanvas.renderer.FluxScreen;
import com.wiyuka.fluxCanvas.renderer.OffscreenRenderer;
import com.wiyuka.fluxCanvas.renderer.ScreenManager;
import com.wiyuka.fluxCanvas.renderer.ui.CanvasUI;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScreenBuilder {
    private static final Map<UUID, BuildingSession> sessions = new HashMap<>();

    private static class BuildingSession {
        final Location startLocation;
        BlockDisplay previewEntity;

        public BuildingSession(Location startLocation) {
            this.startLocation = startLocation;
        }
    }

    public static void startBuilding(Player player, Location blockLocation) {
        cancelBuilding(player);

        BuildingSession session = new BuildingSession(blockLocation.getBlock().getLocation());

        World world = blockLocation.getWorld();
        if (world != null) {
            Location spawnLoc = session.startLocation.clone().add(0.5, 0.5, 0.5);

            session.previewEntity = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
            session.previewEntity.setBlock(Material.LIME_STAINED_GLASS.createBlockData());
            session.previewEntity.setGlowing(true);
            session.previewEntity.setGlowColorOverride(Color.LIME);
            session.previewEntity.setBrightness(new Display.Brightness(15, 15));

            session.previewEntity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f()
            ));
        }
        sessions.put(player.getUniqueId(), session);
    }

    public static void onTick(Player player) {
        BuildingSession session = sessions.get(player.getUniqueId());
        if (session == null || session.previewEntity == null || !session.previewEntity.isValid()) {
            return;
        }

        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null) return;

        Location endLoc = targetBlock.getLocation();
        Location startLoc = session.startLocation;


        if (!endLoc.getWorld().equals(startLoc.getWorld())) return;


        int minX = Math.min(startLoc.getBlockX(), endLoc.getBlockX());
        int minY = Math.min(startLoc.getBlockY(), endLoc.getBlockY());
        int minZ = Math.min(startLoc.getBlockZ(), endLoc.getBlockZ());

        int maxX = Math.max(startLoc.getBlockX(), endLoc.getBlockX());
        int maxY = Math.max(startLoc.getBlockY(), endLoc.getBlockY());
        int maxZ = Math.max(startLoc.getBlockZ(), endLoc.getBlockZ());

        float sizeX = (maxX - minX) + 1;
        float sizeY = (maxY - minY) + 1;
        float sizeZ = (maxZ - minZ) + 1;

        double boxCenterX = minX;// + (sizeX / 2.0);
        double boxCenterY = minY;// + (sizeY / 2.0);
        double boxCenterZ = minZ;// + (sizeZ / 2.0);

        Location entityLoc = session.previewEntity.getLocation();

        float transX = (float) (boxCenterX - entityLoc.getX());
        float transY = (float) (boxCenterY - entityLoc.getY());
        float transZ = (float) (boxCenterZ - entityLoc.getZ());

        session.previewEntity.setInterpolationDuration(1);
        session.previewEntity.setTransformation(new Transformation(
                new Vector3f(transX, transY, transZ),
//                new Vector3f(-0.5f, -0.5f, -0.5f),
                new AxisAngle4f(),
                new Vector3f(sizeX, sizeY, sizeZ),
                new AxisAngle4f()
        ));
    }

    public static void buildScreen(Player player) {
        BuildingSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            if (session.previewEntity != null) {
                session.previewEntity.remove();
            }
            org.bukkit.block.Block targetBlockPoint = player.getTargetBlockExact(10);
            BlockFace blockFace = player.getTargetBlockFace(10);
            if (targetBlockPoint == null || blockFace == null) {
                player.sendMessage("§c目标太远或无效！");
                return;
            }
            Block targetBlock = targetBlockPoint.getLocation().add(blockFace.getDirection().getX(), blockFace.getDirection().getY(), blockFace.getDirection().getZ()).getBlock();
            Location startLoc = session.startLocation;
            Location endLoc = targetBlock.getLocation();
            int minX = Math.min(startLoc.getBlockX(), endLoc.getBlockX());
            int maxX = Math.max(startLoc.getBlockX(), endLoc.getBlockX());
            int minY = Math.min(startLoc.getBlockY(), endLoc.getBlockY());
            int maxY = Math.max(startLoc.getBlockY(), endLoc.getBlockY());
            int minZ = Math.min(startLoc.getBlockZ(), endLoc.getBlockZ());
            int maxZ = Math.max(startLoc.getBlockZ(), endLoc.getBlockZ());
            int sizeX = maxX - minX + 1;
            int sizeZ = maxZ - minZ + 1;
            int heightMulti = maxY - minY + 1;
            BlockFace screenDirection = player.getFacing().getOppositeFace();
            if (screenDirection == BlockFace.UP || screenDirection == BlockFace.DOWN) {
                player.sendMessage("§c不支持水平地面/天花板屏幕，请平视前方构建！");
                return;
            }
            int widthMulti;
            Location originLocation;
            World world = startLoc.getWorld();
            switch (screenDirection) {
                case SOUTH:
                    if (sizeZ > 1) {
                        player.sendMessage("§c构建朝南屏幕时，请确保 Z 轴坐标一致 (不要选成立方体)！");
                        return;
                    }
                    widthMulti = sizeX;
                    originLocation = new Location(world, minX, maxY, minZ);
                    break;
                case NORTH:
                    if (sizeZ > 1) {
                        player.sendMessage("§c构建朝北屏幕时，请确保 Z 轴坐标一致！");
                        return;
                    }
                    widthMulti = sizeX;
                    originLocation = new Location(world, maxX, maxY, minZ);
                    break;
                case WEST:
                    if (sizeX > 1) {
                        player.sendMessage("§c构建朝西屏幕时，请确保 X 轴坐标一致！");
                        return;
                    }
                    widthMulti = sizeZ;
                    originLocation = new Location(world, minX, maxY, minZ);
                    break;
                case EAST:
                    if (sizeX > 1) {
                        player.sendMessage("§c构建朝东屏幕时，请确保 X 轴坐标一致！");
                        return;
                    }
                    widthMulti = sizeZ;
                    originLocation = new Location(world, minX, maxY, maxZ);
                    break;
                default:
                    player.sendMessage("§c无效的方向！");
                    return;
            }
            if (world != null) {
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (world.getBlockAt(x, y, z).getType() != Material.AIR) {
                                player.sendMessage("§c屏幕范围内包含多余的方块！");
                                return;
                            }
                        }
                    }
                }
            }
            CfgData currentConfig = ConfigManager.getCurrentConfig();
            if (widthMulti > currentConfig.maxScreenSizeX || heightMulti > currentConfig.maxScreenSizeY) {
                player.sendMessage("§c屏幕的大小超出限制！");
            }
            int pixelWidth = widthMulti * 128;
            int pixelHeight = heightMulti * 128;
            CanvasUI uiLogic = new CanvasUI();
            OffscreenRenderer renderer = new com.wiyuka.fluxCanvas.renderer.OffscreenRenderer(pixelWidth, pixelHeight);
            String screenId = UUID.randomUUID().toString().substring(0, 8);
            FluxScreen screen = new FluxScreen(screenId, originLocation, screenDirection, widthMulti, heightMulti, uiLogic, renderer, player.getUniqueId().toString());
//            screen.initRenderer();
            ScreenManager.addScreen(screenId, screen);
            player.sendMessage("§a屏幕构建成功！ID: " + screenId + " (" + widthMulti + "x" + heightMulti + ") 朝向: " + screenDirection);
        }
    }


    public static void cancelBuilding(Player player) {
        BuildingSession session = sessions.remove(player.getUniqueId());
        if (session != null && session.previewEntity != null) {
            session.previewEntity.remove();
        }
    }

    public static boolean isBuilding(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }
}