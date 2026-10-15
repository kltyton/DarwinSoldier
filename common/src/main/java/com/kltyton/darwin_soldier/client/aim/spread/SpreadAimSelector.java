package com.kltyton.darwin_soldier.client.aim.spread;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpreadAimSelector {

    public static final int CANDIDATE_COUNT = 9;
    public static final int SAMPLE_COUNT = 9;

    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
    private static final double MIN_DISTANCE = 1.0E-6;
    private static final double EPSILON = 1.0E-6;
    private static final double PARALLEL_EPSILON = 1.0E-6;

    private static final String[] CANDIDATE_NAMES = {
            "center", "left", "right", "up", "down",
            "left_up", "right_up", "left_down", "right_down"
    };
    private static final int[] SAMPLE_WEIGHTS = {4, 2, 2, 2, 2, 1, 1, 1, 1};

    private SpreadAimSelector() {
    }

    public static SpreadAimSelection select(Vec3 origin, Vec3 aimPoint, Vec3 predictedTarget, AABB predictedTargetBox, double spreadDegrees) {
        if (origin == null || aimPoint == null || predictedTarget == null || predictedTargetBox == null
                || !(spreadDegrees > 0.0)
                || origin.distanceToSqr(predictedTarget) < MIN_DISTANCE * MIN_DISTANCE) {
            return SpreadAimSelection.center(aimPoint);
        }

        Vec3 forward = aimPoint.subtract(origin).normalize();
        Vec3 right = forward.cross(UP);
        if (right.lengthSqr() < PARALLEL_EPSILON) {
            right = forward.cross(X_AXIS);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double distance = origin.distanceTo(predictedTarget);
        double spreadRadius = Math.tan(Math.toRadians(spreadDegrees)) * distance;
        double minBoxSide = Math.min(predictedTargetBox.getXsize(), predictedTargetBox.getYsize());
        double candidateStep = Math.min(spreadRadius * 0.5, Math.max(0.05, minBoxSide * 0.35));

        Vec3[] candidateDirs = {
                Vec3.ZERO,
                right.scale(-1.0), right,
                up, up.scale(-1.0),
                right.scale(-1.0).add(up),
                right.add(up),
                right.scale(-1.0).add(up.scale(-1.0)),
                right.add(up.scale(-1.0))
        };

        Vec3[] sampleDirs = {
                Vec3.ZERO,
                right, right.scale(-1.0), up, up.scale(-1.0),
                right.add(up),
                right.scale(-1.0).add(up),
                right.add(up.scale(-1.0)),
                right.scale(-1.0).add(up.scale(-1.0))
        };

        AABB inflatedBox = new AABB(
                predictedTargetBox.minX - EPSILON, predictedTargetBox.minY - EPSILON, predictedTargetBox.minZ - EPSILON,
                predictedTargetBox.maxX + EPSILON, predictedTargetBox.maxY + EPSILON, predictedTargetBox.maxZ + EPSILON);

        SpreadAimSelection best = null;
        for (int i = 0; i < CANDIDATE_COUNT; i++) {
            Vec3 candidateOffset = candidateDirs[i].scale(candidateStep);
            int hitWeight = 0;
            double weightedDistanceSq = 0.0;
            for (int s = 0; s < SAMPLE_COUNT; s++) {
                Vec3 impact = predictedTarget.add(candidateOffset).add(sampleDirs[s].scale(spreadRadius));
                if (inflatedBox.contains(impact)) {
                    hitWeight += SAMPLE_WEIGHTS[s];
                    weightedDistanceSq += SAMPLE_WEIGHTS[s] * impact.distanceToSqr(predictedTarget);
                }
            }
            double score = hitWeight * 1_000_000.0 - weightedDistanceSq * 1000.0 - candidateOffset.lengthSqr();
            if (best == null || score > best.score) {
                best = new SpreadAimSelection(aimPoint.add(candidateOffset), CANDIDATE_NAMES[i], i, score, hitWeight);
            }
        }
        return best;
    }
}
