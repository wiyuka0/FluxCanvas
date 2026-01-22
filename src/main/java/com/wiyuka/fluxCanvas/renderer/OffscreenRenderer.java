package com.wiyuka.fluxCanvas.renderer;

import com.wiyuka.fluxCanvas.api.UiLogic;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class OffscreenRenderer {
    private long window;
    private int fbo;
    private int texture;
    private final int width;
    private final int height;

    private long lastTime;

    private ImGuiContext imGuiContext;

    private static boolean glfwInitialize = false;


    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private ByteBuffer pixelBuffer;

    private byte[] frameBuf;

    public OffscreenRenderer(int pixelWidth, int pixelHeight) {
        this.width = pixelWidth;
        this.height = pixelHeight;
    }

    public static void initGLFW() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("无法初始化 GLFW");
        }
        glfwInitialize = true;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void init() {

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(width, height, "Hidden ImGui Server", 0, 0);
        if (window == 0) throw new RuntimeException("无法创建 GLFW 窗口");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        this.imGuiContext = ImGui.createContext();
        ImGui.setCurrentContext(this.imGuiContext);

        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(width, height);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);

        initFont(io);

        imGuiGlfw.init(window, false);
        imGuiGl3.init("#version 330");

        setupFBO();
        pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
    }

    private void initFont(ImGuiIO io) {
        io.getFonts().clear();


        ImFontConfig normalCfg = new ImFontConfig();
        normalCfg.setSizePixels(13f);
        ImFont normalFont = io.getFonts().addFontDefault(normalCfg);


        ImFontConfig f3_5Cfg = new ImFontConfig();
        f3_5Cfg.setSizePixels(13f * 3.5f);
        ImGuiUtil.setF3_5Font(io.getFonts().addFontDefault(f3_5Cfg));

        ImFontConfig f1_8Cfg = new ImFontConfig();
        f1_8Cfg.setSizePixels(13f * 1.8f);
        ImGuiUtil.setF1_8Font(io.getFonts().addFontDefault(f1_8Cfg));

        ImFontConfig f1_2Cfg = new ImFontConfig();
        f1_2Cfg.setSizePixels(13f * 1.2f);
        ImGuiUtil.setF1_2Font(io.getFonts().addFontDefault(f1_2Cfg));

        ImFontConfig f1_4Cfg = new ImFontConfig();
        f1_4Cfg.setSizePixels(13f * 1.4f);
        ImGuiUtil.setF1_4Font(io.getFonts().addFontDefault(f1_4Cfg));


        io.setFontDefault(normalFont);
        ImGuiUtil.setDefaultFont(normalFont);
    }

    private void setupFBO() {
        if(!glfwInitialize) return;
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FBO 创建失败");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void makeCurrent() {
        if(!glfwInitialize) return ;
        glfwMakeContextCurrent(window);
        ImGui.setCurrentContext(this.imGuiContext);
    }

    public byte[] render(UiLogic uiLogic) {
        if(!glfwInitialize) return null;
        glfwPollEvents();

        ImGuiIO io = ImGui.getIO();

        io.setDisplaySize(width, height);

        long time = System.nanoTime();
        float deltaTime = (float) ((time - lastTime) / 1_000_000_000.0);
        if (deltaTime <= 0.0f) deltaTime = 0.001f;
        io.setDeltaTime(deltaTime);
        lastTime = time;

        uiLogic.bindInput(io);

        imGuiGl3.newFrame();
        ImGui.newFrame();

        uiLogic.run();

        ImGui.render();

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f); // 背景色
        glClear(GL_COLOR_BUFFER_BIT);

        imGuiGl3.renderDrawData(ImGui.getDrawData());

        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(
                0, 0, width, height,
                0, 0, width, height,
                GL_COLOR_BUFFER_BIT,
                GL_NEAREST
        );
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);

        glfwSwapBuffers(window);

        byte[] data = new byte[width * height * 4];
        pixelBuffer.get(data);
        pixelBuffer.clear();
        this.frameBuf = data;
        return data;
    }

    public byte[] getFrameBuf() {
        return frameBuf;
    }


    public static int uploadTexture(BufferedImage image) {
        if(!glfwInitialize) return 0;
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glBindTexture(GL_TEXTURE_2D, 0);

        return textureId;
    }

    public void cleanup() {
        try {
            if(!glfwInitialize) return;
            makeCurrent();
////            imGuiGl3.shutdown();
////            imGuiGlfw.shutdown();
////            ImGui.destroyContext();
//            glDeleteFramebuffers(fbo);
//            glDeleteTextures(texture);
            glfwDestroyWindow(window);
        }catch(Exception e) {}
    }
    public static void cleanupGLFW() {
        glfwInitialize = false;
        glfwTerminate();
    }
}