package com.kltyton.darwin_soldier.diagnostic;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.config.DarwinConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class RuntimeDiagnostics {
    private static final String SESSION_ID = Long.toUnsignedString(System.currentTimeMillis(), 36);
    private static final Map<String, Long> LAST_LOG_NANOS = new ConcurrentHashMap<>();

    private RuntimeDiagnostics() {
    }

    public static void info(String event, String detail) {
        if (isEnabled()) {
            Darwin_soldier.LOGGER.info("[DS-DIAG session={} event={}] {}", SESSION_ID, event, detail);
        }
    }

    public static void warn(String event, String detail) {
        if (isEnabled()) {
            Darwin_soldier.LOGGER.warn("[DS-DIAG session={} event={}] {}", SESSION_ID, event, detail);
        }
    }

    public static void error(String event, String detail, Throwable throwable) {
        Darwin_soldier.LOGGER.error("[DS-DIAG session={} event={}] {}", SESSION_ID, event, detail, throwable);
    }

    public static void infoRateLimited(String key, long intervalMillis, String event, Supplier<String> detail) {
        if (!isEnabled()) {
            return;
        }
        long now = System.nanoTime();
        long intervalNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, intervalMillis));
        Long previous = LAST_LOG_NANOS.putIfAbsent(key, now);
        if (previous != null) {
            if (now - previous < intervalNanos || !LAST_LOG_NANOS.replace(key, previous, now)) {
                return;
            }
        }
        info(event, detail.get());
    }

    public static String sessionId() {
        return SESSION_ID;
    }

    private static boolean isEnabled() {
        try {
            return DarwinConfig.RUNTIME_DIAGNOSTICS_ENABLED.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }
}
