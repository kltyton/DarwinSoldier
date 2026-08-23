package com.kltyton.darwin_soldier.combat.adaptation;

public final class AdaptationRules {
    private AdaptationRules() {
    }

    public static int nextLevel(int currentLevel, int maximumLevel) {
        return clampLevel((long) currentLevel + 1L, maximumLevel);
    }

    public static int clampLevel(long level, int maximumLevel) {
        int maximum = Math.max(0, maximumLevel);
        return (int) Math.max(0L, Math.min(maximum, level));
    }

    public static double reductionForLevel(int level, int maximumLevel,
                                           double levelOne, double levelTwo, double levelThree,
                                           double levelFour, double levelFive) {
        return switch (clampLevel(level, maximumLevel)) {
            case 1 -> levelOne;
            case 2 -> levelTwo;
            case 3 -> levelThree;
            case 4 -> levelFour;
            case 5 -> levelFive;
            default -> 0.0D;
        };
    }
}
