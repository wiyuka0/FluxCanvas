package com.wiyuka.fluxCanvas.events;

import com.wiyuka.fluxCanvas.FluxCanvas;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.wiyuka.fluxCanvas.persistence.PersistenceManager;

public class WorldLoadListener implements Listener {
    private JavaPlugin main;

    public WorldLoadListener(JavaPlugin main) {
        this.main = main;
    }

    @EventHandler
    public void onWorldLoad(ServerLoadEvent event) {
        Bukkit.getWorlds().forEach(world -> world.getEntities().stream().filter(
                entity -> entity.getPersistentDataContainer().has(FluxCanvas.KEY)
        ).forEach(Entity::remove));
        Bukkit.getGlobalRegionScheduler().runDelayed(main, task -> {
            PersistenceManager.initFromData();
        }, 5);
//        PersistenceManager.initFromData();
    }
}
