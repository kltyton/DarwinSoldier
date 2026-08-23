package com.kltyton.darwin_soldier.client.aim.control;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetSwitchSelectorTest {

    private static final AimDirection CURSOR = AimDirection.of(1.0D, 0.0D, 0.0D);

    private static TargetCandidate candidate(int id, double x, double y, double z) {
        return new TargetCandidate(id, AimDirection.of(x, y, z));
    }

    @Test
    void selectsNearestCandidateWithinRange() {
        List<TargetCandidate> candidates = List.of(
                candidate(1, 0.5D, 0.8660254D, 0.0D),
                candidate(2, 0.8660254D, 0.5D, 0.0D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 1, CURSOR, 45.0D);

        assertTrue(selected.isPresent());
        assertEquals(2, selected.get().id());
    }

    @Test
    void includesCurrentCandidateAndPrefersItWhenNearest() {
        List<TargetCandidate> candidates = List.of(
                candidate(1, 0.9848078D, 0.0D, 0.1736482D),
                candidate(2, 0.9396926D, 0.0D, 0.3420201D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 1, CURSOR, 30.0D);

        assertTrue(selected.isPresent());
        assertEquals(1, selected.get().id());
    }

    @Test
    void returnsEmptyWhenNearestCandidateIsOutsideRange() {
        List<TargetCandidate> candidates = List.of(candidate(1, 0.5D, 0.8660254D, 0.0D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 1, CURSOR, 30.0D);

        assertFalse(selected.isPresent());
    }

    @Test
    void candidateAtExactRangeBoundaryIsSelected() {
        List<TargetCandidate> candidates = List.of(candidate(
                1,
                Math.cos(Math.toRadians(30.0D)),
                Math.sin(Math.toRadians(30.0D)),
                0.0D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 1, CURSOR, 30.0D);

        assertTrue(selected.isPresent());
        assertEquals(1, selected.get().id());
    }

    @Test
    void tiePrefersCurrentCandidateOverOtherNearest() {
        List<TargetCandidate> candidates = List.of(
                candidate(5, 0.8660254D, 0.5D, 0.0D),
                candidate(9, 0.8660254D, -0.5D, 0.0D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 5, CURSOR, 45.0D);

        assertTrue(selected.isPresent());
        assertEquals(5, selected.get().id());
    }

    @Test
    void tieWithoutCurrentPrefersLowestIdDeterministically() {
        List<TargetCandidate> candidates = List.of(
                candidate(1, 0.6427876D, 0.0D, 0.7660444D),
                candidate(7, 0.8660254D, 0.5D, 0.0D),
                candidate(3, 0.8660254D, -0.5D, 0.0D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 1, CURSOR, 45.0D);

        assertTrue(selected.isPresent());
        assertEquals(3, selected.get().id());
    }

    @Test
    void currentOutsideRangeDoesNotBlockNearCandidate() {
        List<TargetCandidate> candidates = List.of(
                candidate(1, 0.6427876D, 0.0D, 0.7660444D),
                candidate(2, 0.9848078D, 0.0D, 0.1736482D));

        Optional<TargetCandidate> selected = TargetSwitchSelector.select(candidates, 1, CURSOR, 20.0D);

        assertTrue(selected.isPresent());
        assertEquals(2, selected.get().id());
    }

    @Test
    void emptyOrNullInputsReturnEmpty() {
        assertFalse(TargetSwitchSelector.select(List.of(), 1, CURSOR, 45.0D).isPresent());
        assertFalse(TargetSwitchSelector.select(null, 1, CURSOR, 45.0D).isPresent());
        assertFalse(TargetSwitchSelector.select(
                List.of(candidate(1, 1.0D, 0.0D, 0.0D)), 1, null, 45.0D).isPresent());
    }

    @Test
    void nonPositiveOrNonFiniteRangeReturnsEmpty() {
        List<TargetCandidate> candidates = List.of(candidate(1, 1.0D, 0.0D, 0.0D));
        assertFalse(TargetSwitchSelector.select(candidates, 1, CURSOR, 0.0D).isPresent());
        assertFalse(TargetSwitchSelector.select(candidates, 1, CURSOR, -5.0D).isPresent());
        assertFalse(TargetSwitchSelector.select(candidates, 1, CURSOR, Double.NaN).isPresent());
        assertFalse(TargetSwitchSelector.select(candidates, 1, CURSOR, Double.POSITIVE_INFINITY).isPresent());
    }
}
