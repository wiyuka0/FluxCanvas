package com.wiyuka.fluxCanvas.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import persistence.PersistenceManager;

public class WorldSaveListener implements Listener {

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        PersistenceManager.saveWorld(false, event.getWorld().getName());
    }
}
