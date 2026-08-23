package com.kltyton.darwin_soldier.client.aim;

import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileBallistics;

import javax.annotation.Nullable;

public record AimWeaponSettings(
        AimMode mode,
        AimWeaponTuning manual,
        AimWeaponTuning adaptive,
        boolean trajectoryVisible,
        @Nullable ProjectileBallistics ballistics
) {
    public static AimWeaponSettings defaults() {
        return new AimWeaponSettings(
                AimMode.MANUAL,
                AimWeaponTuning.defaults(),
                AimWeaponTuning.adaptiveDefaults(),
                false,
                null
        );
    }

    public AimWeaponSettings sanitized() {
        return new AimWeaponSettings(
                mode == null ? AimMode.MANUAL : mode,
                manual == null ? AimWeaponTuning.defaults() : manual.sanitized(),
                adaptive == null ? AimWeaponTuning.adaptiveDefaults() : adaptive.sanitized(),
                trajectoryVisible,
                ballistics == null ? null : ballistics.sanitized()
        );
    }

    public AimWeaponTuning activeTuning() {
        return mode == AimMode.ADAPTIVE ? adaptive : manual;
    }

    public AimWeaponSettings withMode(AimMode value) {
        return new AimWeaponSettings(value, manual, adaptive, trajectoryVisible, ballistics).sanitized();
    }

    public AimWeaponSettings withActiveTuning(AimWeaponTuning tuning) {
        return mode == AimMode.ADAPTIVE
                ? new AimWeaponSettings(mode, manual, tuning, trajectoryVisible, ballistics).sanitized()
                : new AimWeaponSettings(mode, tuning, adaptive, trajectoryVisible, ballistics).sanitized();
    }

    public AimWeaponSettings withTrajectoryVisible(boolean value) {
        return new AimWeaponSettings(mode, manual, adaptive, value, ballistics).sanitized();
    }

    public AimWeaponSettings withBallistics(@Nullable ProjectileBallistics value) {
        return new AimWeaponSettings(mode, manual, adaptive, trajectoryVisible, value).sanitized();
    }
}
