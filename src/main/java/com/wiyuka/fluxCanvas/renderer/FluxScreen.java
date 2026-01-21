package com.wiyuka.fluxCanvas.renderer;

import com.wiyuka.fluxCanvas.FluxCanvas;
import com.wiyuka.fluxCanvas.renderer.map.DynamicMapRenderer;
import com.wiyuka.fluxCanvas.renderer.ui.CanvasUI;
import com.wiyuka.fluxCanvas.tracker.MultiContainerTracker;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import persistence.ScreenData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class FluxScreen {
    private final String id;
    private final Location originLocation;
    private final BlockFace direction;
    private final int widthMulti; // blocks
    private final int heightMulti;
    private String ownerUniqueId;

    private final List<GlowItemFrame> frames = new ArrayList<>();
    private final DynamicMapRenderer[][] renderers;
    private final MapView[][] mapViews;

    private final CanvasUI uiLogic;
    private final OffscreenRenderer renderer;
    private final MultiContainerTracker tracker = new MultiContainerTracker();

    private final Object paramLock = new Object();
    private HashMap<String, Object> currentParams = new HashMap<>();

    private final Object bufferLock = new Object();
    private int[][][] readyBuffers = null;
    private boolean hasNewFrame = false;

    private boolean destroyed = false;

    public FluxScreen(String id, Location location, BlockFace direction, int widthMulti, int heightMulti, CanvasUI uiLogic, OffscreenRenderer renderer, String ownerUniqueId) {
        this.id = id;
        this.originLocation = location;
        this.direction = direction;
        this.widthMulti = widthMulti;
        this.heightMulti = heightMulti;

        this.renderers = new DynamicMapRenderer[widthMulti][heightMulti];
        this.mapViews = new MapView[widthMulti][heightMulti];

        this.uiLogic = uiLogic;
        this.renderer = renderer;

        spawnScreen();
        this.ownerUniqueId = ownerUniqueId;
    }

    public void setOwnerUniqueId(UUID ownerUniqueId) {
        this.ownerUniqueId = ownerUniqueId.toString();
    }

    public UUID getOwnerUniqueId() {
        return UUID.fromString(ownerUniqueId);
    }

    private void spawnScreen() {
        for (int x = 0; x < widthMulti; x++) {
            for (int y = 0; y < heightMulti; y++) {
                int dx = 0;
                int dz = 0;

                switch (direction) {
                    case SOUTH: dx = x;  break;
                    case NORTH: dx = -x; break;
                    case WEST:  dz = x;  break;
                    case EAST:  dz = -x; break;
                    default:    dx = x;  break;
                }

                Location spawnLoc = originLocation.clone().add(dx, -y, dz);

                spawnLoc.getChunk().load();
                cleanupOldFrames(spawnLoc);

                GlowItemFrame frame = spawnLoc.getWorld().spawn(spawnLoc, GlowItemFrame.class);
                frame.setFacingDirection(direction);
                frame.setRotation(Rotation.NONE);
                frame.setVisible(false); // Make the item frame invisible
                frame.setFixed(true);

                frame.getPersistentDataContainer().set(FluxCanvas.KEY, PersistentDataType.STRING, FluxCanvas.FLUX_SCREEN_FRAME_FLAG);

                frames.add(frame);

                MapView mapView = Bukkit.createMap(spawnLoc.getWorld());
                mapView.getRenderers().clear();
                mapView.setTrackingPosition(false);
                mapView.setUnlimitedTracking(false);

                DynamicMapRenderer mapRenderer = new DynamicMapRenderer();
                mapView.addRenderer(mapRenderer);

                renderers[x][y] = mapRenderer;
                mapViews[x][y] = mapView;

                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                MapMeta meta = (MapMeta) mapItem.getItemMeta();
                if (meta != null) {
                    meta.setMapView(mapView);
                    mapItem.setItemMeta(meta);
                }
                frame.setItem(mapItem);
            }
        }
    }

    public void updateParams(HashMap<String, Object> newParams) {
        synchronized (paramLock) {
            this.currentParams = newParams;
        }
    }

    boolean rendererInitialized = false;
    public void renderAsync() {

        if(destroyed) {
            destroyImpl();
            return;
        }

        HashMap<String, Object> paramsSnapshot;
        synchronized (paramLock) {
            paramsSnapshot = this.currentParams;
        }

        if(!rendererInitialized) initRenderer();

        this.uiLogic.setParams(paramsSnapshot);
        this.renderer.makeCurrent();
        byte[] rawPixels = this.renderer.render(uiLogic);

        if (rawPixels == null) return;

        int totalWidth = widthMulti * 128;
        int totalHeight = heightMulti * 128;
        int[][][] slicedBuffers = new int[widthMulti][heightMulti][128 * 128];

        for (int mapX = 0; mapX < widthMulti; mapX++) {
            for (int mapY = 0; mapY < heightMulti; mapY++) {
                int[] currentTile = slicedBuffers[mapX][mapY];

                for (int py = 0; py < 128; py++) {
                    for (int px = 0; px < 128; px++) {
                        int globalX = mapX * 128 + px;
                        int globalY = mapY * 128 + py;

                        int sourceY = totalHeight - 1 - globalY;
                        int index = (sourceY * totalWidth + globalX) * 4;

                        if (index >= 0 && index + 3 < rawPixels.length) {
                            byte r = rawPixels[index];
                            byte g = rawPixels[index + 1];
                            byte b = rawPixels[index + 2];
                            byte a = rawPixels[index + 3];

                            currentTile[py * 128 + px] = (a << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                        }
                    }
                }
            }
        }

        synchronized (bufferLock) {
            this.readyBuffers = slicedBuffers;
            this.hasNewFrame = true;
        }
    }

    public void uploadToMap() {
        int[][][] buffers = null;

        synchronized (bufferLock) {
            if (hasNewFrame) {
                buffers = readyBuffers;
                hasNewFrame = false;
            }
        }
        if (buffers == null) return;

        for (int x = 0; x < widthMulti; x++) {
            for (int y = 0; y < heightMulti; y++) {
                if (renderers[x][y] != null) {
                    renderers[x][y].setBuffer(buffers[x][y]);
                }
            }
        }

        broadcastUpdates();
    }

    private void broadcastUpdates() {
        double renderDistSq = 48 * 48;
        for (Player p : originLocation.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(originLocation) < renderDistSq) {
                for (MapView[] row : mapViews) {
                    for (MapView view : row) {
                        if (view != null) p.sendMap(view);
                    }
                }
            }
        }
    }


    public void initRenderer() {
        this.renderer.init();
        rendererInitialized = true;
    }

    public void destroyImpl() {
        Bukkit.getGlobalRegionScheduler().run(FluxCanvas.getInstance(), task -> {
            for (GlowItemFrame frame : frames)
                if (frame != null && frame.isValid())
                    frame.remove();
        });
        frames.clear();
        renderer.cleanup();
        clearLinks();
    }

    public void markDestroy() {
        this.destroyed = true;
    }

    private void cleanupOldFrames(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
            if (e instanceof GlowItemFrame && e.getLocation().getBlockX() == loc.getBlockX()
                    && e.getLocation().getBlockY() == loc.getBlockY()) {
                e.remove();
            }
        }
    }

    public void addLinkedContainer(Location loc) {
        this.tracker.addContainer(loc);
    }

    public void clearLinks() {
        this.tracker.clear();
    }

    public MultiContainerTracker getTracker() {
        return this.tracker;
    }

    public ScreenData toData() {
        ScreenData screenData = new ScreenData();
        screenData.id = this.id;
        screenData.direction = this.direction.name();
        screenData.x = this.originLocation.x();
        screenData.y = this.originLocation.y();
        screenData.z = this.originLocation.z();
        screenData.height = this.heightMulti;
        screenData.width = this.widthMulti;
        screenData.worldName = this.originLocation.getWorld().getName();
        screenData.ownerUniqueId = this.ownerUniqueId;
        screenData.windowWidth = this.renderer.getWidth();
        screenData.windowHeight = this.renderer.getHeight();
        var simpleLocList = new ArrayList<ScreenData.SimpleLoc>();
        for (Location containerLocation : this.tracker.getContainerLocations()) {
            var blockLoc = containerLocation.toBlockLocation();
            var simpleLoc = new ScreenData.SimpleLoc(
                    blockLoc.getWorld().getName(),
                    (int) blockLoc.x(), (int) blockLoc.y(), (int) blockLoc.z()
            );
            simpleLocList.add(simpleLoc);
        }
        screenData.linkedContainers = simpleLocList;
        return screenData;
    }

    public static FluxScreen toInstance(ScreenData data) {
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(data.worldName);
        if (world == null) {
            return null;
        }

        Location origin = new Location(world, data.x, data.y, data.z);
        BlockFace direction;
        try {
            direction = BlockFace.valueOf(data.direction);
        } catch (IllegalArgumentException e) {
            direction = BlockFace.NORTH;
        }

        CanvasUI newUiLogic = new CanvasUI();
        OffscreenRenderer newRenderer = new OffscreenRenderer(
                data.width * 128,
                data.height * 128
        );

//        Player player = Bukkit.get(data.ownerUniqueId);
        FluxScreen screen = new FluxScreen(
                data.id,
                origin,
                direction,
                data.width,
                data.height,
                newUiLogic,
                newRenderer,
                data.ownerUniqueId
        );

        screen.setOwnerUniqueId(UUID.fromString(data.ownerUniqueId));

        if (data.linkedContainers != null) {
            for (ScreenData.SimpleLoc simpleLoc : data.linkedContainers) {
                org.bukkit.World containerWorld = org.bukkit.Bukkit.getWorld(simpleLoc.world);
                if (containerWorld != null) {
                    Location loc = new Location(
                            containerWorld,
                            simpleLoc.x,
                            simpleLoc.y,
                            simpleLoc.z
                    );
                    screen.addLinkedContainer(loc);
                }
            }
        }

        screen.getTracker().tick();

        return screen;
    }
    public String getId() { return id; }
    public Location getOriginLocation() { return originLocation; }
    public BlockFace getDirection() { return direction; }
    public double getWidthMulti() { return widthMulti; }
    public int getHeightMulti() { return heightMulti; }

    public void updateInput(float screenX, float screenY, boolean click) {
        uiLogic.updateInput(screenX, screenY, click);
    }

    public void setClick() {
        uiLogic.setClick();
    }

}