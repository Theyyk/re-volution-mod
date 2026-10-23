package com.example.revolution.location.service;

import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;

public final class StageTransitionTriggerDefinitionTest {
    public static void main(String[] args) {
        cylindricalTriggerUsesHorizontalRadiusAndVerticalHalfHeight();
        messagesRemainDataAndMayBeUnset();
        displayModeIsConfigurable();
        invalidGeometryFailsClosed();
        System.out.println("v1.3.9 stage transition trigger definition regression checks passed");
    }

    private static void cylindricalTriggerUsesHorizontalRadiusAndVerticalHalfHeight() {
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                1, 2, 10.0D, 64.0D, -5.0D, 3.0D, 2.0D,
                DisplayMode.ACTION_BAR, null, null);
        check(trigger.contains(13.0D, 64.0D, -5.0D), "Radius boundary must count as inside");
        check(trigger.contains(10.0D, 66.0D, -5.0D), "Vertical boundary must count as inside");
        check(!trigger.contains(13.01D, 64.0D, -5.0D), "Outside horizontal radius must be rejected");
        check(!trigger.contains(10.0D, 66.01D, -5.0D), "Outside vertical half-height must be rejected");
    }

    private static void messagesRemainDataAndMayBeUnset() {
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                1, 2, 0.0D, 70.0D, 0.0D, 2.0D, 3.0D,
                DisplayMode.ACTION_BAR, null, null);
        check(trigger.getLockedMessage() == null, "Locked message must not have a hardcoded default");
        check(trigger.getEnteringMessage() == null, "Entering message must not have a hardcoded default");
        StageTransitionTriggerDefinition updated = trigger.withMessages("locked text", "entering text");
        check("locked text".equals(updated.getLockedMessage()), "Locked message must be stored as configured data");
        check("entering text".equals(updated.getEnteringMessage()), "Entering message must be stored as configured data");
    }

    private static void displayModeIsConfigurable() {
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                2, 4, 1.0D, 2.0D, 3.0D, 1.0D, 1.0D,
                DisplayMode.ACTION_BAR, "a", "b");
        StageTransitionTriggerDefinition chat = trigger.withDisplayMode(DisplayMode.CHAT);
        check(chat.getDisplayMode() == DisplayMode.CHAT, "Display mode must be replaceable without changing messages");
        check("a".equals(chat.getLockedMessage()) && "b".equals(chat.getEnteringMessage()),
                "Changing display mode must retain configured message data");
    }

    private static void invalidGeometryFailsClosed() {
        boolean failed = false;
        try {
            new StageTransitionTriggerDefinition(
                    1, 1, 0.0D, 64.0D, 0.0D, 0.0D, 1.0D,
                    DisplayMode.ACTION_BAR, null, null);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        check(failed, "Zero-radius trigger must be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
