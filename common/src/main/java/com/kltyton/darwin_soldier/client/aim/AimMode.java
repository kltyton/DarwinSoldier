package com.kltyton.darwin_soldier.client.aim;

import java.util.Locale;

public enum AimMode {
    MANUAL,
    ADAPTIVE;

    public static AimMode parse(String value) {
        if (value == null) {
            return MANUAL;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return MANUAL;
        }
    }
}
