package com.wiyuka.fluxCanvas;

import com.wiyuka.fluxCanvas.commands.FluxCanvasCommand;
import com.wiyuka.fluxCanvas.config.ConfigManager;
import com.wiyuka.fluxCanvas.events.*;
import com.wiyuka.fluxCanvas.persistence.ResourceExtractor;
import com.wiyuka.fluxCanvas.renderer.OffscreenRenderer;
import com.wiyuka.fluxCanvas.renderer.ScreenManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import com.wiyuka.fluxCanvas.persistence.PersistenceManager;

import java.io.File;
import java.io.IOException;

public final class FluxCanvas extends JavaPlugin {

    public static final NamespacedKey KEY = new NamespacedKey("fluxcanvas", "screen_id");
    public static final String FLUX_SCREEN_FRAME_FLAG = "FLUX_SCREEN_FRAME";

    private static FluxCanvas main;

    public static void log() {
        main.getLogger().info("[FluxCanvas] ");
    }

    public static void log(String msg) {
        main.getLogger().info("[FluxCanvas] " + msg);
    }

    public static void warn(String msg) {
        main.getLogger().warning("[FluxCanvas] " + msg);
    }

    public static void err(String msg) {
        main.getLogger().severe("[FluxCanvas] " + msg);
    }

    public static FluxCanvas getInstance() {
        return main;
    }

    public File jarFile() {
        return this.getFile();
    }

    @Override
    public void onEnable() {
        main = this;

        new ResourceExtractor(this).extractDataToRoot(false);

        OffscreenRenderer.initGLFW();
        Bukkit.getPluginManager().registerEvents(new ServerInputHandler(), this);
        Bukkit.getPluginManager().registerEvents(new TickListener(), this);
        Bukkit.getPluginManager().registerEvents(new LinkerListener(), this);
        Bukkit.getPluginManager().registerEvents(new WorldLoadListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldSaveListener(), this);

        this.getFile();
        try {
            ConfigManager.initConfig();
        } catch (IOException e) {
            err("无法加载配置文件，尝试使用默认配置。");
        }
        ScreenManager.startRenderThread();
        PersistenceManager.init(this);

        getCommand("fluxcanvas").setExecutor(new FluxCanvasCommand());
    }

    @Override
    public void onDisable() {
        PersistenceManager.saveAll();
        OffscreenRenderer.cleanupGLFW();

    }
}