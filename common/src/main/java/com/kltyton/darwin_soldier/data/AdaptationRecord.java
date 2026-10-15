package com.kltyton.darwin_soldier.data;

import com.kltyton.darwin_soldier.combat.adaptation.AdaptationRules;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.nbt.CompoundTag;

public class AdaptationRecord {
    private final String key;
    private String displayName;
    private String targetId;
    private boolean playerTarget;
    private int level;
    private boolean enabled = true;

    public AdaptationRecord(String key, String displayName, String targetId, boolean playerTarget) {
        this.key = key;
        this.displayName = displayName;
        this.targetId = targetId;
        this.playerTarget = playerTarget;
    }

    public static AdaptationRecord load(String key, CompoundTag tag) {
        AdaptationRecord record = new AdaptationRecord(
                key,
                tag.getString("DisplayName"),
                tag.getString("TargetId"),
                tag.getBoolean("PlayerTarget")
        );
        record.level = tag.getInt("Level");
        record.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        record.sanitize();
        return record;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("DisplayName", displayName);
        tag.putString("TargetId", targetId);
        tag.putBoolean("PlayerTarget", playerTarget);
        tag.putInt("Level", level);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public void recordDeath(String displayName, String targetId) {
        this.displayName = displayName;
        this.targetId = targetId;
        level = AdaptationRules.nextLevel(level, DarwinConfig.ADAPTATION_MAX_LEVEL.get());
        sanitize();
    }

    private void sanitize() {
        level = AdaptationRules.clampLevel(level, DarwinConfig.ADAPTATION_MAX_LEVEL.get());
        if (displayName == null || displayName.isBlank()) {
            displayName = targetId;
        }
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTargetId() {
        return targetId;
    }

    public boolean isPlayerTarget() {
        return playerTarget;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = AdaptationRules.clampLevel(level, DarwinConfig.ADAPTATION_MAX_LEVEL.get());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
