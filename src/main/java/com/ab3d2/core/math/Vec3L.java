package com.ab3d2.core.math;

/**
 * Represents a 32-bit vector with X, Y, and Z components.
 */
public class Vec3L {
    private final float x;
    private final float y;
    private final float z;

    public Vec3L(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "Vec3L{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}