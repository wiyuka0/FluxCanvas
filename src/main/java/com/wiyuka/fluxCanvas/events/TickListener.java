package com.wiyuka.fluxCanvas.events;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.wiyuka.fluxCanvas.renderer.ScreenManager;
import com.wiyuka.fluxCanvas.tools.ScreenBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TickListener implements Listener {
    @EventHandler
    public void onScreenBuilderUpdate(ServerTickEndEvent event) {
        Bukkit.getOnlinePlayers().forEach(ScreenBuilder::onTick);
    }

    @EventHandler
    public void onScreenUpdate(ServerTickEndEvent event) {
        ScreenManager.runTick();
    }
}
