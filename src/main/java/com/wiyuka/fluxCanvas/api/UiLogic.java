package com.wiyuka.fluxCanvas.api;

import imgui.ImGuiIO;

import java.util.HashMap;

public interface UiLogic {
    void setParams(HashMap<String, Object> arguments);
    void bindInput(ImGuiIO io);
    void run();
    void setClick();
    void updateInput(float x, float y, boolean down);
}
