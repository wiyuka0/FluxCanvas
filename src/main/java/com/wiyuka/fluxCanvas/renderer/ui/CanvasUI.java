package com.wiyuka.fluxCanvas.renderer.ui;

import com.wiyuka.fluxCanvas.api.UiLogic;
import com.wiyuka.fluxCanvas.renderer.ImGuiUtil;
import com.wiyuka.fluxCanvas.renderer.TextureManager;
import com.wiyuka.fluxCanvas.tracker.ItemStat;
import com.wiyuka.fluxCanvas.tracker.MultiContainerTracker;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.*;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;

/**
 * REFACTOR: TACTICAL INDUSTRIAL THEME V2
 * 核心升级：切角几何、动态呼吸、渐变填充、高对比度数据展示
 */
public class CanvasUI implements UiLogic {

    // --- 调色板 (Tactical Palette V2) ---
    private static final int COL_BG_DARK      = 0xFF080808; // 深渊黑
    private static final int COL_BG_PANEL     = 0xCC121212; // 半透明面板
    private static final int COL_BG_HEADER    = 0xFF1E1E1E;

    // 核心色
    private static final int COL_ACCENT_ORANGE  = 0xFFD08000; // 琥珀色 (主交互)
    private static final int COL_ACCENT_CYAN    = 0xFF00A0A0; // 青色 (数据流)
    private static final int COL_ACCENT_RED     = 0xFFC03030; // 警报
    private static final int COL_ACCENT_GREEN   = 0xFF40C040; // 正常

    // 文字与线条
    private static final int COL_TEXT_HI      = 0xFFFFFFFF;
    private static final int COL_TEXT_MED     = 0xFFAAAAAA;
    private static final int COL_TEXT_LOW     = 0xFF555555;
    private static final int COL_LINE_DIM     = 0xFF333333;
    private static final int COL_LINE_LIT     = 0xFF666666;

    // 状态
    private float virtualMouseX = 0;
    private float virtualMouseY = 0;
    private boolean isLeftMouseDown = false;
    private boolean clickConsumed = false;
    private float time = 0; // 用于动画呼吸


    private int timeRangeIndex = 0; // 0: Realtime, 1: 1 Hour, 2: 1 Day
    private static final String[] TIME_RANGES = { "REALTIME", "1 HOUR", "1 DAY" };

    // 数据
    private List<ItemStat> topItems = null;
    private MultiContainerTracker containerTracker = null;
    private int linkedContainerCount = 0;

    // 视图
    private enum Page { DASHBOARD, ANALYTICS, NETWORK }
    private Page currentPage = Page.DASHBOARD;
    private ItemStat selectedItem = null;

    @Override
    public void setParams(HashMap<String, Object> args) {
        if (args == null) return;
        if (args.containsKey("top_items")) {
            //noinspection unchecked
            this.topItems = (List<ItemStat>) args.get("top_items");
            this.linkedContainerCount = (int) args.getOrDefault("container_count", 0);
        }
        if(args.containsKey("container")) {
            this.containerTracker = (MultiContainerTracker) args.get("container");
        }
    }

    @Override
    public void bindInput(ImGuiIO io) {
        io.setMousePos(virtualMouseX, virtualMouseY);
        io.setMouseDown(0, isLeftMouseDown);
        // 模拟时间流逝用于简单的UI动画
        time += io.getDeltaTime();
    }

    @Override
    public void setClick() {
        this.isLeftMouseDown = true;
        this.clickConsumed = false;
    }

    private void resetClickState() {
        if (this.isLeftMouseDown && !this.clickConsumed) {
            this.clickConsumed = true;
        } else {
            this.isLeftMouseDown = false;
        }
    }

    @Override
    public void updateInput(float x, float y, boolean down) {
        this.virtualMouseX = x;
        this.virtualMouseY = y;
        if(down) this.isLeftMouseDown = true;
    }

    @Override
    public void run() {
        ImGuiIO io = ImGui.getIO();
        float displayW = io.getDisplaySizeX();
        float displayH = io.getDisplaySizeY();

        // 样式设置：极简、无圆角、无填充
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COL_BG_DARK);
        ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_HI);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);

        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoScrollbar;

        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(displayW, displayH);

        ImGui.pushFont(ImGuiUtil.getDefaultFont());


        if (ImGui.begin("Flux_Industrial_Main", flags)) {
            // 1. 全局背景网格与装饰
            drawBackgroundEffect(displayW, displayH);

            // 布局定义
            float sidebarW = 70.0f;
            float contentW = displayW - sidebarW;

            // 2. 侧边栏
            drawSidebar(sidebarW, displayH);

            // 3. 内容区
            ImGui.setCursorPos(sidebarW, 0);
            ImGui.pushStyleColor(ImGuiCol.ChildBg, 0); // 透明Child，透出背景
            if (ImGui.beginChild("ContentArea", contentW, displayH, false)) {
                // 内容Padding
                ImGui.setCursorPos(30, 25);
                float workW = contentW - 60;
                float workH = displayH - 50;

                switch (currentPage) {
                    case DASHBOARD:
                        drawDashboard(workW, workH);
                        break;
                    case ANALYTICS:
                        drawAnalytics(workW, workH);
                        break;
                    case NETWORK:
                        drawNetwork(workW, workH);
                        break;
                }
            }
            ImGui.endChild();
            ImGui.popStyleColor();
        }
        ImGui.end();

        ImGui.popFont();

        ImGui.popStyleVar(3);
        ImGui.popStyleColor(2);

        resetClickState();
    }

    // --- 视觉组件绘制 (The Render Engine) ---

    // 绘制切角矩形 (Tactical Box)
    private void drawChamferedBox(ImDrawList dl, float x, float y, float w, float h, int bgCol, int borderCol, float cutSize) {
        // 构建切角路径
        // 左上 -> 右上 -> 右下 -> 左下
        dl.pathLineTo(x + cutSize, y);
        dl.pathLineTo(x + w, y);
        dl.pathLineTo(x + w, y + h - cutSize);
        dl.pathLineTo(x + w - cutSize, y + h);
        dl.pathLineTo(x, y + h);
        dl.pathLineTo(x, y + cutSize);
        dl.pathStroke(borderCol, ImDrawFlags.Closed, 1.0f);

        if ((bgCol & 0xFF000000) != 0) {
            // 重建路径进行填充
            dl.pathLineTo(x + cutSize, y);
            dl.pathLineTo(x + w, y);
            dl.pathLineTo(x + w, y + h - cutSize);
            dl.pathLineTo(x + w - cutSize, y + h);
            dl.pathLineTo(x, y + h);
            dl.pathLineTo(x, y + cutSize);
            dl.pathFillConvex(bgCol);
        }
    }

    private void drawBackgroundEffect(float w, float h) {
        ImDrawList dl = ImGui.getWindowDrawList();

        // 1. 基础深色渐变
        dl.addRectFilledMultiColor(0, 0, w, h,
                COL_BG_DARK, COL_BG_DARK, 0xFF151515, 0xFF151515);

        // 2. 工程网格 (虚化)
        float gridSize = 50.0f;
        int gridCol = 0xFF1A1A1A;
        for (float x = 0; x < w; x += gridSize) dl.addLine(x, 0, x, h, gridCol);
        for (float y = 0; y < h; y += gridSize) dl.addLine(0, y, w, y, gridCol);

        // 3. 装饰性扫描线 (顶部和底部)
        dl.addRectFilled(0, 0, w, 4, COL_ACCENT_ORANGE);
        dl.addRectFilled(0, h-2, w, h, COL_ACCENT_ORANGE);

        // 4. 右下角水印
        String ver = "FLUX.SYS // STABLE // BUILD 2026";
        dl.addText(w - 200, h - 30, COL_TEXT_LOW, ver);
    }

    private void drawSidebar(float w, float h) {
        ImDrawList dl = ImGui.getWindowDrawList();

        // 背景条
        dl.addRectFilled(0, 0, w, h, 0xFF101010);
        dl.addLine(w, 0, w, h, COL_LINE_DIM);

        // 按钮区域
        float startY = 60;
        float gap = 20;
        float btnSize = 40;

        drawSidebarBtn(w, startY, "DB", Page.DASHBOARD);
        drawSidebarBtn(w, startY + btnSize + gap, "NT", Page.NETWORK);
    }

    private void drawSidebarBtn(float w, float y, String label, Page target) {
        float size = 40;
        float x = (w - size) / 2;
        boolean isActive = (currentPage == target) || (target == Page.DASHBOARD && currentPage == Page.ANALYTICS);

        ImGui.setCursorPos(x, y);
        ImGui.pushID("nav_" + label);
        boolean clicked = ImGui.invisibleButton("##btn", size, size);
        ImGui.popID();

        if (clicked) {
            currentPage = target;
            if (target == Page.DASHBOARD) selectedItem = null;
        }

        boolean hovered = ImGui.isItemHovered();
        ImDrawList dl = ImGui.getWindowDrawList();

        int color = isActive ? COL_ACCENT_ORANGE : (hovered ? COL_TEXT_HI : COL_TEXT_MED);
        int border = isActive ? COL_ACCENT_ORANGE : COL_LINE_DIM;

        // 绘制激活状态的左侧光条
        if (isActive) {
            dl.addRectFilled(0, y, 4, y + size, COL_ACCENT_ORANGE);
            // 辉光效果
            dl.addRectFilled(0, y, 10, y + size, (COL_ACCENT_ORANGE & 0x00FFFFFF) | 0x44000000);
        }

        // 按钮框 (切角)
        drawChamferedBox(dl, x, y, size, size, hovered ? 0xFF222222 : 0, border, 6);

        // 文字
        float tx = x + (size - ImGui.calcTextSize(label).x) / 2;
        float ty = y + (size - ImGui.getTextLineHeight()) / 2;
        dl.addText(tx, ty, color, label);
    }

    // --- 页面内容 ---

    private void drawDashboard(float w, float h) {
        drawHeader("DASHBOARD", "SYSTEM OVERVIEW", w);

        // 顶部 KPI 卡片
        float kpiH = 80;
        float kpiW = (w - 20) / 3;

        drawKpiCard(0, 50, kpiW, kpiH, "TOTAL ITEMS",
                topItems == null ? "0" : String.valueOf(topItems.size()), COL_ACCENT_CYAN);

        drawKpiCard(kpiW + 10, 50, kpiW, kpiH, "LINKED NODES",
                String.valueOf(linkedContainerCount), COL_ACCENT_GREEN);

        drawKpiCard((kpiW + 10) * 2, 50, kpiW, kpiH, "FLUX STATE",
                "ACTIVE", COL_ACCENT_ORANGE);

        // 物品列表
        ImGui.setCursorPosY(150);
        ImGui.textColored(0.5f, 0.5f, 0.5f, 1, "// INVENTORY STREAM");
        ImGui.separator();
        ImGui.dummy(0, 10);

        if (topItems == null || topItems.isEmpty()) return;

        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
        if (ImGui.beginChild("ItemList", w, h - 180)) {
            float itemH = 60;
            float itemGap = 8;

            for (ItemStat stat : topItems) {
                drawItemCard(stat, w, itemH);
                ImGui.dummy(0, itemGap);
            }
        }
        ImGui.endChild();
        ImGui.popStyleColor();
    }

    private void drawItemCard(ItemStat stat, float w, float h) {
        ImDrawList dl = ImGui.getWindowDrawList();
        float x = ImGui.getCursorScreenPos().x;
        float y = ImGui.getCursorScreenPos().y;

        ImGui.pushID("card_" + stat.material.name());
        boolean clicked = ImGui.invisibleButton("##btn", w, h);
        ImGui.popID();

        boolean hovered = ImGui.isItemHovered();

        // 背景色根据速率变化
        int baseBg = COL_BG_PANEL;
        int accent = COL_LINE_DIM;

        if (stat.rate > 0) accent = COL_ACCENT_GREEN;
        else if (stat.rate < 0) accent = COL_ACCENT_RED;

        if (hovered) {
            baseBg = 0xFF252525;
            accent = COL_ACCENT_ORANGE;
        }

        // 绘制卡片主体 (左下和右上切角)
        float cut = 12.0f;
        dl.pathLineTo(x, y);
        dl.pathLineTo(x + w - cut, y); // 右上切角
        dl.pathLineTo(x + w, y + cut);
        dl.pathLineTo(x + w, y + h);
        dl.pathLineTo(x + cut, y + h); // 左下切角
        dl.pathLineTo(x, y + h - cut);
        dl.pathFillConvex(baseBg);

        // 边框
        dl.pathLineTo(x, y);
        dl.pathLineTo(x + w - cut, y);
        dl.pathLineTo(x + w, y + cut);
        dl.pathLineTo(x + w, y + h);
        dl.pathLineTo(x + cut, y + h);
        dl.pathLineTo(x, y + h - cut);
        dl.pathStroke(COL_LINE_DIM, ImDrawFlags.Closed, 1.0f);

        // 左侧状态条
        dl.addRectFilled(x, y + 4, x + 4, y + h - 4, accent);

        // 图标区
        int texId = TextureManager.getMaterialTexId(stat.material);
        if (texId != 0) {
            dl.addImage(texId, x + 15, y + 10, x + 55, y + 50);
        } else {
            // 文本占位符
            dl.addRect(x+15, y+10, x+55, y+50, COL_LINE_DIM);
        }

        // 文本信息
        float tx = x + 70;
        dl.addText(tx, y + 10, COL_TEXT_HI, stat.getName());
        dl.addText(tx, y + 30, COL_TEXT_LOW, stat.material.name());

        // 数据 (大字体)
        String countStr = formatCount(stat.count);
        float countW = ImGui.calcTextSize(countStr).x;
//        ImGui.setWindowFontScale(1.4f); // 放大数字
        ImGui.pushFont(ImGuiUtil.getF1_4Font());
        dl.addText(x + w - countW - 140, y + 15, COL_TEXT_HI, countStr);
//        ImGui.setWindowFontScale(1.0f);
        ImGui.popFont();

        // 速率
        String rateStr = (stat.rate > 0 ? "+" : "") + formatRate(stat.rate) + "/h";
        int rateCol = stat.rate == 0 ? COL_TEXT_MED : (stat.rate > 0 ? COL_ACCENT_GREEN : COL_ACCENT_RED);
        dl.addText(x + w - 100, y + 22, rateCol, rateStr);

        // 装饰线连接
        if (hovered) {
            dl.addLine(x + w - 130, y + 45, x + w - 20, y + 45, COL_ACCENT_ORANGE);
        }

        if (clicked) {
            this.selectedItem = stat;
            this.currentPage = Page.ANALYTICS;
        }
    }

    private void drawAnalytics(float w, float h) {
        if (selectedItem == null) { currentPage = Page.DASHBOARD; return; }

        // --- 顶部导航栏 ---
        if (ImGui.button("<< TERMINAL", 100, 30)) {
            currentPage = Page.DASHBOARD;
            selectedItem = null;
            return;
        }

        ImGui.dummy(0, 10);
        drawHeader("ANALYTICS", selectedItem.material.name(), w);

        // --- 1. 详情面板 ---
        float infoH = 110; //稍微增加高度以容纳更多信息
        float startX = ImGui.getCursorScreenPos().x;
        float startY = ImGui.getCursorScreenPos().y;

        ImDrawList dl = ImGui.getWindowDrawList();

        // 绘制面板背景
        drawChamferedBox(dl, startX, startY, w, infoH, COL_BG_PANEL, COL_LINE_DIM, 20);

        // ===========================
        // [左侧区域]：库存与速率
        // ===========================

        // 1. 库存标题
        ImGui.setCursorPos(20, ImGui.getCursorPosY() + 15);
        ImGui.textColored(0.5f, 0.5f, 0.5f, 1, "CURRENT STOCK");

        // 2. 库存大数字
        ImGui.pushFont(ImGuiUtil.get3_5Font());
        ImGui.setCursorPosX(20);
        ImGui.textColored(255, 255, 255, 255, formatCount(selectedItem.count));
        ImGui.popFont();

        // 3. 动态速率 (新增功能)
        // 根据选择的时间范围计算显示的速率逻辑
        // 注意：这里假设 stat.rate 是每tick或每秒的基础速率，如果 ItemStat 有预存的 avgRate1h 等字段可在此替换
        double displayRate = selectedItem.rate;
        String rateLabel = "REALTIME";

        if (timeRangeIndex == 1) { // 1 HOUR
            // 如果有历史平均数据，在这里赋值给 displayRate
            // displayRate = selectedItem.avgRate1h;
            rateLabel = "1H MEAN";
        } else if (timeRangeIndex == 2) { // 1 DAY
            // displayRate = selectedItem.avgRate1d;
            rateLabel = "24H MEAN";
        }

        String rateStr = (displayRate > 0 ? "+" : "") + formatRate(displayRate) + "/h";
        int rateCol = displayRate == 0 ? COL_TEXT_MED : (displayRate > 0 ? COL_ACCENT_GREEN : COL_ACCENT_RED);

        // 使用 DrawList 精确绘制在库存数字下方
        // startY + 80 大概在大数字下方
        dl.addText(startX + 20, startY + 75, rateCol, rateStr);

        // 绘制速率的小标签 (如 REALTIME)
//        float rateStrW = ImGui.calcTextSize(rateStr).x;
//        dl.addText(startX + 20 + rateStrW + 8, startY + 75, COL_TEXT_LOW, "// " + rateLabel);


        // ===========================
        // [右侧区域]：进度条与 ETF
        // ===========================

        float barWidth = 350;
        float rightContentX = startX + w - barWidth - 30;
        float rightContentY = startY + 25;

        // 1. 进度条标题
        dl.addText(rightContentX, rightContentY, COL_ACCENT_CYAN, "CONTAINER FILL LEVEL");

        // 2. 进度条
        drawSegmentedBar(dl, rightContentX, rightContentY + 25, barWidth, 16, (float)selectedItem.fillLevel);

        // 3. 百分比数字
        String pctText = String.format("%.1f%%", selectedItem.fillLevel * 100);
        dl.addText(rightContentX + barWidth + 10, rightContentY + 25, COL_TEXT_HI, pctText);

        // 4. 预计爆仓时间 (ETF) (新增功能)
        // 位置：进度条下方
        String etfVal = selectedItem.getEtfFormatted(); // 获取格式化后的时间

        // 只有当速率为正且未满时显示 ETF，或者速率为负时显示清空时间(如果API支持)，否则显示状态
        String etfLabel = "EST. TIME TO FULL:";
        int etfColor = COL_ACCENT_ORANGE;

        if (selectedItem.rate <= 0) {
            etfVal = "DRAINING / IDLE";
            etfColor = COL_TEXT_LOW;
            etfLabel = "STATUS:";
        } else if (selectedItem.fillLevel >= 1.0f) {
            etfVal = "OVERFLOW WARNING";
            etfColor = COL_ACCENT_RED;
            etfLabel = "STATUS:";
        }

        // 绘制标签
        dl.addText(rightContentX, rightContentY + 50, COL_TEXT_MED, etfLabel);
        // 绘制值 (紧接标签之后)
        float labelW = ImGui.calcTextSize(etfLabel).x;
        dl.addText(rightContentX + labelW + 8, rightContentY + 50, etfColor, etfVal);


        // --- 恢复光标位置 ---
        ImGui.setCursorPosY(startY + infoH + 20 - ImGui.getWindowPos().y);

        // --- 2. 示波器图表区域 ---
        float[] data = timeSelector(w, selectedItem);
        float chartH = h - 260;
        drawTechOscilloscope(w, chartH, data);
    }

    private void drawTechOscilloscope(float w, float h, float[] data) {
        ImDrawList dl = ImGui.getWindowDrawList();
        float x = ImGui.getCursorScreenPos().x;
        float y = ImGui.getCursorScreenPos().y;

        // --- 1. 数据预处理 (计算极值) ---
        float minVal = Float.MAX_VALUE;
        float maxVal = -Float.MAX_VALUE;
        double totalSum = 0;

        if (data == null || data.length == 0) {
            minVal = 0; maxVal = 100;
        } else {
            for (float v : data) {
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
                totalSum += v;
            }
        }

        // 避免除零
        if (Math.abs(maxVal - minVal) < 0.001f) {
            maxVal += 10;
            minVal -= 10;
        }

        // 自动量程 (Auto-Scaling) + 10% Padding
        float range = maxVal - minVal;
        float displayMax = maxVal + (range * 0.1f);
        float displayMin = minVal - (range * 0.1f);
        float displayRange = displayMax - displayMin;

        // --- 2. 计算均值 (Averages) ---

        // A. Total Avg
        float totalAvg = (data != null && data.length > 0) ? (float) (totalSum / data.length) : 0;

        // B. Avg 20 (Last 20 points)
        float avg20 = 0;
        if (data != null && data.length > 0) {
            double sum20 = 0;
            int count20 = 0;
            int startIdx = Math.max(0, data.length - 20);
            for (int i = startIdx; i < data.length; i++) {
                sum20 += data[i];
                count20++;
            }
            avg20 = (float) (sum20 / count20);
        }

        // C. Avg 3 (Last 3 points)
        float avg3 = 0;
        if (data != null && data.length > 0) {
            int startIdx = Math.max(0, data.length - 3);
            double sum3 = 0;
            int count3 = 0;
            for (int i = startIdx; i < data.length; i++) {
                sum3 += data[i];
                count3++;
            }
            avg3 = (float) (sum3 / count3);
        }

        // --- 3. 绘制背景与刻度 ---
        dl.addRectFilled(x, y, x + w, y + h, 0xFF050505);
        dl.addRect(x, y, x + w, y + h, COL_LINE_DIM);

        int gridDiv = 4;
        ImGui.pushFont(ImGuiUtil.getDefaultFont());
        for (int i = 0; i <= gridDiv; i++) {
            float ratio = (float)i / gridDiv;
            float ly = y + h - (h * ratio);

            // 网格线
            if (i > 0 && i < gridDiv) dl.addLine(x, ly, x + w, ly, 0xFF151515);

            // 右侧刻度数值
            float val = displayMin + (displayRange * ratio);
            String label = formatCount(val);
            float textW = ImGui.calcTextSize(label).x;
            dl.addText(x + w - textW - 5, ly - 13, COL_TEXT_LOW, label);
        }
        ImGui.popFont();

        if (data == null || data.length < 2) {
            dl.addText(x + w/2 - 30, y + h/2 - 10, COL_TEXT_LOW, "NO SIGNAL");
            return;
        }

        // --- 4. 绘制参考线 (Layered Trends) ---
        // 顺序很重要：先画长周期的在底部，短周期的在顶部

        // Layer 1: Total Average (绿色实线 - 长期基准)
        drawTrendLine(dl, x, y, w, h, totalAvg, displayMin, displayRange, COL_ACCENT_GREEN, "AVG(ALL)", false);

        // Layer 2: Avg 20 (青色虚线 - 中期趋势)
        drawTrendLine(dl, x, y, w, h, avg20, displayMin, displayRange, COL_ACCENT_CYAN, "AVG(20)", true);


        // --- 5. 绘制主波形 (Waveform) ---
        float step = w / (data.length - 1);
        int fillColor = 0x40D08000; // 橙色半透明
        int lineColor = COL_ACCENT_ORANGE;

        for (int i = 0; i < data.length - 1; i++) {
            float v1 = data[i];
            float v2 = data[i+1];

            float x1 = x + (i * step);
            float y1 = y + h - ((v1 - displayMin) / displayRange * h);
            float x2 = x + ((i + 1) * step);
            float y2 = y + h - ((v2 - displayMin) / displayRange * h);

            // Clamp
            y1 = Math.max(y, Math.min(y + h, y1));
            y2 = Math.max(y, Math.min(y + h, y2));
            float yBase = y + h;

            dl.addQuadFilled(x1, y1, x2, y2, x2, yBase, x1, yBase, fillColor);
            dl.addLine(x1, y1, x2, y2, lineColor, 2.0f);
        }

        // Layer 3: Avg 3 (黄色虚线 - 绘制在波形之上，作为高亮焦点)
        drawTrendLine(dl, x, y, w, h, avg3, displayMin, displayRange, 0xFFD0D040, "AVG(3)", true);

        // --- 6. 扫描线效果 ---
        float scanX = x + (float)((Math.sin(time * 2) + 1) * 0.5 * w);
        dl.addLine(scanX, y, scanX, y+h, 0x33FFFFFF);

        float mouseX = ImGui.getIO().getMousePos().x;
        float mouseY = ImGui.getIO().getMousePos().y;
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        if (isHovered) {
            // 1. 绘制十字线 (Crosshair Lines)
            // 垂直线
            dl.addLine(mouseX, y, mouseX, y + h, 0x88FFFFFF, 1.0f);
            // 水平线
            dl.addLine(x, mouseY, x + w, mouseY, 0x88FFFFFF, 1.0f);

            float hoverVal = displayMin + ((y + h - mouseY) / h * displayRange);

            // 3. 绘制数值标签 (Tag)
            String valStr = String.format("%.1f", hoverVal); // 保留一位小数
            float tagW = ImGui.calcTextSize(valStr).x + 10;
            float tagH = 18;

            // 标签跟随鼠标 Y 轴，但在图表右侧显示，避免遮挡波形
            float tagX = x + w - tagW;
            float tagY = mouseY - (tagH / 2);

            // 标签背景 (高亮橙色背景，黑色文字)
            dl.addRectFilled(tagX, tagY, tagX + tagW, tagY + tagH, COL_ACCENT_ORANGE);
            dl.addText(tagX + 5, tagY + 2, 0xFF000000, valStr);

            // 4. 装饰：在鼠标中心画个小靶心
            dl.addCircle(mouseX, mouseY, 4, COL_ACCENT_ORANGE);

            // 5. 装饰：在X轴对应位置画个时间标记（可选）
            // 假设 X 轴最右边是 NOW，往左是过去
            float timeRatio = (mouseX - x) / w; // 0.0 ~ 1.0
            String timeLabel = "T-" + String.format("%.0f%%", (1.0f - timeRatio) * 100);
            dl.addText(mouseX + 5, y + h - 20, COL_TEXT_MED, timeLabel);
        }
    }

    private void drawTrendLine(ImDrawList dl, float x, float y, float w, float h,
                               float val, float min, float range, int color, String label, boolean dashed) {
        // 1. 映射 Y 坐标
        float ly = y + h - ((val - min) / range * h);

        // 2. 边界检查 (Clamp)，防止画出框外
        if (ly < y || ly > y + h) return;

        // 3. 绘制线条
        if (dashed) {
            float dashSize = 5.0f;
            float gapSize = 5.0f;
            for (float dx = x; dx < x + w; dx += (dashSize + gapSize)) {
                dl.addLine(dx, ly, Math.min(dx + dashSize, x + w), ly, color, 1.0f);
            }
        } else {
            // 实线
            dl.addLine(x, ly, x + w, ly, color, 1.0f);
        }

        // 4. 绘制标签 (带背景以防看不清)
        float textY = ly - 14;
        // 如果线太靠近顶部，把字挪到线下面
        if (textY < y) textY = ly + 2;

        // 简单的文字背景阴影，增加可读性
        dl.addText(x + 6, textY + 1, 0xFF000000, label);
        dl.addText(x + 5, textY, color, label);
    }

    private float[] timeSelector(float w, ItemStat selectedItem) {

        float[] res = selectedItem.history;

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0); // 按钮紧贴

        float btnW = 80;
        float btnH = 24;

        // 将按钮组放在右侧
        ImGui.setCursorPosX(w - (btnW * 3) - 10);

        for (int i = 0; i < TIME_RANGES.length; i++) {
            boolean active = (i == timeRangeIndex);

            // 样式：激活时为橙色，未激活为暗色
            if (active) {
                ImGui.pushStyleColor(ImGuiCol.Button, COL_ACCENT_ORANGE);
                ImGui.pushStyleColor(ImGuiCol.Text, 0xFF000000); // 黑色字
            } else {
                ImGui.pushStyleColor(ImGuiCol.Button, COL_BG_PANEL);
                ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_MED);
            }

            if (ImGui.button(TIME_RANGES[i], btnW, btnH)) {
                timeRangeIndex = i;
            }

            ImGui.popStyleColor(2);

            // 绘制按钮边框
            if (i < TIME_RANGES.length - 1) ImGui.sameLine();
        }

        ImGui.popStyleVar();
        ImGui.dummy(0, 5);

        switch(timeRangeIndex) {
            case 0 -> // REALTIME
                    res =  selectedItem.history;
            case 1 -> // HOUR
                    res =  selectedItem.history1h;
            case 2 -> // DAY
                    res =  selectedItem.history1d;

            default -> res =  selectedItem.history;
        }
        return res;
    }

    private void drawNetwork(float w, float h) {
        drawHeader("NETWORK", "TOPOLOGY MAP", w);

        if (containerTracker == null || containerTracker.getContainerLocations().isEmpty()) {
            ImGui.textDisabled("NO NODES DETECTED");
            return;
        }

        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
        if (ImGui.beginChild("NetList", w, h - 80)) {
            int i = 0;
            for (Location loc : containerTracker.getContainerLocations()) {
                i++;
                drawNodeRow(loc, w, i);
                ImGui.dummy(0, 5);
            }
        }
        ImGui.endChild();
        ImGui.popStyleColor();
    }

    private void drawNodeRow(Location loc, float w, int idx) {
        ImDrawList dl = ImGui.getWindowDrawList();
        float x = ImGui.getCursorScreenPos().x;
        float y = ImGui.getCursorScreenPos().y;
        float h = 40;

        // 背景
        dl.addRectFilled(x, y, x + w, y + h, idx % 2 == 0 ? 0xFF0E0E0E : 0xFF121212);

        // 装饰点
        dl.addCircleFilled(x + 20, y + 20, 3, COL_ACCENT_ORANGE);

        ImGui.setCursorPos(40, ImGui.getCursorPosY() + 10);
        ImGui.text("NODE_" + String.format("%03d", idx));

        ImGui.sameLine(150);
        ImGui.textColored(0.6f, 0.6f, 0.6f, 1.0f, loc.getBlock().getType().name());

        ImGui.sameLine(w - 200);
        String coords = String.format("X:%d Y:%d Z:%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        ImGui.text(coords);

        ImGui.sameLine(w - 80);
        ImGui.setCursorPosY(ImGui.getCursorPosY() - 5);
        if (ImGui.button("UNLINK##" + idx, 60, 20)) {
            containerTracker.removeContainer(loc);
            linkedContainerCount--;
        }
    }

    // --- 通用组件 ---

    private void drawHeader(String title, String sub, float w) {
        // 大标题
//        ImGui.pushFont(ImGui.getFont()); // 假设有大字体更好，这里用默认
//        ImGui.pushFont(ImGuiUtil.getBigFont());
        ImGui.setCursorPosX(20);
        ImGui.textColored(1.0f, 0.6f, 0.0f, 1.0f, title);
//        ImGui.popFont();

        ImGui.sameLine();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + 2);
        ImGui.textDisabled(" - " + sub);

        ImDrawList dl = ImGui.getWindowDrawList();
        float ly = ImGui.getCursorScreenPos().y + ImGui.getTextLineHeightWithSpacing();
        float lx = ImGui.getCursorScreenPos().x;

        dl.addRectFilledMultiColor(lx, ly, lx + w, ly + 2,
                COL_ACCENT_ORANGE, 0x00000000, 0x00000000, COL_ACCENT_ORANGE);

        ImGui.dummy(0, 20);
    }

    private void drawKpiCard(float x, float y, float w, float h, String label, String value, int accentCol) {
        ImGui.setCursorPos(x, y);
        float sx = ImGui.getCursorScreenPos().x;
        float sy = ImGui.getCursorScreenPos().y;
        ImDrawList dl = ImGui.getWindowDrawList();

        // 绘制带切角的背景
        drawChamferedBox(dl, sx, sy, w, h, COL_BG_PANEL, COL_LINE_DIM, 10.0f);

        // 顶部高亮条
        dl.addRectFilled(sx + 10, sy, sx + 40, sy + 3, accentCol);

        // 文字
        dl.addText(sx + 15, sy + 15, COL_TEXT_MED, label);

        ImGui.pushFont(ImGuiUtil.getF1_8Font());
//        ImGui.setWindowFontScale(1.8f);
        dl.addText(sx + 15, sy + 35, COL_TEXT_HI, value);
//        ImGui.setWindowFontScale(1.0f);
        ImGui.popFont();
    }

    private void drawSegmentedBar(ImDrawList dl, float x, float y, float w, float h, float pct) {
        int segments = 15;
        float gap = 4.0f;
        float segW = (w - (gap * (segments - 1))) / segments;
        int activeSegs = (int) (segments * pct);

        for (int i = 0; i < segments; i++) {
            float sx = x + i * (segW + gap);
            // 激活颜色渐变
            int col = (i < activeSegs) ? COL_ACCENT_CYAN : 0xFF222222;
            if (i < activeSegs && i > activeSegs - 3) col = COL_TEXT_HI; // 亮头

            dl.addRectFilled(sx, y, sx + segW, y + h, col);
        }
    }

    private String formatCount(double value) {
        if (value < 0) return "-" + formatCount(-value);
        if (value < 1000) return String.format("%.0f", value);
        String[] suffixes = {"", "K", "M", "B"};
        int index = 0;
        while (value >= 1000 && index < suffixes.length - 1) {
            value /= 1000.0;
            index++;
        }
        return String.format("%.1f%s", value, suffixes[index]);
    }

    private String formatRate(double rate) {
        return formatCount(rate * 60);
    }
}