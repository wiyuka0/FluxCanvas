package com.wiyuka.fluxCanvas.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.block.EnderChest;

import java.util.HashMap;

public class CfgData {
    // Player Limit
    public int playerMaxScreens = 3;
    public int maxScreenSizeX = 5;
    public int maxScreenSizeY = 5;

    // Screen
    public int maximumNumberOfConnectedContainers = 16;
    public int maximumConnectionDistance = 64;
    public HashMap<String, Integer> materialsOneUnit = new HashMap<>();

    // Global
    public boolean noLimit = false;

    public JsonObject toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(this);
        return gson.fromJson(jsonString, JsonObject.class);
    }

    public static CfgData fromJson(String jsonData) {
        Gson gson = new Gson();
        CfgData cfgData = gson.fromJson(jsonData, CfgData.class);
        if (cfgData.materialsOneUnit == null) cfgData.materialsOneUnit = new HashMap<>();
        return cfgData;
    }
}
