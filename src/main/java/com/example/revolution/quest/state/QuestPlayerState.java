package com.example.revolution.quest.state;

import java.util.ArrayList;
import java.util.List;

/** Permanent Quest state; deliberately independent from run/Mastery state. */
public final class QuestPlayerState {
    public final List<QuestEntry> boardEntries = new ArrayList<>();
    public long batchCooldownUntil;
    public long completedQuestCount;
    public String rerollDayId;
    public int rerollsUsed;

    public QuestPlayerState copy() {
        QuestPlayerState copy = new QuestPlayerState();
        for (QuestEntry entry : boardEntries) copy.boardEntries.add(entry.copy());
        copy.batchCooldownUntil = batchCooldownUntil;
        copy.completedQuestCount = completedQuestCount;
        copy.rerollDayId = rerollDayId;
        copy.rerollsUsed = rerollsUsed;
        return copy;
    }
}
