package com.wiyuka.fluxCanvas.events;

import com.wiyuka.fluxCanvas.renderer.FluxScreen;
import com.wiyuka.fluxCanvas.tools.ScreenTools;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LinkerListener implements Listener {
    private final Map<UUID, Set<Location>> selectionBuffer = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!isLinkerTool(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getState() instanceof org.bukkit.block.Container) {
                event.setCancelled(true);
                addLocationToBuffer(p, event.getClickedBlock().getLocation());
                return;
            }
        }

        if (event.getAction().name().contains("RIGHT")) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) event.setCancelled(true);

            tryLinkScreen(p);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!(event.getRightClicked() instanceof ItemFrame)) return;

        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!isLinkerTool(item)) return;

        event.setCancelled(true);

        tryLinkScreen(p);
    }

    private void tryLinkScreen(Player p) {
        FluxScreen screen = ScreenTools.getScreenLookingAt(p, 8);

        if (screen != null) {
            Set<Location> buffer = selectionBuffer.get(p.getUniqueId());
            if (buffer != null && !buffer.isEmpty()) {
                int count = 0;
                for (Location loc : buffer) {
                    screen.addLinkedContainer(loc);
                    count++;
                }
                p.sendMessage("§a成功连接 " + count + " 个容器到屏幕 " + screen.getId() + "！");
                buffer.clear();
            } else {
                p.sendMessage("§e缓冲区为空，请先右键点击容器进行选择。");
            }
        }
    }

    private void addLocationToBuffer(Player p, Location loc) {
        Set<Location> buffer = selectionBuffer.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (buffer.contains(loc)) {
            buffer.remove(loc);
            p.sendMessage("§e已移除该容器。当前选中: " + buffer.size());
        } else {
            buffer.add(loc);
            p.sendMessage("§a已添加容器。当前选中: " + buffer.size());
        }
    }
    private boolean isLinkerTool(ItemStack item) {
        return item != null && item.getType() == Material.BLAZE_ROD;
        //todo item meta check
    }
}