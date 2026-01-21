package com.wiyuka.fluxCanvas.renderer.map;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;

public class DynamicMapRenderer extends MapRenderer {
    private final int[] buffer = new int[128 * 128];
    private boolean dirty = false;

    public void setBuffer(int[] newBuffer) {
        System.arraycopy(newBuffer, 0, this.buffer, 0, newBuffer.length);
        this.dirty = true;
    }
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (!dirty) return;
        dirty = false;
        for (int i = 0; i < buffer.length; i++) {
            canvas.setPixelColor(i % 128, i / 128, new Color(buffer[i], true));
        }
    }
}
