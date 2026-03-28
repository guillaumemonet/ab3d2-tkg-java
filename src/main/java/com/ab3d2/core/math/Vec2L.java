// Vec2L.java
// Represents a 32-bit XZ vector

public class Vec2L {
    // Coordinate values
    private float x;
    private float z;

    // Constructor
    public Vec2L(float x, float z) {
        this.x = x;
        this.z = z;
    }

    // Getters
    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    // Setters
    public void setX(float x) {
        this.x = x;
    }

    public void setZ(float z) {
        this.z = z;
    }

    // Method to add another Vec2L
    public Vec2L add(Vec2L other) {
        return new Vec2L(this.x + other.x, this.z + other.z);
    }

    // Method to subtract another Vec2L
    public Vec2L subtract(Vec2L other) {
        return new Vec2L(this.x - other.x, this.z - other.z);
    }

    // Method to scale the vector
    public Vec2L scale(float scalar) {
        return new Vec2L(this.x * scalar, this.z * scalar);
    }

    // String representation
    @Override
    public String toString() {
        return "Vec2L{" + "x=" + x + ", z=" + z + '}';
    }
}