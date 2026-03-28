import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CollisionMapParser {

    private static final int GRID_SIZE = 100;
    private final boolean[][] collisionGrid = new boolean[GRID_SIZE][GRID_SIZE];

    public CollisionMapParser(byte[] binaryData) {
        parseCollisionMap(binaryData);
    }

    private void parseCollisionMap(byte[] binaryData) {
        ByteBuffer buffer = ByteBuffer.wrap(binaryData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                collisionGrid[i][j] = (buffer.get() != 0);
            }
        }
    }

    public boolean isCollision(int x, int y) {
        if (x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds");
        }
        return collisionGrid[x][y];
    }
}