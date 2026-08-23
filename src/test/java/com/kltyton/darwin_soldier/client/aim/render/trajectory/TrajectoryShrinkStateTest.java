package com.kltyton.darwin_soldier.client.aim.render.trajectory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrajectoryShrinkStateTest {
    private static final String WEAPON_A = "minecraft:bow";
    private static final String WEAPON_B = "minecraft:crossbow";
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long SHRINK_DURATION_NANOS = (long) (1.5D * NANOS_PER_SECOND);

    @Test
    void freshStateStartsAtFullSize() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        assertEquals(1.0D, state.update(WEAPON_A, true, 1_000L), 1.0e-12D);
    }

    @Test
    void reachesTargetScaleAfterOneAndAHalfSeconds() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        double scale = state.update(WEAPON_A, true, start + SHRINK_DURATION_NANOS);
        assertEquals(TrajectoryShrinkState.TARGET_SCALE, scale, 1.0e-9D);
    }

    @Test
    void shrinkProgressDependsOnElapsedTimeNotFrameCount() {
        TrajectoryShrinkState fastFrames = new TrajectoryShrinkState();
        long fastStart = 10_000L;
        fastFrames.update(WEAPON_A, true, fastStart);
        for (int frame = 1; frame <= 60; frame++) {
            fastFrames.update(WEAPON_A, true, fastStart + frame * 25_000_000L);
        }
        assertEquals(TrajectoryShrinkState.TARGET_SCALE,
                fastFrames.update(WEAPON_A, true, fastStart + 60L * 25_000_000L), 1.0e-9D);

        TrajectoryShrinkState slowFrames = new TrajectoryShrinkState();
        long slowStart = 100_000L;
        slowFrames.update(WEAPON_A, true, slowStart);
        for (int frame = 1; frame <= 15; frame++) {
            slowFrames.update(WEAPON_A, true, slowStart + frame * 100_000_000L);
        }
        assertEquals(TrajectoryShrinkState.TARGET_SCALE,
                slowFrames.update(WEAPON_A, true, slowStart + 15L * 100_000_000L), 1.0e-9D);
    }

    @Test
    void shrinksBothMarkerKindsToThirtyFivePercent() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        double scale = state.update(WEAPON_A, true, start + SHRINK_DURATION_NANOS);
        assertEquals(0.035D, 0.10D * scale, 1.0e-9D);
        assertEquals(0.070D, 0.20D * scale, 1.0e-9D);
    }

    @Test
    void progressionIsContinuousAndStrictlyMonotonic() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        double previous = state.update(WEAPON_A, true, start + 50_000_000L);
        assertTrue(previous < 1.0D);
        for (long elapsed = 100_000_000L; elapsed <= SHRINK_DURATION_NANOS; elapsed += 50_000_000L) {
            double next = state.update(WEAPON_A, true, start + elapsed);
            assertTrue(next < previous, "scale must strictly decrease at " + elapsed + " ns");
            previous = next;
        }
        assertEquals(TrajectoryShrinkState.TARGET_SCALE, previous, 1.0e-9D);
    }

    @Test
    void usesSmoothstepMidpointAtZeroPointSevenFiveSeconds() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        double scale = state.update(WEAPON_A, true, start + 750_000_000L);
        assertEquals(0.675D, scale, 1.0e-9D);
    }

    @Test
    void sameTimestampRepeatedCausesNoProgress() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        assertEquals(1.0D, state.update(WEAPON_A, true, start), 1.0e-12D);
        double atHalf = state.update(WEAPON_A, true, start + 750_000_000L);
        assertEquals(atHalf, state.update(WEAPON_A, true, start + 750_000_000L), 1.0e-12D);
    }

    @Test
    void resetsImmediatelyWhenPreviewIsNotEligible() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        state.update(WEAPON_A, true, start + 750_000_000L);
        assertEquals(1.0D, state.update(WEAPON_A, false, start + 800_000_000L), 1.0e-12D);
        assertEquals(1.0D, state.update(WEAPON_A, false, start + 900_000_000L), 1.0e-12D);
    }

    @Test
    void weaponIdentityChangeResetsTheShrink() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        state.update(WEAPON_A, true, start + 750_000_000L);
        assertEquals(1.0D, state.update(WEAPON_B, true, start + 800_000_000L), 1.0e-12D);
    }

    @Test
    void nullWeaponIdentityIsNotEligibleForTracking() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        assertEquals(1.0D, state.update(null, true, 1_000L), 1.0e-12D);
        state.update(WEAPON_A, true, 1_000L);
        state.update(WEAPON_A, true, 1_000L + 750_000_000L);
        assertEquals(1.0D, state.update(null, true, 1_000L + 800_000_000L), 1.0e-12D);
    }

    @Test
    void remainsAtTargetScaleOnceFullyShrunk() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        state.update(WEAPON_A, true, start + 2L * NANOS_PER_SECOND);
        double later = state.update(WEAPON_A, true, start + 3L * NANOS_PER_SECOND);
        assertEquals(TrajectoryShrinkState.TARGET_SCALE, later, 1.0e-9D);
        assertEquals(later, state.update(WEAPON_A, true, start + 3L * NANOS_PER_SECOND), 1.0e-12D);
    }

    @Test
    void rearmingAfterResetShrinksAgainFromFullSize() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        state.update(WEAPON_A, true, start + 750_000_000L);
        long rearm = 2L * NANOS_PER_SECOND;
        state.update(WEAPON_A, false, rearm);
        assertEquals(1.0D, state.update(WEAPON_A, true, rearm), 1.0e-12D);
        double first = state.update(WEAPON_A, true, rearm + 100_000_000L);
        double second = state.update(WEAPON_A, true, rearm + 200_000_000L);
        assertTrue(first < 1.0D);
        assertTrue(second < first);
        assertEquals(second, state.update(WEAPON_A, true, rearm + 200_000_000L), 1.0e-12D);
    }

    @Test
    void eligibilityCanReachFullShrinkInOneContinuousRun() {
        TrajectoryShrinkState state = new TrajectoryShrinkState();
        long start = 1_000L;
        state.update(WEAPON_A, true, start);
        assertTrue(state.update(WEAPON_A, true, start + SHRINK_DURATION_NANOS) < 1.0D);
    }
}
