package com.example.revolution.achievement;

/** Small shared formatter for functional UI/chat labels. Supports arbitrary positive stage indexes. */
public final class AchievementStageLabels {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private AchievementStageLabels() {}

    public static String roman(int stageIndex) {
        if (stageIndex <= 0) return "";
        int value = stageIndex;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (value >= VALUES[i]) {
                out.append(ROMAN[i]);
                value -= VALUES[i];
            }
        }
        return out.toString();
    }
}
