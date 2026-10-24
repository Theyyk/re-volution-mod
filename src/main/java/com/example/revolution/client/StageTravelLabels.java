package com.example.revolution.client;

import com.example.revolution.network.StageTravelMenuPacket;

/** Pure label mapping for the server-authoritative stage travel snapshot. */
public final class StageTravelLabels {
    private StageTravelLabels() {}

    public static String stageLabel(StageTravelMenuPacket.Entry entry) {
        if (entry == null) throw new IllegalArgumentException("entry is required");
        String label = entry.isBoss() ? "Босс" : "Моб " + entry.getMobSlotId();
        if (!entry.isUnlocked()) label += " - закрыт";
        else if (!entry.hasTransitionPoint()) label += " - нет точки";
        return label;
    }
}
