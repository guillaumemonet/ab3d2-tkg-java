package com.ab3d2.core.level;

public class SpriteClip {
    private int clipId;
    private String name;
    private int frameCount;

    public SpriteClip(int id, String n, int count) {
        clipId=id;
        name=n;
        frameCount=count;
    }

    public int getClipId(){return clipId;}

    public String getName(){return name;}

    public int getFrameCount(){return frameCount;}
}