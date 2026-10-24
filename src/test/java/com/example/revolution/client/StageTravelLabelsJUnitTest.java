package com.example.revolution.client;

import com.example.revolution.network.StageTravelMenuPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class StageTravelLabelsJUnitTest {
    @Test
    void normalStageUsesMobSlotLabel() {
        StageTravelMenuPacket.Entry entry = new StageTravelMenuPacket.Entry(1, 3, true, true, false);
        assertEquals("Моб 3", StageTravelLabels.stageLabel(entry));
    }

    @Test
    void bossStageUsesBossLabel() {
        StageTravelMenuPacket.Entry entry = new StageTravelMenuPacket.Entry(1, 3, true, true, true);
        assertEquals("Босс", StageTravelLabels.stageLabel(entry));
    }

    @Test
    void removingBossStatusReturnsNormalLabel() {
        StageTravelMenuPacket.Entry boss = new StageTravelMenuPacket.Entry(1, 3, true, true, true);
        StageTravelMenuPacket.Entry normal = new StageTravelMenuPacket.Entry(1, 3, true, true, false);
        assertEquals("Босс", StageTravelLabels.stageLabel(boss));
        assertEquals("Моб 3", StageTravelLabels.stageLabel(normal));
    }

    @Test
    void availabilitySuffixesRemainDynamic() {
        assertEquals("Босс - закрыт", StageTravelLabels.stageLabel(
                new StageTravelMenuPacket.Entry(1, 3, false, true, true)));
        assertEquals("Моб 2 - нет точки", StageTravelLabels.stageLabel(
                new StageTravelMenuPacket.Entry(1, 2, true, false, false)));
    }
}
