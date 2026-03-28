package com.ab3d2.core.level;


import java.util.List;

public class Zone {
    private int zoneId;
    private float floor;
    private float roof;
    private int brightness;
    private List<ZEdge> edges;

    public Zone(int zoneId, float floor, float roof, int brightness, List<ZEdge> edges) {
        this.zoneId = zoneId;
        this.floor = floor;
        this.roof = roof;
        this.brightness = brightness;
        this.edges = edges;
    }

    public int getZoneId() {
        return zoneId;
    }

    public float getFloor() {
        return floor;
    }

    public float getRoof() {
        return roof;
    }

    public int getBrightness() {
        return brightness;
    }

    public List<ZEdge> getEdges() {
        return edges;
    }
}