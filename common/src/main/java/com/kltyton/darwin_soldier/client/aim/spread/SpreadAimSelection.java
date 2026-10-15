package com.kltyton.darwin_soldier.client.aim.spread;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public final class SpreadAimSelection {

    public final Vec3 aimPoint;
    public final String candidate;
    public final int candidateIndex;
    public final double score;
    public final int hitWeight;

    public SpreadAimSelection(Vec3 aimPoint, String candidate, int candidateIndex, double score, int hitWeight) {
        this.aimPoint = aimPoint;
        this.candidate = candidate;
        this.candidateIndex = candidateIndex;
        this.score = score;
        this.hitWeight = hitWeight;
    }

    public static SpreadAimSelection center(Vec3 aimPoint) {
        return new SpreadAimSelection(aimPoint, "center", 0, 0.0, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpreadAimSelection)) {
            return false;
        }
        SpreadAimSelection other = (SpreadAimSelection) obj;
        return candidateIndex == other.candidateIndex
                && hitWeight == other.hitWeight
                && Double.compare(score, other.score) == 0
                && Objects.equals(aimPoint, other.aimPoint)
                && Objects.equals(candidate, other.candidate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aimPoint, candidate, candidateIndex, score, hitWeight);
    }
}
