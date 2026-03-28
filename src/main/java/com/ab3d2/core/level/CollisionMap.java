public class CollisionMap {
    private boolean[][] mapData;
    private int width;
    private int height;

    public CollisionMap(int width, int height) {
        this.width = width;
        this.height = height;
        mapData = new boolean[width][height]; // Assuming it's a 2D map for walkability
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
        return mapData[x][y];
    }

    public int getCollisionFlag(int x, int y) {
        // Placeholder for actual collision flag logic
        return isWalkable(x, y) ? 0 : 1; // 0 = walkable, 1 = not walkable
    }

    public boolean[][] getMapData() {
        return mapData;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}