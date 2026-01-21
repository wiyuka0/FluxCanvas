package com.wiyuka.fluxCanvas.tools;

import com.wiyuka.fluxCanvas.renderer.FluxScreen;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class MathTools {
    public static Vector getIntersection(Vector rayOrigin, Vector rayVector, Vector planeOrigin, BlockFace facing) {
        Vector normal = facing.getDirection();

        Vector rayDir = rayVector.clone().normalize();

        double denom = normal.dot(rayDir);

        if (Math.abs(denom) < 1e-6) return null;

        Vector diff = planeOrigin.clone().subtract(rayOrigin);

        double t = diff.dot(normal) / denom;

        if (t < 0 || t > rayVector.length()) return null;

        // p = origin + dir * t
        return rayOrigin.clone().add(rayDir.multiply(t));
    }
    public static Double getRayPlaneIntersection(Vector rayOrigin, Vector rayDir, Vector planeOrigin, BlockFace facing) {
        Vector normal = facing.getDirection();
        double denom = normal.dot(rayDir);
        if (Math.abs(denom) < 1e-6) return null;
        Vector diff = planeOrigin.clone().subtract(rayOrigin);
        double t = diff.dot(normal) / denom;
        if (t < 0) return null;

        return t;
    }

    public static boolean isPointInsideScreen(FluxScreen screen, Vector intersection, Vector planeOrigin, BlockFace facing) {
        Vector diff = intersection.clone().subtract(planeOrigin);

        double localX = 0;
        double localY = -diff.getY();

        switch (facing) {
            case SOUTH: localX = diff.getX(); break;
            case NORTH: localX = -diff.getX(); break;
            case EAST:  localX = -diff.getZ(); break;
            case WEST:  localX = diff.getZ(); break;
            default: return false;
        }

        double epsilon = 0.05;

        return localX >= -epsilon && localX <= screen.getWidthMulti() + epsilon &&
                localY >= -epsilon && localY <= screen.getHeightMulti() + epsilon;
    }

    public static float calculateAverage(float[] data) {
        if (data == null || data.length == 0) return 0;
        double sum = 0;
        int count = 0;
        for (float v : data) {
            sum += v;
            count++;
        }
        return count == 0 ? 0 : (float) (sum / count);
    }
}
