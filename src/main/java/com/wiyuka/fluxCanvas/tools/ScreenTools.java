package com.wiyuka.fluxCanvas.tools;

import com.wiyuka.fluxCanvas.renderer.FluxScreen;
import com.wiyuka.fluxCanvas.renderer.ScreenManager;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ScreenTools {

    public static ScreenHit rayTraceScreen(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector eyePos = eyeLoc.toVector();
        Vector viewVec = eyeLoc.getDirection().multiply(10.0); // 10格距离

        ScreenHit closestHit = null;
        double closestDistSq = Double.MAX_VALUE;

        for (FluxScreen screen : ScreenManager.getAllScreens().values()) {
            if (!screen.getOriginLocation().getWorld().equals(player.getWorld())) continue;
            if (screen.getOriginLocation().distanceSquared(eyeLoc) > 169) continue; // 13^2

            Location originLoc = screen.getOriginLocation();
            Vector planeOrigin = new Vector(originLoc.getX(), originLoc.getY() + 1, originLoc.getZ());
            BlockFace facing = screen.getDirection();

            double offset = 1.0;
            switch (facing) {
                case NORTH:
                    planeOrigin.setZ(planeOrigin.getZ() + offset);
                    planeOrigin.setX(planeOrigin.getX() + offset);
                    break;
                case WEST:
                    planeOrigin.setX(planeOrigin.getX() + offset);
//                    planeOrigin.setZ(planeOrigin.getZ() - offset);
                    break;
                case EAST:
                    planeOrigin.setZ(planeOrigin.getZ() + offset);

            }

            Vector intersection = MathTools.getIntersection(eyePos, viewVec, planeOrigin, facing);
            if (intersection == null) continue;

            double distSq = intersection.distanceSquared(eyePos);
            if (distSq > closestDistSq) continue;

            Vector diff = intersection.clone().subtract(planeOrigin);

            double localX = 0;
            double localY = -diff.getY();

            switch (facing) {
                case NORTH: localX = -diff.getX(); break; // 向西延伸 (X变小)
                case SOUTH: localX = diff.getX();  break; // 向东延伸 (X变大)
                case WEST:  localX = diff.getZ();  break; // 向南延伸 (Z变大)
                case EAST:  localX = -diff.getZ(); break; // 向北延伸 (Z变小)
                default: continue;
            }

            double widthBlocks = screen.getWidthMulti();
            double heightBlocks = screen.getHeightMulti();

            if (localX >= 0 && localX <= widthBlocks && localY >= 0 && localY <= heightBlocks) {
                int pixelWidth = (int) (widthBlocks * 128);
                int pixelHeight = (int) (heightBlocks * 128);

                float ratioX = (float) (localX / widthBlocks);
                float ratioY = (float) (localY / heightBlocks);

                ratioX = Math.max(0, Math.min(1, ratioX));
                ratioY = Math.max(0, Math.min(1, ratioY));

                closestHit = new ScreenHit(screen, ratioX * pixelWidth, ratioY * pixelHeight);
                closestDistSq = distSq;
            }
        }

        return closestHit;
    }

    public static FluxScreen getScreenLookingAt(Player player, int maxDistance) {
        ScreenHit hit = rayTraceScreen(player);
        if (hit != null && player.getLocation().distance(hit.instance().getOriginLocation()) <= maxDistance) {
            return hit.instance();
        }
        return null;
    }

    public record ScreenHit(FluxScreen instance, float screenX, float screenY) {}
}