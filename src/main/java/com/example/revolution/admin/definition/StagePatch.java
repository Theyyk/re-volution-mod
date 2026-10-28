package com.example.revolution.admin.definition;

/** Null fields are deliberately absent changes, never zero/default replacements. */
public final class StagePatch {
    public final int location, stage;
    public final String hp, resource, amount;
    public final Integer population;
    public StagePatch(int location, int stage, String hp, Integer population, String resource, String amount) {
        this.location = location; this.stage = stage; this.hp = hp; this.population = population;
        this.resource = resource; this.amount = amount;
    }
}
