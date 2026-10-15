package com.kltyton.darwin_soldier.client.aim.control;

/**
 * A selectable switch candidate: an opaque id plus the direction of the
 * candidate relative to the player.
 */
public record TargetCandidate(int id, AimDirection direction) {

    public TargetCandidate {
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
    }
}
