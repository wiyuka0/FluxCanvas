package com.wiyuka.fluxCanvas.renderer;

import imgui.ImFont;
import imgui.ImGui;

public class ImGuiUtil {
    private static ImFont f3_5Font = null;
    private static ImFont defaultFont = null;
    private static ImFont f1_8Font = null;
    private static ImFont f1_2Font = null;
    private static ImFont f1_4Font = null;

    public static ImFont get3_5Font() {
        if(f3_5Font != null)
            return f3_5Font;
        else return ImGui.getFont();
    }

    public static void setF3_5Font(ImFont f3_5Font) {
        ImGuiUtil.f3_5Font = f3_5Font;
    }


    public static void setDefaultFont(ImFont defaultFont) {
        ImGuiUtil.defaultFont = defaultFont;
    }

    public static ImFont getDefaultFont() {
        if (defaultFont != null) {
            return defaultFont;
        }
        return ImGui.getFont();
    }

    public static void setF1_8Font(ImFont f1_8Font) {
        ImGuiUtil.f1_8Font = f1_8Font;
    }

    public static ImFont getF1_8Font() {
        if (f1_8Font != null) {
            return f1_8Font;
        }
        return ImGui.getFont();
    }

    public static void setF1_2Font(ImFont f1_2Font) {
        ImGuiUtil.f1_2Font = f1_2Font;
    }
    public static ImFont getF1_2Font() {
        if (f1_2Font != null) return f1_2Font;
        return ImGui.getFont();
    }

    public static ImFont getF1_4Font() {
        return f1_4Font;
    }

    public static void setF1_4Font(ImFont f1_4Font) {
        ImGuiUtil.f1_4Font = f1_4Font;
    }
}
