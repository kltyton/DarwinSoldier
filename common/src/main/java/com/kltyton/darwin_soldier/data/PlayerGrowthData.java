package com.kltyton.darwin_soldier.data;

import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerGrowthData {
    private boolean enabled;
    private double currentGrowth;
    private double totalGrowth;
    private int totalPoints;
    private int healthPoints;
    private int attackPoints;
    private int defensePoints;
    private int perceptionPoints;
    private int cumulativeHealthPoints;
    private int cumulativeAttackPoints;
    private int cumulativeDefensePoints;
    private int cumulativePerceptionPoints;
    private final NutritionData nutrition = new NutritionData();
    private long lastEntityDamageGameTime = Long.MIN_VALUE;
    private boolean damageAdaptationUnlocked;
    private boolean huntingInstinctUnlocked;
    private boolean stressEvolutionUnlocked;
    private boolean superPerceptionUnlocked;
    private boolean battleInstinctUnlocked;
    private boolean battleInstinctEverUnlocked;
    private boolean damageAdaptationEnabled = true;
    private boolean huntingInstinctEnabled = true;
    private boolean huntingInternalImpactEnabled = true;
    private boolean stressEvolutionEnabled = true;
    private boolean superPerceptionEnabled = true;
    private boolean battleInstinctEnabled = true;
    private boolean battleInstinctCounterEnabled = true;
    private int battleInstinctReserves;
    private long battleInstinctLastReserveGameTime;
    private long battleInstinctInvulnerableUntil;
    private long huntingInstinctCooldownUntil;
    private long stressEvolutionCooldownUntil;
    private long stressEvolutionLastCooldownReductionTime = Long.MIN_VALUE;
    private final Map<String, AdaptationRecord> adaptationRecords = new LinkedHashMap<>();
    private transient long huntingInstinctActiveUntil;
    private transient int battleInstinctCounterTargetId = -1;
    private transient long battleInstinctCounterUntil;

    public static PlayerGrowthData load(CompoundTag tag) {
        PlayerGrowthData data = new PlayerGrowthData();
        data.enabled = tag.getBoolean("Enabled");
        data.currentGrowth = tag.getDouble("CurrentGrowth");
        data.totalGrowth = tag.getDouble("TotalGrowth");
        data.totalPoints = tag.getInt("TotalPoints");
        data.healthPoints = tag.getInt("HealthPoints");
        data.attackPoints = tag.getInt("AttackPoints");
        data.defensePoints = tag.getInt("DefensePoints");
        data.perceptionPoints = tag.getInt("PerceptionPoints");
        data.cumulativeHealthPoints = tag.contains("CumulativeHealthPoints") ? tag.getInt("CumulativeHealthPoints") : data.healthPoints;
        data.cumulativeAttackPoints = tag.contains("CumulativeAttackPoints") ? tag.getInt("CumulativeAttackPoints") : data.attackPoints;
        data.cumulativeDefensePoints = tag.contains("CumulativeDefensePoints") ? tag.getInt("CumulativeDefensePoints") : data.defensePoints;
        data.cumulativePerceptionPoints = tag.contains("CumulativePerceptionPoints") ? tag.getInt("CumulativePerceptionPoints") : data.perceptionPoints;
        data.nutrition.load(tag.getCompound("Nutrition"));
        data.lastEntityDamageGameTime = tag.getLong("LastEntityDamageGameTime");
        data.damageAdaptationUnlocked = tag.getBoolean("DamageAdaptationUnlocked");
        data.huntingInstinctUnlocked = tag.getBoolean("HuntingInstinctUnlocked");
        data.stressEvolutionUnlocked = tag.getBoolean("StressEvolutionUnlocked");
        data.superPerceptionUnlocked = tag.getBoolean("SuperPerceptionUnlocked");
        data.battleInstinctUnlocked = tag.getBoolean("BattleInstinctUnlocked");
        data.battleInstinctEverUnlocked = tag.contains("BattleInstinctEverUnlocked")
                ? tag.getBoolean("BattleInstinctEverUnlocked")
                : data.battleInstinctUnlocked;
        data.damageAdaptationEnabled = !tag.contains("DamageAdaptationEnabled") || tag.getBoolean("DamageAdaptationEnabled");
        data.huntingInstinctEnabled = !tag.contains("HuntingInstinctEnabled") || tag.getBoolean("HuntingInstinctEnabled");
        data.huntingInternalImpactEnabled = !tag.contains("HuntingInternalImpactEnabled") || tag.getBoolean("HuntingInternalImpactEnabled");
        data.stressEvolutionEnabled = !tag.contains("StressEvolutionEnabled") || tag.getBoolean("StressEvolutionEnabled");
        data.superPerceptionEnabled = !tag.contains("SuperPerceptionEnabled") || tag.getBoolean("SuperPerceptionEnabled");
        data.battleInstinctEnabled = !tag.contains("BattleInstinctEnabled") || tag.getBoolean("BattleInstinctEnabled");
        data.battleInstinctCounterEnabled = !tag.contains("BattleInstinctCounterEnabled")
                || tag.getBoolean("BattleInstinctCounterEnabled");
        data.battleInstinctReserves = tag.getInt("BattleInstinctReserves");
        data.battleInstinctLastReserveGameTime = tag.getLong("BattleInstinctLastReserveGameTime");
        data.huntingInstinctCooldownUntil = tag.getLong("HuntingInstinctCooldownUntil");
        data.stressEvolutionCooldownUntil = tag.getLong("StressEvolutionCooldownUntil");
        data.stressEvolutionLastCooldownReductionTime = tag.getLong("StressEvolutionLastCooldownReductionTime");
        CompoundTag recordsTag = tag.getCompound("AdaptationRecords");
        for (String key : recordsTag.getAllKeys()) {
            if (recordsTag.contains(key, Tag.TAG_COMPOUND)) {
                data.adaptationRecords.put(key, AdaptationRecord.load(key, recordsTag.getCompound(key)));
            }
        }
        data.sanitizeAllocation();
        data.convertGrowthToPoints();
        data.updateAbilityUnlocks();
        data.sanitizeBattleInstinct();
        return data;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Enabled", enabled);
        tag.putDouble("CurrentGrowth", currentGrowth);
        tag.putDouble("TotalGrowth", totalGrowth);
        tag.putInt("TotalPoints", totalPoints);
        tag.putInt("UsedPoints", getUsedPoints());
        tag.putInt("RemainingPoints", getRemainingPoints());
        tag.putInt("HealthPoints", healthPoints);
        tag.putInt("AttackPoints", attackPoints);
        tag.putInt("DefensePoints", defensePoints);
        tag.putInt("PerceptionPoints", perceptionPoints);
        tag.putInt("CumulativeHealthPoints", cumulativeHealthPoints);
        tag.putInt("CumulativeAttackPoints", cumulativeAttackPoints);
        tag.putInt("CumulativeDefensePoints", cumulativeDefensePoints);
        tag.putInt("CumulativePerceptionPoints", cumulativePerceptionPoints);
        tag.put("Nutrition", nutrition.save());
        tag.putLong("LastEntityDamageGameTime", lastEntityDamageGameTime);
        tag.putBoolean("DamageAdaptationUnlocked", damageAdaptationUnlocked);
        tag.putBoolean("HuntingInstinctUnlocked", huntingInstinctUnlocked);
        tag.putBoolean("StressEvolutionUnlocked", stressEvolutionUnlocked);
        tag.putBoolean("SuperPerceptionUnlocked", superPerceptionUnlocked);
        tag.putBoolean("BattleInstinctUnlocked", battleInstinctUnlocked);
        tag.putBoolean("BattleInstinctEverUnlocked", battleInstinctEverUnlocked);
        tag.putBoolean("DamageAdaptationEnabled", damageAdaptationEnabled);
        tag.putBoolean("HuntingInstinctEnabled", huntingInstinctEnabled);
        tag.putBoolean("HuntingInternalImpactEnabled", huntingInternalImpactEnabled);
        tag.putBoolean("StressEvolutionEnabled", stressEvolutionEnabled);
        tag.putBoolean("SuperPerceptionEnabled", superPerceptionEnabled);
        tag.putBoolean("BattleInstinctEnabled", battleInstinctEnabled);
        tag.putBoolean("BattleInstinctCounterEnabled", battleInstinctCounterEnabled);
        tag.putInt("BattleInstinctReserves", battleInstinctReserves);
        tag.putLong("BattleInstinctLastReserveGameTime", battleInstinctLastReserveGameTime);
        tag.putLong("HuntingInstinctCooldownUntil", huntingInstinctCooldownUntil);
        tag.putLong("StressEvolutionCooldownUntil", stressEvolutionCooldownUntil);
        tag.putLong("StressEvolutionLastCooldownReductionTime", stressEvolutionLastCooldownReductionTime);
        CompoundTag recordsTag = new CompoundTag();
        adaptationRecords.forEach((key, record) -> recordsTag.put(key, record.save()));
        tag.put("AdaptationRecords", recordsTag);
        return tag;
    }

    public void addGrowth(double amount) {
        if (amount <= 0.0D) {
            return;
        }

        currentGrowth += amount;
        totalGrowth += amount;
        convertGrowthToPoints();
    }

    private void convertGrowthToPoints() {
        double ratio = Math.max(1.0D, DarwinConfig.POINTS_PER_ATTRIBUTE_POINT.get());
        long converted = (long) Math.floor(currentGrowth / ratio);
        if (converted <= 0L) {
            return;
        }
        int available = Integer.MAX_VALUE - totalPoints;
        int points = (int) Math.min(converted, available);
        totalPoints += points;
        currentGrowth -= points * ratio;
    }

    public boolean allocate(int health, int attack, int defense, int perception, int nutritionPoints) {
        if (health < 0 || attack < 0 || defense < 0 || perception < 0 || nutritionPoints < 0
                || perception > DarwinConfig.PERCEPTION_MAX_POINTS.get()
                || nutritionPoints > DarwinConfig.NUTRITION_MAX_POINTS.get()) {
            return false;
        }
        long requested = (long) health + attack + defense + perception + nutritionPoints;
        if (requested > totalPoints || requested > Integer.MAX_VALUE) {
            return false;
        }

        recordHistoricalAllocation(health, attack, defense, perception, nutritionPoints);
        healthPoints = health;
        attackPoints = attack;
        defensePoints = defense;
        perceptionPoints = perception;
        nutrition.setPoints(nutritionPoints);
        updateAbilityUnlocks();
        return true;
    }

    public void updateAbilityUnlocks() {
        String before = unlockSummary();
        damageAdaptationUnlocked = defensePoints >= DarwinConfig.DAMAGE_ADAPTATION_UNLOCK_POINTS.get();
        huntingInstinctUnlocked = attackPoints >= DarwinConfig.HUNTING_INSTINCT_UNLOCK_POINTS.get();
        stressEvolutionUnlocked = healthPoints >= DarwinConfig.STRESS_EVOLUTION_UNLOCK_POINTS.get();
        superPerceptionUnlocked = perceptionPoints >= DarwinConfig.SUPER_PERCEPTION_UNLOCK_POINTS.get();

        boolean shouldUnlockBattleInstinct = perceptionPoints >= DarwinConfig.BATTLE_INSTINCT_UNLOCK_POINTS.get();
        if (shouldUnlockBattleInstinct && !battleInstinctEverUnlocked) {
            battleInstinctReserves = Math.max(battleInstinctReserves, DarwinConfig.BATTLE_INSTINCT_MAX_RESERVES.get());
            battleInstinctEverUnlocked = true;
        }
        battleInstinctUnlocked = shouldUnlockBattleInstinct;

        if (!battleInstinctUnlocked) {
            battleInstinctInvulnerableUntil = 0L;
            endBattleInstinctCounter();
        }
        nutrition.updateUnlocks();
        sanitizeBattleInstinct();
        String after = unlockSummary();
        if (!before.equals(after)) {
            RuntimeDiagnostics.info("ability_unlock_change", "points=" + healthPoints + "/" + attackPoints
                    + "/" + defensePoints + "/" + perceptionPoints + "/" + nutrition.getPoints()
                    + " thresholds=" + DarwinConfig.STRESS_EVOLUTION_UNLOCK_POINTS.get() + "/"
                    + DarwinConfig.HUNTING_INSTINCT_UNLOCK_POINTS.get() + "/"
                    + DarwinConfig.DAMAGE_ADAPTATION_UNLOCK_POINTS.get() + "/"
                    + DarwinConfig.SUPER_PERCEPTION_UNLOCK_POINTS.get() + "/"
                    + DarwinConfig.BATTLE_INSTINCT_UNLOCK_POINTS.get() + "/"
                    + DarwinConfig.EFFICIENT_METABOLISM_UNLOCK_POINTS.get() + "/"
                    + DarwinConfig.NUTRITION_FULLNESS_UNLOCK_POINTS.get()
                    + " before=" + before + " after=" + after);
        }
    }

    private String unlockSummary() {
        return damageAdaptationUnlocked + "/" + huntingInstinctUnlocked + "/" + stressEvolutionUnlocked
                + "/" + superPerceptionUnlocked + "/" + battleInstinctUnlocked + "/"
                + nutrition.isEfficientMetabolismUnlocked() + "/" + nutrition.isNutritionFullnessUnlocked();
    }

    private void recordHistoricalAllocation(int newHealth, int newAttack, int newDefense, int newPerception, int newNutrition) {
        long remainingNewPoints = Math.max(0L, (long) totalPoints - getHistoricalAllocatedPoints());
        if (remainingNewPoints <= 0) {
            return;
        }

        int added = (int) Math.min(remainingNewPoints, Math.max(0, newHealth - cumulativeHealthPoints));
        cumulativeHealthPoints += added;
        remainingNewPoints -= added;

        added = (int) Math.min(remainingNewPoints, Math.max(0, newAttack - cumulativeAttackPoints));
        cumulativeAttackPoints += added;
        remainingNewPoints -= added;

        added = (int) Math.min(remainingNewPoints, Math.max(0, newDefense - cumulativeDefensePoints));
        cumulativeDefensePoints += added;
        remainingNewPoints -= added;

        added = (int) Math.min(remainingNewPoints, Math.max(0, newPerception - cumulativePerceptionPoints));
        cumulativePerceptionPoints += added;
        remainingNewPoints -= added;

        added = (int) Math.min(remainingNewPoints, Math.max(0, newNutrition - nutrition.getCumulativePoints()));
        nutrition.addHistoricalPoints(added);
    }

    public AdaptationRecord recordAdaptationDeath(String key, String displayName, String targetId, boolean playerTarget) {
        AdaptationRecord record = adaptationRecords.computeIfAbsent(key, ignored -> new AdaptationRecord(key, displayName, targetId, playerTarget));
        record.recordDeath(displayName, targetId);
        return record;
    }

    public AdaptationRecord getAdaptationRecord(String key) {
        return adaptationRecords.get(key);
    }

    public AdaptationRecord getOrCreateAdaptationRecord(String key, String displayName, String targetId, boolean playerTarget) {
        return adaptationRecords.computeIfAbsent(key, ignored -> new AdaptationRecord(key, displayName, targetId, playerTarget));
    }

    public void clearAdaptationRecords() {
        adaptationRecords.clear();
    }

    private void sanitizeAllocation() {
        healthPoints = Math.max(0, healthPoints);
        attackPoints = Math.max(0, attackPoints);
        defensePoints = Math.max(0, defensePoints);
        perceptionPoints = Math.max(0, Math.min(DarwinConfig.PERCEPTION_MAX_POINTS.get(), perceptionPoints));
        cumulativeHealthPoints = Math.max(0, cumulativeHealthPoints);
        cumulativeAttackPoints = Math.max(0, cumulativeAttackPoints);
        cumulativeDefensePoints = Math.max(0, cumulativeDefensePoints);
        cumulativePerceptionPoints = Math.max(0, cumulativePerceptionPoints);
        nutrition.sanitize();
        if (getUsedPoints() <= totalPoints) {
            return;
        }

        healthPoints = 0;
        attackPoints = 0;
        defensePoints = 0;
        perceptionPoints = 0;
        nutrition.setPoints(0);
    }

    private void sanitizeBattleInstinct() {
        int maxReserves = Math.max(0, DarwinConfig.BATTLE_INSTINCT_MAX_RESERVES.get());
        battleInstinctReserves = Math.max(0, Math.min(maxReserves, battleInstinctReserves));
        battleInstinctLastReserveGameTime = Math.max(0L, battleInstinctLastReserveGameTime);
        battleInstinctInvulnerableUntil = Math.max(0L, battleInstinctInvulnerableUntil);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCurrentGrowth(double currentGrowth) {
        this.currentGrowth = Math.max(0.0D, currentGrowth);
        convertGrowthToPoints();
    }

    public double getCurrentGrowth() {
        return currentGrowth;
    }

    public void setTotalGrowth(double totalGrowth) {
        this.totalGrowth = Math.max(0.0D, totalGrowth);
    }

    public double getTotalGrowth() {
        return totalGrowth;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = Math.max(0, totalPoints);
        sanitizeAllocation();
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getUsedPoints() {
        return healthPoints + attackPoints + defensePoints + perceptionPoints + nutrition.getPoints();
    }

    public int getRemainingPoints() {
        return Math.max(0, totalPoints - getUsedPoints());
    }

    private long getHistoricalAllocatedPoints() {
        return (long) cumulativeHealthPoints + cumulativeAttackPoints + cumulativeDefensePoints
                + cumulativePerceptionPoints + nutrition.getCumulativePoints();
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public void setHealthPointsDebug(int healthPoints) {
        this.healthPoints = Math.max(0, healthPoints);
        this.cumulativeHealthPoints = Math.max(this.cumulativeHealthPoints, this.healthPoints);
        ensureTotalPointsCoversAllocation();
        updateAbilityUnlocks();
    }

    public int getAttackPoints() {
        return attackPoints;
    }

    public void setAttackPointsDebug(int attackPoints) {
        this.attackPoints = Math.max(0, attackPoints);
        this.cumulativeAttackPoints = Math.max(this.cumulativeAttackPoints, this.attackPoints);
        ensureTotalPointsCoversAllocation();
        updateAbilityUnlocks();
    }

    public int getDefensePoints() {
        return defensePoints;
    }

    public void setDefensePointsDebug(int defensePoints) {
        this.defensePoints = Math.max(0, defensePoints);
        this.cumulativeDefensePoints = Math.max(this.cumulativeDefensePoints, this.defensePoints);
        ensureTotalPointsCoversAllocation();
        updateAbilityUnlocks();
    }

    public int getPerceptionPoints() {
        return perceptionPoints;
    }

    public void setPerceptionPointsDebug(int perceptionPoints) {
        this.perceptionPoints = Math.max(0, Math.min(DarwinConfig.PERCEPTION_MAX_POINTS.get(), perceptionPoints));
        this.cumulativePerceptionPoints = Math.max(this.cumulativePerceptionPoints, this.perceptionPoints);
        ensureTotalPointsCoversAllocation();
        updateAbilityUnlocks();
    }

    public int getNutritionPoints() {
        return nutrition.getPoints();
    }

    public void setNutritionPointsDebug(int nutritionPoints) {
        nutrition.setPointsDebug(nutritionPoints);
        ensureTotalPointsCoversAllocation();
        updateAbilityUnlocks();
    }

    public int getCumulativeHealthPoints() {
        return cumulativeHealthPoints;
    }

    public void setCumulativeHealthPoints(int cumulativeHealthPoints) {
        this.cumulativeHealthPoints = Math.max(0, cumulativeHealthPoints);
        updateAbilityUnlocks();
    }

    public int getCumulativeAttackPoints() {
        return cumulativeAttackPoints;
    }

    public void setCumulativeAttackPoints(int cumulativeAttackPoints) {
        this.cumulativeAttackPoints = Math.max(0, cumulativeAttackPoints);
        updateAbilityUnlocks();
    }

    public int getCumulativeDefensePoints() {
        return cumulativeDefensePoints;
    }

    public void setCumulativeDefensePoints(int cumulativeDefensePoints) {
        this.cumulativeDefensePoints = Math.max(0, cumulativeDefensePoints);
        updateAbilityUnlocks();
    }

    public int getCumulativePerceptionPoints() {
        return cumulativePerceptionPoints;
    }

    public void setCumulativePerceptionPoints(int cumulativePerceptionPoints) {
        this.cumulativePerceptionPoints = Math.max(0, cumulativePerceptionPoints);
        updateAbilityUnlocks();
    }

    public int getCumulativeNutritionPoints() {
        return nutrition.getCumulativePoints();
    }

    public void setCumulativeNutritionPoints(int cumulativeNutritionPoints) {
        nutrition.setCumulativePoints(cumulativeNutritionPoints);
        updateAbilityUnlocks();
    }

    public NutritionData getNutrition() {
        return nutrition;
    }

    private void ensureTotalPointsCoversAllocation() {
        if (getUsedPoints() > totalPoints) {
            totalPoints = getUsedPoints();
        }
    }

    public long getLastEntityDamageGameTime() {
        return lastEntityDamageGameTime;
    }

    public void setLastEntityDamageGameTime(long lastEntityDamageGameTime) {
        this.lastEntityDamageGameTime = lastEntityDamageGameTime;
    }

    public boolean isAbilityUnlocked(AbilityType type) {
        return switch (type) {
            case DAMAGE_ADAPTATION -> damageAdaptationUnlocked;
            case HUNTING_INSTINCT -> huntingInstinctUnlocked;
            case HUNTING_INTERNAL_IMPACT -> huntingInstinctUnlocked;
            case STRESS_EVOLUTION -> stressEvolutionUnlocked;
            case SUPER_PERCEPTION -> superPerceptionUnlocked;
            case BATTLE_INSTINCT -> battleInstinctUnlocked;
            case EFFICIENT_METABOLISM -> nutrition.isEfficientMetabolismUnlocked();
            case NUTRITION_FULLNESS -> nutrition.isNutritionFullnessUnlocked();
        };
    }

    public void setAbilityUnlocked(AbilityType type, boolean unlocked) {
        switch (type) {
            case DAMAGE_ADAPTATION -> damageAdaptationUnlocked = unlocked;
            case HUNTING_INSTINCT -> huntingInstinctUnlocked = unlocked;
            case HUNTING_INTERNAL_IMPACT -> huntingInstinctUnlocked = unlocked;
            case STRESS_EVOLUTION -> stressEvolutionUnlocked = unlocked;
            case SUPER_PERCEPTION -> superPerceptionUnlocked = unlocked;
            case BATTLE_INSTINCT -> {
                battleInstinctUnlocked = unlocked;
                if (unlocked) {
                    battleInstinctEverUnlocked = true;
                    battleInstinctReserves = Math.max(battleInstinctReserves, DarwinConfig.BATTLE_INSTINCT_MAX_RESERVES.get());
                } else {
                    battleInstinctInvulnerableUntil = 0L;
                    endBattleInstinctCounter();
                }
            }
            case EFFICIENT_METABOLISM -> nutrition.setEfficientMetabolismUnlocked(unlocked);
            case NUTRITION_FULLNESS -> nutrition.setNutritionFullnessUnlocked(unlocked);
        }
        sanitizeBattleInstinct();
    }

    public boolean isHuntingInstinctUnlocked() {
        return huntingInstinctUnlocked;
    }

    public boolean isHuntingInstinctEnabled() {
        return huntingInstinctEnabled;
    }

    public void setHuntingInstinctEnabled(boolean huntingInstinctEnabled) {
        this.huntingInstinctEnabled = huntingInstinctEnabled;
    }

    public boolean isHuntingInternalImpactEnabled() {
        return huntingInternalImpactEnabled;
    }

    public void setHuntingInternalImpactEnabled(boolean huntingInternalImpactEnabled) {
        this.huntingInternalImpactEnabled = huntingInternalImpactEnabled;
    }

    public boolean isStressEvolutionUnlocked() {
        return stressEvolutionUnlocked;
    }

    public boolean isStressEvolutionEnabled() {
        return stressEvolutionEnabled;
    }

    public void setStressEvolutionEnabled(boolean stressEvolutionEnabled) {
        this.stressEvolutionEnabled = stressEvolutionEnabled;
    }

    public boolean isSuperPerceptionUnlocked() {
        return superPerceptionUnlocked;
    }

    public boolean isSuperPerceptionEnabled() {
        return superPerceptionEnabled;
    }

    public void setSuperPerceptionEnabled(boolean superPerceptionEnabled) {
        this.superPerceptionEnabled = superPerceptionEnabled;
    }

    public boolean isBattleInstinctUnlocked() {
        return battleInstinctUnlocked;
    }

    public boolean isBattleInstinctEnabled() {
        return battleInstinctEnabled;
    }

    public void setBattleInstinctEnabled(boolean battleInstinctEnabled) {
        this.battleInstinctEnabled = battleInstinctEnabled;
    }

    public boolean isBattleInstinctCounterEnabled() {
        return battleInstinctCounterEnabled;
    }

    public void setBattleInstinctCounterEnabled(boolean battleInstinctCounterEnabled) {
        this.battleInstinctCounterEnabled = battleInstinctCounterEnabled;
        if (!battleInstinctCounterEnabled) {
            endBattleInstinctCounter();
        }
    }

    public int getBattleInstinctReserves() {
        sanitizeBattleInstinct();
        return battleInstinctReserves;
    }

    public void setBattleInstinctReserves(int battleInstinctReserves) {
        this.battleInstinctReserves = battleInstinctReserves;
        sanitizeBattleInstinct();
    }

    public int restoreBattleInstinctReserves(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = getBattleInstinctReserves();
        setBattleInstinctReserves(before + amount);
        return getBattleInstinctReserves() - before;
    }

    public boolean consumeBattleInstinctReserve(long now) {
        sanitizeBattleInstinct();
        if (battleInstinctReserves <= 0) {
            return false;
        }
        battleInstinctReserves--;
        battleInstinctLastReserveGameTime = Math.max(0L, now);
        return true;
    }

    public long getBattleInstinctLastReserveGameTime() {
        return battleInstinctLastReserveGameTime;
    }

    public void setBattleInstinctLastReserveGameTime(long battleInstinctLastReserveGameTime) {
        this.battleInstinctLastReserveGameTime = Math.max(0L, battleInstinctLastReserveGameTime);
    }

    public long getBattleInstinctInvulnerableUntil() {
        return battleInstinctInvulnerableUntil;
    }

    public void setBattleInstinctInvulnerableUntil(long battleInstinctInvulnerableUntil) {
        this.battleInstinctInvulnerableUntil = Math.max(0L, battleInstinctInvulnerableUntil);
    }

    public boolean isDamageAdaptationUnlocked() {
        return damageAdaptationUnlocked;
    }

    public boolean isDamageAdaptationEnabled() {
        return damageAdaptationEnabled;
    }

    public void setDamageAdaptationEnabled(boolean damageAdaptationEnabled) {
        this.damageAdaptationEnabled = damageAdaptationEnabled;
    }

    public long getHuntingInstinctCooldownUntil() {
        return huntingInstinctCooldownUntil;
    }

    public void setHuntingInstinctCooldownUntil(long huntingInstinctCooldownUntil) {
        this.huntingInstinctCooldownUntil = Math.max(0L, huntingInstinctCooldownUntil);
    }

    public long getStressEvolutionCooldownUntil() {
        return stressEvolutionCooldownUntil;
    }

    public void setStressEvolutionCooldownUntil(long stressEvolutionCooldownUntil) {
        this.stressEvolutionCooldownUntil = Math.max(0L, stressEvolutionCooldownUntil);
    }

    public long getStressEvolutionLastCooldownReductionTime() {
        return stressEvolutionLastCooldownReductionTime;
    }

    public void setStressEvolutionLastCooldownReductionTime(long stressEvolutionLastCooldownReductionTime) {
        this.stressEvolutionLastCooldownReductionTime = stressEvolutionLastCooldownReductionTime;
    }

    public Collection<AdaptationRecord> getAdaptationRecords() {
        return adaptationRecords.values();
    }

    public long getHuntingInstinctActiveUntil() {
        return huntingInstinctActiveUntil;
    }

    public void beginHuntingInstinct(long activeUntil) {
        huntingInstinctActiveUntil = Math.max(0L, activeUntil);
    }

    public boolean isHuntingInstinctActive(long now) {
        return huntingInstinctActiveUntil > now;
    }

    public void endHuntingInstinct() {
        huntingInstinctActiveUntil = 0L;
    }

    public void beginBattleInstinctCounter(int targetId, long until) {
        this.battleInstinctCounterTargetId = targetId;
        this.battleInstinctCounterUntil = until;
    }

    public boolean isBattleInstinctCounterActiveFor(int targetId, long now) {
        return battleInstinctCounterTargetId == targetId && battleInstinctCounterUntil >= now;
    }

    public void endBattleInstinctCounter() {
        battleInstinctCounterTargetId = -1;
        battleInstinctCounterUntil = 0L;
    }
}
