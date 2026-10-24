package com.example.revolution.location.definition;

/** Domain kind of a configured Location geometry entry. */
public enum LocationKind {
    WORLD,
    MOB_ZONE,
    BOSS_ZONE;

    public boolean isMobStageZone() {
        return this == MOB_ZONE || this == BOSS_ZONE;
    }
}
