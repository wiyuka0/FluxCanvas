package com.wiyuka.fluxCanvas.renderer.ui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

public class CanvasUIStyle {

    public static void set() {
        ImGuiStyle style = ImGui.getStyle();

        //todo update this file


        style.setWindowRounding(0.0f);
        style.setChildRounding(6.0f);
        style.setFrameRounding(4.0f);
        style.setGrabRounding(4.0f);
        style.setPopupRounding(4.0f);

        style.setItemSpacing(8, 6);
        style.setFramePadding(8, 4);

        style.setColor(ImGuiCol.WindowBg, 0.10f, 0.10f, 0.12f, 1.0f);
        style.setColor(ImGuiCol.PopupBg, 0.12f, 0.12f, 0.14f, 1.0f);

        style.setColor(ImGuiCol.Border, 0.25f, 0.25f, 0.28f, 1.0f);

        style.setColor(ImGuiCol.TitleBg, 0.12f, 0.12f, 0.14f, 1.0f);
        style.setColor(ImGuiCol.TitleBgActive, 0.12f, 0.12f, 0.14f, 1.0f);

        style.setColor(ImGuiCol.Tab, 0.10f, 0.10f, 0.12f, 1.0f);
        style.setColor(ImGuiCol.TabHovered, 0.20f, 0.20f, 0.24f, 1.0f);
        style.setColor(ImGuiCol.TabActive, 0.18f, 0.18f, 0.20f, 1.0f);
        style.setColor(ImGuiCol.TabUnfocused, 0.10f, 0.10f, 0.12f, 1.0f);
        style.setColor(ImGuiCol.TabUnfocusedActive, 0.18f, 0.18f, 0.20f, 1.0f);

        style.setColor(ImGuiCol.FrameBg, 0.16f, 0.16f, 0.19f, 1.0f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.20f, 0.20f, 0.24f, 1.0f);
        style.setColor(ImGuiCol.FrameBgActive, 0.24f, 0.24f, 0.28f, 1.0f);

        style.setColor(ImGuiCol.ScrollbarBg, 0.10f, 0.10f, 0.12f, 0.0f);
        style.setColor(ImGuiCol.ScrollbarGrab, 0.25f, 0.25f, 0.28f, 1.0f);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.30f, 0.30f, 0.35f, 1.0f);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.35f, 0.35f, 0.40f, 1.0f);

        style.setColor(ImGuiCol.Text, 0.90f, 0.90f, 0.92f, 1.0f);
        style.setColor(ImGuiCol.TextDisabled, 0.50f, 0.50f, 0.55f, 1.0f);

        style.setColor(ImGuiCol.PlotLines, 0.00f, 0.70f, 0.80f, 1.0f);
        style.setColor(ImGuiCol.PlotLinesHovered, 0.00f, 0.85f, 0.95f, 1.0f);
        style.setColor(ImGuiCol.PlotHistogram, 0.00f, 0.70f, 0.80f, 1.0f);
        style.setColor(ImGuiCol.PlotHistogramHovered, 0.00f, 0.85f, 0.95f, 1.0f);
    }
}
