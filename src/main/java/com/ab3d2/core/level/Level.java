package com.ab3d2.core.level;

import java.util.List;

public class Level {
    private String levelLetter;
    private List<String> zones;
    private String walkMap;
    private String flyMap;
    private int frameCount;
    private String name;

    // Getters and Setters
    public String getLevelLetter() {
        return levelLetter;
    }

    public void setLevelLetter(String levelLetter) {
        this.levelLetter = levelLetter;
    }

    public List<String> getZones() {
        return zones;
    }

    public void setZones(List<String> zones) {
        this.zones = zones;
    }

    public String getWalkMap() {
        return walkMap;
    }

    public void setWalkMap(String walkMap) {
        this.walkMap = walkMap;
    }

    public String getFlyMap() {
        return flyMap;
    }

    public void setFlyMap(String flyMap) {
        this.flyMap = flyMap;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}