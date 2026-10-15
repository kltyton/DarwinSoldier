package com.kltyton.darwin_soldier.client.aim.spread;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpreadAimSelectorTest {

    @Test
    void zeroSpreadReturnsCenterOriginalAimPoint() {
        Vec3 origin = new Vec3(0.0, 0.0, 0.0);
        Vec3 aimPoint = new Vec3(10.0, 0.0, 0.0);
        Vec3 predictedTarget = new Vec3(8.0, 0.0, 0.0);
        AABB box = new AABB(7.8, -0.3, -0.3, 8.2, 0.3, 0.3);

        SpreadAimSelection selection = SpreadAimSelector.select(origin, aimPoint, predictedTarget, box, 0.0);

        assertEquals("center", selection.candidate);
        assertEquals(0, selection.candidateIndex);
        assertEquals(aimPoint, selection.aimPoint);
    }

    @Test
    void symmetricBoxSelectsCenter() {
        Vec3 origin = new Vec3(0.0, 0.0, 0.0);
        Vec3 predictedTarget = new Vec3(10.0, 0.0, 0.0);
        AABB box = new AABB(9.7, -0.3, -0.3, 10.3, 0.3, 0.3);

        SpreadAimSelection selection = SpreadAimSelector.select(origin, predictedTarget, predictedTarget, box, 10.0);

        assertEquals("center", selection.candidate);
        assertEquals(predictedTarget, selection.aimPoint);
    }

    @Test
    void boxShiftedRightPrefersRightSideCandidate() {
        Vec3 origin = new Vec3(0.0, 0.0, 0.0);
        Vec3 predictedTarget = new Vec3(10.0, 0.0, 0.0);
        AABB box = new AABB(9.7, -0.3, 0.15, 10.3, 0.3, 0.75);

        SpreadAimSelection selection = SpreadAimSelector.select(origin, predictedTarget, predictedTarget, box, 10.0);

        assertTrue(isOneOf(selection.candidate, "right", "right_up", "right_down"),
                "unexpected candidate: " + selection.candidate);
        assertNotEquals("center", selection.candidate);
    }

    @Test
    void boxShiftedUpPrefersUpSideCandidate() {
        Vec3 origin = new Vec3(0.0, 0.0, 0.0);
        Vec3 predictedTarget = new Vec3(10.0, 0.0, 0.0);
        AABB box = new AABB(9.7, 0.15, -0.3, 10.3, 0.75, 0.3);

        SpreadAimSelection selection = SpreadAimSelector.select(origin, predictedTarget, predictedTarget, box, 10.0);

        assertTrue(isOneOf(selection.candidate, "up", "left_up", "right_up"),
                "unexpected candidate: " + selection.candidate);
        assertNotEquals("center", selection.candidate);
    }

    @Test
    void extremeSpreadCorrectionStaysWithinCandidateStepBound() {
        Vec3 origin = new Vec3(0.0, 0.0, 0.0);
        Vec3 predictedTarget = new Vec3(10.0, 0.0, 0.0);
        AABB box = new AABB(9.8, -0.2, -0.2, 10.2, 0.2, 0.2);
        double spreadDegrees = 45.0;

        double radius = Math.tan(Math.toRadians(spreadDegrees)) * origin.distanceTo(predictedTarget);
        double step = Math.min(radius * 0.5, Math.max(0.05, Math.min(box.getXsize(), box.getYsize()) * 0.35));

        SpreadAimSelection selection = SpreadAimSelector.select(origin, predictedTarget, predictedTarget, box, spreadDegrees);

        assertTrue(selection.aimPoint.distanceTo(predictedTarget) <= Math.sqrt(2.0) * step + 1.0E-6,
                "aim point deviated " + selection.aimPoint.distanceTo(predictedTarget)
                        + " beyond bound " + (Math.sqrt(2.0) * step));
    }

    @Test
    void repeatedCallsProduceEqualSelections() {
        Vec3 origin = new Vec3(0.0, 0.0, 0.0);
        Vec3 aimPoint = new Vec3(12.0, 1.0, -1.0);
        Vec3 predictedTarget = new Vec3(10.0, 0.0, 0.0);
        AABB box = new AABB(9.7, -0.3, -0.3, 10.3, 0.3, 0.3);

        SpreadAimSelection first = SpreadAimSelector.select(origin, aimPoint, predictedTarget, box, 8.0);
        SpreadAimSelection second = SpreadAimSelector.select(origin, aimPoint, predictedTarget, box, 8.0);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void correctionPreservesBallisticAimPointInsteadOfReplacingItWithTargetPoint() {
        Vec3 origin = Vec3.ZERO;
        Vec3 aimPoint = new Vec3(10.0, 2.0, 0.0);
        Vec3 predictedTarget = new Vec3(10.0, 0.0, 0.0);
        AABB box = new AABB(9.7, -0.3, -0.3, 10.3, 0.3, 0.3);

        SpreadAimSelection selection = SpreadAimSelector.select(
                origin, aimPoint, predictedTarget, box, 5.0);

        assertTrue(selection.aimPoint.y > 1.5,
                "spread correction must retain the solver's gravity compensation");
    }

    @Test
    void constantsMatchSpecification() {
        assertEquals(9, SpreadAimSelector.CANDIDATE_COUNT);
        assertEquals(9, SpreadAimSelector.SAMPLE_COUNT);
    }

    private static boolean isOneOf(String candidate, String... accepted) {
        for (String name : accepted) {
            if (name.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
