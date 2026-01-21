package persistence;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.wiyuka.fluxCanvas.FluxCanvas;
import com.wiyuka.fluxCanvas.renderer.FluxScreen;
import com.wiyuka.fluxCanvas.renderer.ScreenManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.sisu.launch.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PersistenceManager {
    private static File path;
    private static Gson gson;

    public static void init(JavaPlugin plugin) {
        gson = new Gson();
        path = new File(plugin.getDataFolder().getAbsoluteFile(), "data");
        if(!path.exists()) path.mkdirs();
    }

    private static void save(FluxScreen screen) throws IOException {
        var data = screen.toData();
        var jsonStr = gson.toJson(data);
        var targetFile = new File(path, data.id + ".json");
        if(!targetFile.exists()) targetFile.createNewFile();

        Files.writeString(targetFile.toPath(), jsonStr);
    }

    private static FluxScreen readAndCreate(String id) {
        var targetFile = new File(path, id + ".json");
        if(!targetFile.exists()) return null;
        try {
            var jsonStr = Files.readString(targetFile.toPath());
            var data = gson.fromJson(jsonStr, ScreenData.class);
            return FluxScreen.toInstance(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void saveAll() {
        saveAll(false);
    }

    public static void saveAll(boolean clear) {
        var screens = ScreenManager.getAllScreens();
        for (var screen : screens.values()) {
            try {
                save(screen);
            } catch (IOException e) {
                FluxCanvas.err("Failed to save screen " + screen.getId() + ": " + e.getMessage());
            }
        }
        if(clear) {
            ScreenManager.removeAll();
        }
    }

    public static void saveWorld(boolean clear, String worldName) {
        var screens = ScreenManager.getScreensInWorld(worldName);
        for (var screen : screens.values()) {
            try {
                save(screen);
            } catch (IOException e) {
                FluxCanvas.err("Failed to save screen " + screen.getId() + ": " + e.getMessage());
            }
        }
        if(clear) {
            ScreenManager.removeScreensInWorld(worldName);
        }
    }

    public static void initFromData() {
        var files = path.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (var file : files) {
            var id = file.getName().replace(".json", "");
            var screen = readAndCreate(id);
            if (screen != null) {
                ScreenManager.addScreen(id, screen);
            }
//                FluxCanvas.warn("Failed to load screen from file: " + file.getName());
        }
    }
}
