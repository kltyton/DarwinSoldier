package com.kltyton.darwin_soldier.client.aim.control;

import java.util.List;
import java.util.Optional;

/**
 * Selects the candidate direction nearest a virtual cursor direction within a
 * maximum angular range. The current candidate is part of the normal candidate
 * set. Ties are deterministic: the current id wins, then the lowest id.
 */
public final class TargetSwitchSelector {

    private static final double ANGLE_EPSILON = 1.0E-9D;

    private TargetSwitchSelector() {
    }

    /**
     * @return the nearest in-range candidate, or empty when no candidate is
     *         within {@code maxAngularRangeDegrees}
     */
    public static Optional<TargetCandidate> select(
            List<TargetCandidate> candidates,
            int currentId,
            AimDirection cursor,
            double maxAngularRangeDegrees) {
        if (candidates == null || candidates.isEmpty() || cursor == null
                || !Double.isFinite(maxAngularRangeDegrees) || maxAngularRangeDegrees <= 0.0D) {
            return Optional.empty();
        }

        TargetCandidate best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (TargetCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            double distance = cursor.angleDegreesTo(candidate.direction());
            if (distance > maxAngularRangeDegrees + ANGLE_EPSILON) {
                continue;
            }
            if (best == null
                    || distance < bestDistance - ANGLE_EPSILON
                    || (Math.abs(distance - bestDistance) <= ANGLE_EPSILON
                            && prefers(candidate, best, currentId))) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best == null ? Optional.empty() : Optional.of(best);
    }

    private static boolean prefers(TargetCandidate candidate, TargetCandidate currentBest, int currentId) {
        boolean candidateIsCurrent = candidate.id() == currentId;
        boolean bestIsCurrent = currentBest.id() == currentId;
        if (candidateIsCurrent != bestIsCurrent) {
            return candidateIsCurrent;
        }
        return candidate.id() < currentBest.id();
    }
}
