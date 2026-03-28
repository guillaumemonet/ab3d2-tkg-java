package com.ab3d2.core.level;


import com.ab3d2.core.level.Position;

public class ZEdge {
    private Position position;
    private double length;
    private String joinZoneId;
    private int flags;

    public ZEdge(Position position, double length, String joinZoneId, int flags) {
        this.position = position;
        this.length = length;
        this.joinZoneId = joinZoneId;
        this.flags = flags;
    }

    // Getters and setters
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getJoinZoneId() {
        return joinZoneId;
    }

    public void setJoinZoneId(String joinZoneId) {
        this.joinZoneId = joinZoneId;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }
}