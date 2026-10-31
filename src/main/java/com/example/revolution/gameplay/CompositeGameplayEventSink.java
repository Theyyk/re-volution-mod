package com.example.revolution.gameplay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Fan-out adapter so factual events can be reused by Mastery, Quests and future Achievements. */
public final class CompositeGameplayEventSink implements GameplayEventSink {
    private final List<GameplayEventSink> sinks = new ArrayList<>();

    public CompositeGameplayEventSink(GameplayEventSink... sinks) {
        if (sinks != null) for (GameplayEventSink sink : Arrays.asList(sinks)) if (sink != null) this.sinks.add(sink);
    }

    @Override public void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount) {
        for (GameplayEventSink sink : sinks) sink.confirmedDamage(playerUuid, source, amount);
    }

    @Override public void earnedResource(UUID playerUuid, String resource, BigDecimal amount) {
        for (GameplayEventSink sink : sinks) sink.earnedResource(playerUuid, resource, amount);
    }

    @Override public void factualEvent(GameplayEvent event) {
        for (GameplayEventSink sink : sinks) sink.factualEvent(event);
    }
}
