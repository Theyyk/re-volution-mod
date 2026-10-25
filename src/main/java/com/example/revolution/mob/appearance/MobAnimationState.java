package com.example.revolution.mob.appearance;

/** Visual-only animation states supported by managed mobs. */
public enum MobAnimationState {
    IDLE("idle"),
    WALK("walk"),
    SPAWN("spawn"),
    HURT("hurt"),
    DEATH("death");

    private final String id;

    MobAnimationState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
