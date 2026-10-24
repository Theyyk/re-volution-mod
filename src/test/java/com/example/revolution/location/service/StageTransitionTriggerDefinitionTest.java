package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;

public final class StageTransitionTriggerDefinitionTest {
    public static void main(String[] args) {
        boundaryRingStartsOutsideMobZone();
        ringIsSplitIntoExactForwardBackwardHalves();
        messagesRemainDataAndMayBeUnset();
        displayModeIsConfigurable();
        invalidGeometryFailsClosed();
        System.out.println("v1.3.9 stage boundary trigger regression checks passed");
    }

    private static void boundaryRingStartsOutsideMobZone() {
        LocationDefinition source = new LocationDefinition(1000000101, "stage", 10.0D, -5.0D, 8.0D);
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                1, 1, 4.0D, DisplayMode.ACTION_BAR, null, null, null);
        check(!trigger.containsBoundaryRing(source, 18.0D, -5.0D),
                "Mob-zone radius itself must not count as trigger ring");
        check(trigger.containsBoundaryRing(source, 20.0D, -5.0D),
                "Position inside outer width must count as trigger ring");
        check(trigger.containsBoundaryRing(source, 22.0D, -5.0D),
                "Outer boundary must count as trigger ring");
        check(!trigger.containsBoundaryRing(source, 22.01D, -5.0D),
                "Position beyond outer ring must be rejected");
    }

    private static void ringIsSplitIntoExactForwardBackwardHalves() {
        LocationDefinition source = new LocationDefinition(1000000101, "stage", 0.0D, 0.0D, 8.0D);
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                1, 1, 4.0D, DisplayMode.ACTION_BAR, null, null, null);
        check(trigger.isForwardHalf(source, 10.0D, 0.0D, 1.0D, 0.0D),
                "Half facing next stage must be forward");
        check(!trigger.isForwardHalf(source, -10.0D, 0.0D, 1.0D, 0.0D),
                "Opposite 180 degrees must be backward");
        check(trigger.isForwardHalf(source, 0.0D, 10.0D, 1.0D, 0.0D),
                "Exact divider belongs deterministically to forward half");
    }

    private static void messagesRemainDataAndMayBeUnset() {
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                1, 2, 3.0D, DisplayMode.ACTION_BAR, null, null, null);
        check(trigger.getLockedMessage() == null, "Locked message must not have a hardcoded default");
        check(trigger.getForwardMessage() == null, "Forward message must not have a hardcoded default");
        check(trigger.getBackwardMessage() == null, "Backward message must not have a hardcoded default");
        StageTransitionTriggerDefinition updated = trigger.withMessages("locked", "forward", "backward");
        check("locked".equals(updated.getLockedMessage()), "Locked message must remain configured data");
        check("forward".equals(updated.getForwardMessage()), "Forward message must remain configured data");
        check("backward".equals(updated.getBackwardMessage()), "Backward message must remain configured data");
    }

    private static void displayModeIsConfigurable() {
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                2, 4, 2.0D, DisplayMode.ACTION_BAR, "a", "b", "c");
        StageTransitionTriggerDefinition chat = trigger.withDisplayMode(DisplayMode.CHAT);
        check(chat.getDisplayMode() == DisplayMode.CHAT, "Display mode must be replaceable");
        check("a".equals(chat.getLockedMessage())
                        && "b".equals(chat.getForwardMessage())
                        && "c".equals(chat.getBackwardMessage()),
                "Changing display mode must retain all configured messages");
    }

    private static void invalidGeometryFailsClosed() {
        boolean failed = false;
        try {
            new StageTransitionTriggerDefinition(
                    1, 1, 0.0D, DisplayMode.ACTION_BAR, null, null, null);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        check(failed, "Zero-width trigger ring must be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
