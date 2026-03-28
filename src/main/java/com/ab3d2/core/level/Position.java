package com.ab3d2.core.level;

public class Position {
    private double x;
    private double z;

    public Position(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double distance(Position other) {
        double dx = this.x - other.x;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Position add(Position other) {
        return new Position(this.x + other.x, this.z + other.z);
    }

    public Position subtract(Position other) {
        return new Position(this.x - other.x, this.z - other.z);
    }

    public Position scale(double factor) {
        return new Position(this.x * factor, this.z * factor);
    }
}