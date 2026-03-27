package com.ab3d2.game;

public class Player {

    private float x, y, z;
    private float angle;

    private int health = 100;
    private int armor = 0;
    private int ammo = 50;
    private int currentWeapon = 0;

    public void init(Level level) {
        // plus tard : spawn point depuis le niveau
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.angle = 0;
    }

    public void update(double dt, InputAdapter input, Level level) {

        // Rotation caméra
        angle += input.lookDX * 0.002f;

        float speed = 4.0f;

        float dx = 0;
        float dy = 0;

        if (input.forward) {
            dy += speed * dt;
        }
        if (input.backward) {
            dy -= speed * dt;
        }
        if (input.left) {
            dx -= speed * dt;
        }
        if (input.right) {
            dx += speed * dt;
        }

        // plus tard : collision avec le niveau
        x += dx;
        y += dy;

        if (input.fire) {
            // plus tard : tir
        }
    }

    public int getHealth() {
        return health;
    }

    public int getArmor() {
        return armor;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getCurrentWeapon() {
        return currentWeapon;
    }
}
