package com.wiyuka.fluxCanvas.renderer;

import com.wiyuka.fluxCanvas.tracker.MultiContainerTracker;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenManager {
    public static final Map<String, FluxScreen> screens = new ConcurrentHashMap<>();

    private static Thread renderThread;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public static void startRenderThread() {
        if (isRunning.get()) return;
        isRunning.set(true);

        renderThread = new Thread(ScreenManager::renderWhile, "FluxCanvas-Render-Thread");

        renderThread.start();
    }

    public static void stopRenderThread() {
        isRunning.set(false);
        if (renderThread != null) {
            try {
                renderThread.join(1000);
            } catch (InterruptedException ignored) {}
        }
        removeAll();
    }

    public static void addScreen(String id, FluxScreen screen) {
        if (screens.containsKey(id)) removeScreen(id);
        screens.put(id, screen);
    }

    public static FluxScreen getScreen(String id) {
        return screens.get(id);
    }

    public static void removeScreen(String id) {
        FluxScreen screen = screens.remove(id);
        willDestroy.add(screen);
        if (screen != null) screen.markDestroy();
    }

    private static final ArrayList<FluxScreen> willDestroy = new ArrayList<>();

    public static void removeAll() {
        for (FluxScreen screen : screens.values()) screen.markDestroy();
        willDestroy.addAll(screens.values());
        screens.clear();
    }

    public static Map<String, FluxScreen> getAllScreens() {
        return screens;
    }

    public static void runTick() {


        long tickCount = Bukkit.getCurrentTick();

        for (FluxScreen screen : screens.values()) {
            HashMap<String, Object> params = new HashMap<>();

            if (screen.getOriginLocation().getChunk().isLoaded())
                params.put("redstone", screen.getOriginLocation().getBlock().getBlockPower());
            if (tickCount % 20 == 0) screen.getTracker().tick();

            MultiContainerTracker tracker = screen.getTracker();
            params.put("linked", tracker.getContainerCount() > 0);
            if (tracker.getContainerCount() > 0) {
                params.put("container_count", tracker.getContainerCount());
                params.put("total_items", tracker.getTotalItemCount());
                params.put("top_items", tracker.getTopItems(tracker.getTotalItemCount()));
                params.put("container", tracker);
            }
            screen.updateParams(params);
            screen.uploadToMap();
        }
    }

    public static HashMap<String, FluxScreen> getScreensInWorld(String worldName) {
        HashMap<String, FluxScreen> screensInWorld = new HashMap<>();
        for (Map.Entry<String, FluxScreen> entry : screens.entrySet()) {
            if (entry.getValue().getOriginLocation().getWorld().getName().equals(worldName)) {
                screensInWorld.put(entry.getKey(), entry.getValue());
            }
        }
        return screensInWorld;
    }

    public static void removeScreensInWorld(String worldName) {
        Iterator<Map.Entry<String, FluxScreen>> iterator = screens.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FluxScreen> entry = iterator.next();
            if (entry.getValue().getOriginLocation().getWorld().getName().equals(worldName)) {
                FluxScreen screen = entry.getValue();
                screen.markDestroy();
                willDestroy.add(screen);
                iterator.remove();
            }
        }
    }

    public static HashMap<String, Integer> getPlayerCreated() {
        HashMap<String, Integer> playerScreenCounts = new HashMap<>();
        for (FluxScreen screen : screens.values()) {
            String owner = screen.getOwnerUniqueId().toString();
            if (owner != null) {
                playerScreenCounts.put(owner, playerScreenCounts.getOrDefault(owner, 0) + 1);
            }
        }
        return playerScreenCounts;
    }

    private static void renderWhile() {
        while (isRunning.get()) {
            long start = System.currentTimeMillis();

            for (FluxScreen screen : screens.values()) {
                try {
                    screen.renderAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            willDestroy.forEach(FluxScreen::renderAsync);
            willDestroy.clear();

            long elapsed = System.currentTimeMillis() - start;
            long wait = 50 - elapsed;
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}