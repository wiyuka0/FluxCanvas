package com.wiyuka.fluxCanvas.config;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.wiyuka.fluxCanvas.FluxCanvas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

public class ConfigManager {
    private static CfgData currentConfig = new CfgData();
    public static void initConfig() throws IOException {
        File configFile = new File(FluxCanvas.getInstance().getDataPath().toFile(), "fluxcanvas_cfg.json");
        if(!configFile.exists()) {
            configFile.createNewFile();
            Files.writeString(configFile.toPath(), new Gson().toJson(currentConfig));
            FluxCanvas.log("正在使用默认配置");
        }
        currentConfig = CfgData.fromJson(Files.readString(configFile.toPath()));
    }

    public static CfgData getCurrentConfig() {
        return currentConfig;
    }
}
