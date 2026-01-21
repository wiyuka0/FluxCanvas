package com.wiyuka.fluxCanvas.tracker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiContainerTracker {
    private final Set<Location> containerLocations = new HashSet<>();
    private final Map<Material, ItemStat> statsMap = new ConcurrentHashMap<>();
    private int totalItemCount = 0;

    public void addContainer(Location loc) {
        if (loc != null) {
            this.containerLocations.add(loc);
        }
    }

    public void clear() {
        this.containerLocations.clear();
        this.statsMap.clear();
        this.totalItemCount = 0;
    }

    public int getContainerCount() {
        return containerLocations.size();
    }

    public Set<Location> getContainerLocations() {
        synchronized (containerLocations) {
            return new HashSet<>(containerLocations);
        }
    }

    /**
     * per 20 ticks (1 second)
     */
    public void tick() {
        if (containerLocations.isEmpty()) return;

        Map<Material, Integer> currentSnapshot = new HashMap<>();
        Map<Material, Integer> partialFreeSpaceMap = new HashMap<>();

        int currentTotal = 0;
        int globalEmptySlots = 0;

        Iterator<Location> it = containerLocations.iterator();
        while (it.hasNext()) {
            Location loc = it.next();

            if (!loc.getChunk().isLoaded()) continue;
            BlockState state = loc.getBlock().getState();

            if (!(state instanceof Container container)) {
                it.remove();
                continue;
            }

            Inventory inv = container.getInventory();
            for (ItemStack item : inv.getStorageContents()) {
                if (item == null || item.getType() == Material.AIR) {
                    globalEmptySlots++;
                } else {
                    Material mat = item.getType();
                    int amount = item.getAmount();
                    int maxStack = mat.getMaxStackSize();

                    currentSnapshot.merge(mat, amount, Integer::sum);
                    currentTotal += amount;

                    if (amount < maxStack) {
                        partialFreeSpaceMap.merge(mat, maxStack - amount, Integer::sum);
                    }
                }
            }
        }

        this.totalItemCount = currentTotal;

        updateStats(currentSnapshot, partialFreeSpaceMap, globalEmptySlots);
    }


    public int getTotalItemCount() {
        return totalItemCount;
    }

    private void updateStats(Map<Material, Integer> currentSnapshot,
                             Map<Material, Integer> partialFreeSpaceMap,
                             int globalEmptySlots) {

        for (Map.Entry<Material, Integer> entry : currentSnapshot.entrySet()) {
            Material mat = entry.getKey();
            int currentCount = entry.getValue();

            int partialSpace = partialFreeSpaceMap.getOrDefault(mat, 0);
            int emptySpace = globalEmptySlots * mat.getMaxStackSize();
            int maxCapacity = currentCount + partialSpace + emptySpace;

            statsMap.computeIfAbsent(mat, k -> new ItemStat(mat))
                    .update(currentCount, maxCapacity);
        }

        for (Material mat : statsMap.keySet()) {
            if (!currentSnapshot.containsKey(mat)) {
                int virtualCapacity = globalEmptySlots * mat.getMaxStackSize();
                statsMap.get(mat).update(0, virtualCapacity);
            }
        }

        statsMap.values().removeIf(ItemStat::isDead);
    }

    public void removeContainer(Location loc) {
        containerLocations.remove(loc);
    }
    public List<ItemStat> getTopItems(int limit) {
        return statsMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.count, a.count))
                .limit(limit)
                .toList();
    }
}