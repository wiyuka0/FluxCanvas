package com.wiyuka.fluxCanvas.renderer;

import com.wiyuka.fluxCanvas.FluxCanvas;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public class TextureManager {
    private static final Map<Material, Integer> textureCache = new HashMap<>();

    private static final String TEXTURE_DIR = "texture";

    public static int getMaterialTexId(Material mat) {
        if (mat == null) return 0;

        if (textureCache.containsKey(mat)) {
            return textureCache.get(mat);
        }

        int textureId = loadTextureFromDisk(mat);
        textureCache.put(mat, textureId);

        return textureId;
    }

    private static int loadTextureFromDisk(Material mat) {
        try {
            String filename = mat.name().toLowerCase();

            File file = new File(TEXTURE_DIR, filename + ".png");

            if (!file.exists()) {
                file = new File(TEXTURE_DIR, filename + ".jpg");
            }

            if (!file.exists()) {
                return 0;
            }

            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                FluxCanvas.err("无法读取图片文件: " + file.getName());
                return 0;
            }

            return OffscreenRenderer.uploadTexture(image);

        } catch (IOException e) {
            FluxCanvas.err("加载材质异常: " + mat.name());
            e.printStackTrace();
            return 0;
        }
    }

    public static void cleanup() {
        for (int texId : textureCache.values()) {
            if (texId != 0) {
                glDeleteTextures(texId);
            }
        }
        textureCache.clear();
    }
}