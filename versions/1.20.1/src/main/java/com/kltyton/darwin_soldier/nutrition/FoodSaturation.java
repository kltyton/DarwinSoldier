package com.kltyton.darwin_soldier.nutrition;

final class FoodSaturation {
    private FoodSaturation() {
    }

    static double base(int food, float value) {
        return (double) food * value * 2.0D;
    }
}
