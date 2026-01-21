package com.wiyuka.fluxCanvas.tracker;

import com.wiyuka.fluxCanvas.tools.MathTools;
import org.bukkit.Material;

public class ItemStat {
    public final Material material;
    public int count = 0;
    public int rate = 0;

    public int maxCapacity = 0;
    public double fillLevel = 0.0;
    public long etfSeconds = -1;

    private int lastCount = -1;

    public final float[] history = new float[60];
    public final float[] history1h = new float[60];
    public final float[] history1d = new float[24];

    private int tickCounter = 0;
    private int minuteAccumulator = 0;

    public ItemStat(Material material) {
        this.material = material;
    }

    public void update(int newCount, int newMaxCapacity) {
        if (lastCount != -1) {
            int delta = newCount - lastCount;
            this.rate = delta * 3600;
        } else {
            this.rate = 0;
        }

        this.count = newCount;
        this.maxCapacity = newMaxCapacity;
        this.lastCount = newCount;

        if (this.maxCapacity > 0) {
            this.fillLevel = (double) this.count / this.maxCapacity;
        } else {
            this.fillLevel = 1.0;
        }

        int remainingSpace = this.maxCapacity - this.count;
        double ratePerSecond = MathTools.calculateAverage(this.history) / 3600;

        if (remainingSpace <= 0) this.etfSeconds = 0;
        else if (ratePerSecond > 0) this.etfSeconds = (long) (remainingSpace / ratePerSecond);
        else this.etfSeconds = -1;

        tickCounter++;
        minuteAccumulator += this.rate;

        if (tickCounter >= 60) {
            float avgRate = minuteAccumulator / 60.0f;
            System.arraycopy(history1h, 1, history1h, 0, history1h.length - 1);
            history1h[history1h.length - 1] = avgRate;
            tickCounter = 0;
            minuteAccumulator = 0;
        }

        System.arraycopy(history, 1, history, 0, history.length - 1);
        history[history.length - 1] = (float) this.rate;
    }

    public boolean isDead() {
        return count == 0 && rate == 0;
    }

    public String getFillLevelPercent() {
        return String.format("%.1f%%", fillLevel * 100);
    }

    public String getEtfFormatted() {
        if (etfSeconds == -1) return "∞";
        if (etfSeconds == 0) return "FULL";

        long hours = etfSeconds / 3600;
        long minutes = (etfSeconds % 3600) / 60;
        long seconds = etfSeconds % 60;

        if (hours > 0) return String.format("%dh %dm", hours, minutes);
        return String.format("%dm %ds", minutes, seconds);
    }

    public String getName() {
        return material.name();
    }
}
