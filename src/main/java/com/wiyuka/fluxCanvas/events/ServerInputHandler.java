package com.wiyuka.fluxCanvas.events;


import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.wiyuka.fluxCanvas.tools.ScreenTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import static com.wiyuka.fluxCanvas.tools.ScreenTools.rayTraceScreen;

public class ServerInputHandler implements Listener {
    @EventHandler
    public void onTick(ServerTickEndEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ScreenTools.ScreenHit hit = rayTraceScreen(player);
            if (hit != null) {
                hit.instance().updateInput(hit.screenX(), hit.screenY(), false);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (handleInteract(event.getPlayer(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (handleInteract(player, true)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (handleInteract(player, true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;

        boolean isClick = true;

        if (handleInteract(event.getPlayer(), isClick)) {
            event.setCancelled(true);
        }
    }

    private boolean handleInteract(Player player, boolean isClick) {
        ScreenTools.ScreenHit hit = rayTraceScreen(player);
        if (hit != null) {
            hit.instance().updateInput(hit.screenX(), hit.screenY(), isClick);

            if (isClick) {
                hit.instance().setClick();
            }
            return true;
        }
        return false;
    }
}