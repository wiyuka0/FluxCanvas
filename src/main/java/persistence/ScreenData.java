package persistence;

import java.util.List;

public class ScreenData {
    public String id;

    public String worldName;
    public double x, y, z;
    public String direction; // BlockFace.name()
    public int width;
    public int height;

    public int windowWidth;
    public int windowHeight;

    public String ownerUniqueId;

    public List<SimpleLoc> linkedContainers;

    public static class SimpleLoc {
        public String world;
        public int x, y, z;

        public SimpleLoc(String world, int x, int y, int z) {
            this.world = world; this.x = x; this.y = y; this.z = z;
        }
    }
}