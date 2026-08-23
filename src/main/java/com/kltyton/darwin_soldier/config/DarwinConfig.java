package com.kltyton.darwin_soldier.config;

import com.kltyton.darwin_soldier.combat.adaptation.AdaptationRules;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class DarwinConfig {
    private static final int CURRENT_CONFIG_VERSION = 1;

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CONFIG_VERSION;

    public static final ForgeConfigSpec.DoubleValue PEACEFUL_GROWTH;
    public static final ForgeConfigSpec.DoubleValue NEUTRAL_GROWTH;
    public static final ForgeConfigSpec.DoubleValue HOSTILE_GROWTH;
    public static final ForgeConfigSpec.DoubleValue BOSS_GROWTH;
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue POINTS_PER_ATTRIBUTE_POINT;
    public static final ForgeConfigSpec.DoubleValue LOW_HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ANTI_AFK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue PET_GROWTH_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue PARTICIPATION_REFRESH_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue PARTICIPATION_EXPIRE_SECONDS;
    public static final ForgeConfigSpec.IntValue PARTICIPATION_MAX_PLAYERS;
    public static final ForgeConfigSpec.DoubleValue HEALTH_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue ATTACK_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue BASE_CRITICAL_CHANCE;
    public static final ForgeConfigSpec.DoubleValue BASE_CRITICAL_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue PERCEPTION_CRITICAL_CHANCE_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue PERCEPTION_CRITICAL_DAMAGE_PER_POINT;
    public static final ForgeConfigSpec.BooleanValue DERIVED_ATTRIBUTES_ENABLED;
    public static final ForgeConfigSpec.IntValue HEALING_AMPLIFICATION_HEALTH_POINTS;
    public static final ForgeConfigSpec.DoubleValue HEALING_AMPLIFICATION_PER_STEP;
    public static final ForgeConfigSpec.IntValue ARMOR_PIERCE_ATTACK_POINTS;
    public static final ForgeConfigSpec.DoubleValue ARMOR_PIERCE_PER_STEP;
    public static final ForgeConfigSpec.IntValue ARMOR_TOUGHNESS_DEFENSE_POINTS;
    public static final ForgeConfigSpec.DoubleValue ARMOR_TOUGHNESS_PER_STEP;
    public static final ForgeConfigSpec.BooleanValue MINIMUM_EFFECTIVE_DAMAGE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue MINIMUM_EFFECTIVE_DAMAGE_RATIO;
    public static final ForgeConfigSpec.IntValue MINIMUM_EFFECTIVE_DAMAGE_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue PERCEPTION_MAX_POINTS;
    public static final ForgeConfigSpec.IntValue SUPER_PERCEPTION_UNLOCK_POINTS;
    public static final ForgeConfigSpec.DoubleValue SUPER_PERCEPTION_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue SUPER_PERCEPTION_ANGLE;
    public static final ForgeConfigSpec.BooleanValue SUPER_PERCEPTION_TARGET_MONSTERS;
    public static final ForgeConfigSpec.BooleanValue SUPER_PERCEPTION_TARGET_NEUTRAL;
    public static final ForgeConfigSpec.BooleanValue SUPER_PERCEPTION_TARGET_PASSIVE;
    public static final ForgeConfigSpec.BooleanValue SUPER_PERCEPTION_TARGET_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue SUPER_PERCEPTION_FILTER_PETS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SUPER_PERCEPTION_WHITELIST;
    public static final ForgeConfigSpec.BooleanValue NUTRITION_ENABLED;
    public static final ForgeConfigSpec.IntValue NUTRITION_MAX_POINTS;
    public static final ForgeConfigSpec.IntValue NUTRITION_BASE_MAX_FOOD;
    public static final ForgeConfigSpec.IntValue NUTRITION_MAX_FOOD_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue NUTRITION_FOOD_RESTORE_BONUS_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue NUTRITION_SATURATION_RESTORE_BONUS_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue NUTRITION_DRINK_RESTORE_BONUS_PER_POINT;
    public static final ForgeConfigSpec.IntValue EFFICIENT_METABOLISM_UNLOCK_POINTS;
    public static final ForgeConfigSpec.IntValue EFFICIENT_METABOLISM_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue EFFICIENT_METABOLISM_HEAL_MAX_HEALTH_PERCENT;
    public static final ForgeConfigSpec.IntValue EFFICIENT_METABOLISM_FOOD_COST;
    public static final ForgeConfigSpec.IntValue EFFICIENT_METABOLISM_MIN_FOOD;
    public static final ForgeConfigSpec.DoubleValue EFFICIENT_METABOLISM_THIRST_COST;
    public static final ForgeConfigSpec.DoubleValue EFFICIENT_METABOLISM_MIN_THIRST_PERCENT;
    public static final ForgeConfigSpec.IntValue NUTRITION_FULLNESS_UNLOCK_POINTS;
    public static final ForgeConfigSpec.DoubleValue NUTRITION_FULLNESS_THRESHOLD;
    public static final ForgeConfigSpec.IntValue NUTRITION_FULLNESS_HASTE_LEVEL;
    public static final ForgeConfigSpec.DoubleValue NUTRITION_FULLNESS_ABSORPTION_BASE;
    public static final ForgeConfigSpec.DoubleValue NUTRITION_FULLNESS_ABSORPTION_MAX_HEALTH_PERCENT;
    public static final ForgeConfigSpec.IntValue NUTRITION_FULLNESS_ABSORPTION_REFILL_SECONDS;
    public static final ForgeConfigSpec.IntValue NUTRITION_FULLNESS_COMBAT_DELAY_SECONDS;
    public static final ForgeConfigSpec.BooleanValue ADAPTATION_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ADAPTATION_PVP_ENABLED;
    public static final ForgeConfigSpec.IntValue DAMAGE_ADAPTATION_UNLOCK_POINTS;
    public static final ForgeConfigSpec.IntValue HUNTING_INSTINCT_UNLOCK_POINTS;
    public static final ForgeConfigSpec.IntValue STRESS_EVOLUTION_UNLOCK_POINTS;
    public static final ForgeConfigSpec.DoubleValue ADAPTATION_LV1;
    public static final ForgeConfigSpec.DoubleValue ADAPTATION_LV2;
    public static final ForgeConfigSpec.DoubleValue ADAPTATION_LV3;
    public static final ForgeConfigSpec.DoubleValue ADAPTATION_LV4;
    public static final ForgeConfigSpec.DoubleValue ADAPTATION_LV5;
    public static final ForgeConfigSpec.IntValue ADAPTATION_MAX_LEVEL;
    public static final ForgeConfigSpec.BooleanValue HUNT_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNTING_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HUNTING_BASE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue HUNTING_KNOCKBACK_PER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HUNTING_MAX_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue HUNTING_ACTIVATION_PARTICLE_COUNT;
    public static final ForgeConfigSpec.IntValue HUNTING_HIT_PARTICLE_COUNT;
    public static final ForgeConfigSpec.IntValue HUNTING_ACTIVE_SECONDS;
    public static final ForgeConfigSpec.IntValue HUNTING_SPEED_LEVEL;
    public static final ForgeConfigSpec.IntValue HUNTING_JUMP_LEVEL;
    public static final ForgeConfigSpec.IntValue HUNTING_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue HUNTING_LOW_HEALTH_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HUNTING_LOW_HEALTH_PERCENT;
    public static final ForgeConfigSpec.BooleanValue HUNTING_INTERNAL_IMPACT_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNTING_SHOCK_RADIUS;
    public static final ForgeConfigSpec.DoubleValue HUNTING_SHOCK_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue HUNTING_INJURY_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue HUNTING_INJURY_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue HUNTING_INJURY_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HUNTING_VULNERABILITY_BONUS;
    public static final ForgeConfigSpec.BooleanValue BATTLE_INSTINCT_ENABLED;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_UNLOCK_POINTS;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_MAX_RESERVES;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_RECOVER_SECONDS;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_LOW_HEALTH_RECOVER_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BATTLE_INSTINCT_LOW_HEALTH_PERCENT;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_INVULNERABILITY_TICKS;
    public static final ForgeConfigSpec.DoubleValue BATTLE_INSTINCT_COUNTER_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BATTLE_INSTINCT_COUNTER_RANGE_PADDING;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_COUNTER_KILL_RESERVES;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_BLOCK_POSE_TICKS;
    public static final ForgeConfigSpec.IntValue BATTLE_INSTINCT_TRAIL_TICKS;
    public static final ForgeConfigSpec.DoubleValue BATTLE_INSTINCT_BLOCK_SOUND_VOLUME;
    public static final ForgeConfigSpec.DoubleValue BATTLE_INSTINCT_BLOCK_SOUND_PITCH;
    public static final ForgeConfigSpec.BooleanValue EVOLUTION_ENABLED;
    public static final ForgeConfigSpec.IntValue STRESS_UTILITY_EFFECT_SECONDS;
    public static final ForgeConfigSpec.IntValue STRESS_RESISTANCE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue STRESS_SHOCK_RANGE;
    public static final ForgeConfigSpec.DoubleValue STRESS_NORMAL_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue STRESS_ENHANCED_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue STRESS_NORMAL_SLOW_LEVEL;
    public static final ForgeConfigSpec.IntValue STRESS_NORMAL_SLOW_SECONDS;
    public static final ForgeConfigSpec.IntValue STRESS_ENHANCED_SLOW_LEVEL;
    public static final ForgeConfigSpec.IntValue STRESS_ENHANCED_SLOW_SECONDS;
    public static final ForgeConfigSpec.IntValue STRESS_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue STRESS_DAMAGE_COOLDOWN_REDUCTION_SECONDS;
    public static final ForgeConfigSpec.IntValue STRESS_DAMAGE_COOLDOWN_REDUCTION_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue STRESS_VISUAL_EXPLOSION_PARTICLES;
    public static final ForgeConfigSpec.IntValue STRESS_VISUAL_SHOCKWAVE_PARTICLES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INTIMIDATION_IMMUNITY;
    public static final ForgeConfigSpec.BooleanValue RUNTIME_DIAGNOSTICS_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("migration");
        CONFIG_VERSION = builder.comment("内部配置迁移版本，请勿手动修改。")
                .defineInRange("version", 0, 0, CURRENT_CONFIG_VERSION);
        builder.pop();

        builder.push("growth");
        PEACEFUL_GROWTH = builder.comment("从被动生物中获得的增长。")
                .defineInRange("peacefulGrowth", 0.5D, 0.0D, Double.MAX_VALUE);
        NEUTRAL_GROWTH = builder.comment("从中立生物获得的增长。")
                .defineInRange("neutralGrowth", 1.0D, 0.0D, Double.MAX_VALUE);
        HOSTILE_GROWTH = builder.comment("从敌对生物获得的增长。")
                .defineInRange("hostileGrowth", 1.0D, 0.0D, Double.MAX_VALUE);
        BOSS_GROWTH = builder.comment("从Boss那里获得增长。")
                .defineInRange("bossGrowth", 20.0D, 0.0D, Double.MAX_VALUE);
        BOSS_HEALTH_THRESHOLD = builder.comment("最大健康值等于或高于此值的生命实体将被视为boss。")
                .defineInRange("boss_health_threshold", 150.0D, 1.0D, Double.MAX_VALUE);
        POINTS_PER_ATTRIBUTE_POINT = builder.comment("每个属性值消耗的当前增长。")
                .defineInRange("pointsPerAttributePoint", 200.0D, 1.0D, Double.MAX_VALUE);
        LOW_HEALTH_MULTIPLIER = builder.comment("当击杀者处于或低于20% 最大健康时的乘数。")
                .defineInRange("lowHealthMultiplier", 3.0D, 0.0D, Double.MAX_VALUE);
        ANTI_AFK_MULTIPLIER = builder.comment("当击杀者在过去30秒内没有受到实体伤害时的乘数。")
                .defineInRange("antiAfkMultiplier", 0.1D, 0.0D, Double.MAX_VALUE);
        PET_GROWTH_MULTIPLIER = builder.comment("只有宠物、召唤物或可追溯主人实体参与击杀时的成长倍率。")
                .defineInRange("pet_growth_multiplier", 0.5D, 0.0D, Double.MAX_VALUE);
        PARTICIPATION_REFRESH_INTERVAL_SECONDS = builder.comment("同一玩家对同一实体的参与记录最短刷新间隔。")
                .defineInRange("participation_refresh_interval", 1, 0, Integer.MAX_VALUE);
        PARTICIPATION_EXPIRE_SECONDS = builder.comment("击杀参与记录在多少秒后过期。")
                .defineInRange("participation_expire_seconds", 300, 1, Integer.MAX_VALUE);
        PARTICIPATION_MAX_PLAYERS = builder.comment("单个实体最多记录的参与玩家数量，超过后移除最早记录。")
                .defineInRange("participation_max_players", 32, 1, 1024);
        builder.pop();

        builder.push("base_attributes");
        HEALTH_PER_POINT = builder.comment("每点生命属性增加的最大生命值。")
                .defineInRange("health_per_point", 1.5D, 0.0D, Double.MAX_VALUE);
        ATTACK_PER_POINT = builder.comment("每点攻击属性增加的攻击力。")
                .defineInRange("attack_per_point", 0.4D, 0.0D, Double.MAX_VALUE);
        DEFENSE_PER_POINT = builder.comment("每点防御属性增加的护甲。")
                .defineInRange("defense_per_point", 0.25D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("critical");
        BASE_CRITICAL_CHANCE = builder.defineInRange("base_critical_chance", 0.0D, 0.0D, 1.0D);
        BASE_CRITICAL_DAMAGE = builder.defineInRange("base_critical_damage", 1.5D, 1.0D, 1024.0D);
        PERCEPTION_CRITICAL_CHANCE_PER_POINT = builder.defineInRange("perception_critical_chance_per_point", 0.02D, 0.0D, 1.0D);
        PERCEPTION_CRITICAL_DAMAGE_PER_POINT = builder.defineInRange("perception_critical_damage_per_point", 0.03D, 0.0D, 1024.0D);
        builder.pop();

        builder.push("derived_attributes");
        DERIVED_ATTRIBUTES_ENABLED = builder.define("derived_attributes_enabled", true);
        HEALING_AMPLIFICATION_HEALTH_POINTS = builder.defineInRange("healing_amplification_health_points", 5, 1, Integer.MAX_VALUE);
        HEALING_AMPLIFICATION_PER_STEP = builder.defineInRange("healing_amplification_per_step", 0.01D, 0.0D, 1024.0D);
        ARMOR_PIERCE_ATTACK_POINTS = builder.defineInRange("armor_pierce_attack_points", 15, 1, Integer.MAX_VALUE);
        ARMOR_PIERCE_PER_STEP = builder.defineInRange("armor_pierce_per_step", 1.0D, 0.0D, 1024.0D);
        ARMOR_TOUGHNESS_DEFENSE_POINTS = builder.defineInRange("armor_toughness_defense_points", 5, 1, Integer.MAX_VALUE);
        ARMOR_TOUGHNESS_PER_STEP = builder.defineInRange("armor_toughness_per_step", 1.0D, 0.0D, 1024.0D);
        MINIMUM_EFFECTIVE_DAMAGE_ENABLED = builder.comment("是否启用攻击成长提供的最低有效伤害。")
                .define("minimum_effective_damage_enabled", true);
        MINIMUM_EFFECTIVE_DAMAGE_RATIO = builder.comment("成长攻击力转换为最低有效伤害的比例。0.75代表75%。")
                .defineInRange("minimum_effective_damage_ratio", 0.75D, 0.0D, 1024.0D);
        MINIMUM_EFFECTIVE_DAMAGE_WINDOW_TICKS = builder.comment("非近战伤害合并窗口，20 tick为1秒。")
                .defineInRange("minimum_effective_damage_window_ticks", 20, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("perception");
        PERCEPTION_MAX_POINTS = builder.defineInRange("perception_max_points", 10, 0, Integer.MAX_VALUE);
        SUPER_PERCEPTION_UNLOCK_POINTS = builder.defineInRange("super_perception_unlock_points", 5, 0, Integer.MAX_VALUE);
        SUPER_PERCEPTION_DISTANCE = builder.defineInRange("super_perception_distance", 50.0D, 1.0D, 256.0D);
        SUPER_PERCEPTION_ANGLE = builder.defineInRange("super_perception_angle", 45.0D, 1.0D, 180.0D);
        SUPER_PERCEPTION_TARGET_MONSTERS = builder.define("super_perception_target_monsters", true);
        SUPER_PERCEPTION_TARGET_NEUTRAL = builder.define("super_perception_target_neutral", true);
        SUPER_PERCEPTION_TARGET_PASSIVE = builder.define("super_perception_target_passive", false);
        SUPER_PERCEPTION_TARGET_PLAYERS = builder.define("super_perception_target_players", false);
        SUPER_PERCEPTION_FILTER_PETS = builder.define("super_perception_filter_pets", true);
        SUPER_PERCEPTION_WHITELIST = builder.defineList("super_perception_whitelist", List.of(), value -> value instanceof String);
        builder.pop();

        builder.push("nutrition");
        NUTRITION_ENABLED = builder.comment("是否启用营养属性及其能力。")
                .define("nutrition_enabled", true);
        NUTRITION_MAX_POINTS = builder.comment("营养属性点数上限。")
                .defineInRange("nutrition_max_points", 10, 0, Integer.MAX_VALUE);
        NUTRITION_BASE_MAX_FOOD = builder.comment("未投入营养点数时的最大饥饿值。")
                .defineInRange("nutrition_base_max_food", 20, 20, Integer.MAX_VALUE);
        NUTRITION_MAX_FOOD_PER_POINT = builder.comment("每点营养增加的最大饥饿值。")
                .defineInRange("nutrition_max_food_per_point", 2, 0, Integer.MAX_VALUE);
        NUTRITION_FOOD_RESTORE_BONUS_PER_POINT = builder.comment("每点营养增加的食物饥饿值恢复比例。0.2代表20%。")
                .defineInRange("nutrition_food_restore_bonus_per_point", 0.20D, 0.0D, 1024.0D);
        NUTRITION_SATURATION_RESTORE_BONUS_PER_POINT = builder.comment("每点营养增加的食物饱和度恢复比例。0.2代表20%。")
                .defineInRange("nutrition_saturation_restore_bonus_per_point", 0.20D, 0.0D, 1024.0D);
        NUTRITION_DRINK_RESTORE_BONUS_PER_POINT = builder.comment("每点营养增加的LSO饮水恢复比例。0.2代表20%。")
                .defineInRange("nutrition_drink_restore_bonus_per_point", 0.20D, 0.0D, 1024.0D);
        EFFICIENT_METABOLISM_UNLOCK_POINTS = builder.comment("解锁高效代谢所需的当前营养点数；低于阈值时能力暂时失效。")
                .defineInRange("efficient_metabolism_unlock_points", 5, 0, Integer.MAX_VALUE);
        EFFICIENT_METABOLISM_INTERVAL_TICKS = builder.comment("高效代谢每次结算间隔，20 tick为1秒。")
                .defineInRange("efficient_metabolism_interval_ticks", 20, 1, Integer.MAX_VALUE);
        EFFICIENT_METABOLISM_HEAL_MAX_HEALTH_PERCENT = builder.comment("高效代谢每次直接恢复的最大生命值比例。")
                .defineInRange("efficient_metabolism_heal_max_health_percent", 0.015D, 0.0D, 1.0D);
        EFFICIENT_METABOLISM_FOOD_COST = builder.comment("高效代谢每次结算消耗的饥饿值。")
                .defineInRange("efficient_metabolism_food_cost", 1, 0, Integer.MAX_VALUE);
        EFFICIENT_METABOLISM_MIN_FOOD = builder.comment("饥饿值小于或等于此值时停止高效代谢。")
                .defineInRange("efficient_metabolism_min_food", 6, 0, Integer.MAX_VALUE);
        EFFICIENT_METABOLISM_THIRST_COST = builder.comment("启用LSO联动时，高效代谢每次结算消耗的口渴值。")
                .defineInRange("efficient_metabolism_thirst_cost", 0.5D, 0.0D, Double.MAX_VALUE);
        EFFICIENT_METABOLISM_MIN_THIRST_PERCENT = builder.comment("启用LSO联动时，口渴值小于或等于此比例时停止高效代谢。")
                .defineInRange("efficient_metabolism_min_thirst_percent", 0.20D, 0.0D, 1.0D);
        NUTRITION_FULLNESS_UNLOCK_POINTS = builder.comment("解锁营养充盈所需的当前营养点数；低于阈值时能力暂时失效。")
                .defineInRange("nutrition_fullness_unlock_points", 10, 0, Integer.MAX_VALUE);
        NUTRITION_FULLNESS_THRESHOLD = builder.comment("营养充盈所需的饥饿值和口渴值比例。")
                .defineInRange("nutrition_fullness_threshold", 0.80D, 0.0D, 1.0D);
        NUTRITION_FULLNESS_HASTE_LEVEL = builder.comment("营养充盈提供的急迫等级。2代表急迫II。")
                .defineInRange("nutrition_fullness_haste_level", 2, 0, 256);
        NUTRITION_FULLNESS_ABSORPTION_BASE = builder.comment("营养充盈伤害吸收上限的固定部分。")
                .defineInRange("nutrition_fullness_absorption_base", 10.0D, 0.0D, Double.MAX_VALUE);
        NUTRITION_FULLNESS_ABSORPTION_MAX_HEALTH_PERCENT = builder.comment("伤害吸收上限额外增加的最大生命值比例。")
                .defineInRange("nutrition_fullness_absorption_max_health_percent", 0.01D, 0.0D, 1024.0D);
        NUTRITION_FULLNESS_ABSORPTION_REFILL_SECONDS = builder.comment("营养充盈补满伤害吸收的检查间隔。")
                .defineInRange("nutrition_fullness_absorption_refill_seconds", 10, 1, Integer.MAX_VALUE);
        NUTRITION_FULLNESS_COMBAT_DELAY_SECONDS = builder.comment("最后一次实体战斗伤害后，暂停刷新营养充盈护盾的时间。")
                .defineInRange("nutrition_fullness_combat_delay_seconds", 10, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("abilities");
        DAMAGE_ADAPTATION_UNLOCK_POINTS = builder.defineInRange("damage_adaptation_unlock_points", 5, 0, Integer.MAX_VALUE);
        HUNTING_INSTINCT_UNLOCK_POINTS = builder.defineInRange("hunting_instinct_unlock_points", 5, 0, Integer.MAX_VALUE);
        STRESS_EVOLUTION_UNLOCK_POINTS = builder.defineInRange("stress_evolution_unlock_points", 5, 0, Integer.MAX_VALUE);
        ADAPTATION_ENABLED = builder.define("adaptation_enabled", true);
        ADAPTATION_PVP_ENABLED = builder.define("adaptation_pvp_enabled", true);
        ADAPTATION_LV1 = builder.defineInRange("adaptation_lv1", 0.05D, 0.0D, 1.0D);
        ADAPTATION_LV2 = builder.defineInRange("adaptation_lv2", 0.10D, 0.0D, 1.0D);
        ADAPTATION_LV3 = builder.defineInRange("adaptation_lv3", 0.15D, 0.0D, 1.0D);
        ADAPTATION_LV4 = builder.defineInRange("adaptation_lv4", 0.20D, 0.0D, 1.0D);
        ADAPTATION_LV5 = builder.defineInRange("adaptation_lv5", 0.25D, 0.0D, 1.0D);
        ADAPTATION_MAX_LEVEL = builder.defineInRange("adaptation_max_level", 5, 0, 5);
        HUNT_ENABLED = builder.define("hunt_enabled", true);
        HUNTING_DAMAGE_MULTIPLIER = builder.defineInRange("hunt_damage_multiplier", 1.5D, 0.0D, 1024.0D);
        HUNTING_COOLDOWN_SECONDS = builder.defineInRange("hunt_cd_normal", 20, 0, Integer.MAX_VALUE);
        HUNTING_LOW_HEALTH_COOLDOWN_SECONDS = builder.defineInRange("hunt_cd_lowhp", 10, 0, Integer.MAX_VALUE);
        HUNTING_LOW_HEALTH_PERCENT = builder.defineInRange("hunt_lowhp_percent", 0.20D, 0.0D, 1.0D);
        HUNTING_ACTIVE_SECONDS = builder.defineInRange("hunt_active_duration", 10, 1, Integer.MAX_VALUE);
        HUNTING_SPEED_LEVEL = builder.defineInRange("hunt_speed_level", 2, 0, 256);
        HUNTING_JUMP_LEVEL = builder.defineInRange("hunt_jump_level", 2, 0, 256);
        HUNTING_BASE_KNOCKBACK = builder.defineInRange("hunt_base_knockback", 2.0D, 0.0D, 128.0D);
        HUNTING_KNOCKBACK_PER_DAMAGE = builder.defineInRange("hunt_knockback_per_damage", 10.0D, 0.0001D, 1024.0D);
        HUNTING_MAX_KNOCKBACK = builder.defineInRange("hunt_max_knockback", 10.0D, 0.0D, 128.0D);
        HUNTING_ACTIVATION_PARTICLE_COUNT = builder.defineInRange("hunt_activation_smoke_particles", 60, 0, 4096);
        HUNTING_HIT_PARTICLE_COUNT = builder.defineInRange("hunt_hit_particle_count", 36, 0, 1024);
        HUNTING_INTERNAL_IMPACT_ENABLED = builder.define("hunt_internal_impact_enabled", true);
        HUNTING_SHOCK_RADIUS = builder.defineInRange("hunt_shock_radius", 3.0D, 0.0D, 128.0D);
        HUNTING_SHOCK_DAMAGE_MULTIPLIER = builder.defineInRange("hunt_shock_damage_multiplier", 0.75D, 0.0D, 1024.0D);
        HUNTING_INJURY_DURATION_SECONDS = builder.defineInRange("hunt_injury_duration", 5, 0, Integer.MAX_VALUE);
        HUNTING_INJURY_INTERVAL_TICKS = builder.defineInRange("hunt_injury_interval_ticks", 20, 1, Integer.MAX_VALUE);
        HUNTING_INJURY_DAMAGE_MULTIPLIER = builder.defineInRange("hunt_injury_damage_multiplier", 0.05D, 0.0D, 1024.0D);
        HUNTING_VULNERABILITY_BONUS = builder.defineInRange("hunt_vulnerability_bonus", 0.10D, 0.0D, 1024.0D);
        EVOLUTION_ENABLED = builder.define("evolution_enabled", true);
        BATTLE_INSTINCT_ENABLED = builder.define("battle_instinct_enabled", true);
        BATTLE_INSTINCT_UNLOCK_POINTS = builder.defineInRange("battle_instinct_unlock_points", 10, 0, Integer.MAX_VALUE);
        BATTLE_INSTINCT_MAX_RESERVES = builder.defineInRange("battle_instinct_max_reserves", 5, 0, Integer.MAX_VALUE);
        BATTLE_INSTINCT_RECOVER_SECONDS = builder.defineInRange("battle_instinct_recover_seconds", 60, 1, Integer.MAX_VALUE);
        BATTLE_INSTINCT_LOW_HEALTH_RECOVER_SECONDS = builder.defineInRange("battle_instinct_low_health_recover_seconds", 30, 1, Integer.MAX_VALUE);
        BATTLE_INSTINCT_LOW_HEALTH_PERCENT = builder.defineInRange("battle_instinct_low_health_percent", 0.20D, 0.0D, 1.0D);
        BATTLE_INSTINCT_INVULNERABILITY_TICKS = builder.defineInRange("battle_instinct_invulnerability_ticks", 10, 0, Integer.MAX_VALUE);
        BATTLE_INSTINCT_COUNTER_DAMAGE_MULTIPLIER = builder.defineInRange("battle_instinct_counter_damage_multiplier", 1.20D, 0.0D, 1024.0D);
        BATTLE_INSTINCT_COUNTER_RANGE_PADDING = builder.defineInRange("battle_instinct_counter_range_padding", 0.25D, 0.0D, 16.0D);
        BATTLE_INSTINCT_COUNTER_KILL_RESERVES = builder.defineInRange("battle_instinct_counter_kill_reserves", 1, 0, 1);
        BATTLE_INSTINCT_BLOCK_POSE_TICKS = builder.defineInRange("battle_instinct_block_pose_ticks", 6, 0, 100);
        BATTLE_INSTINCT_TRAIL_TICKS = builder.defineInRange("battle_instinct_trail_ticks", 8, 1, 100);
        BATTLE_INSTINCT_BLOCK_SOUND_VOLUME = builder.defineInRange("battle_instinct_block_sound_volume", 0.6D, 0.0D, 4.0D);
        BATTLE_INSTINCT_BLOCK_SOUND_PITCH = builder.defineInRange("battle_instinct_block_sound_pitch", 1.2D, 0.01D, 4.0D);
        STRESS_COOLDOWN_SECONDS = builder.defineInRange("evolution_cd", 120, 0, Integer.MAX_VALUE);
        STRESS_DAMAGE_COOLDOWN_REDUCTION_SECONDS = builder.defineInRange("evolution_cd_reduce", 1, 0, Integer.MAX_VALUE);
        STRESS_DAMAGE_COOLDOWN_REDUCTION_INTERVAL_SECONDS = builder.defineInRange("evolution_cd_reduce_interval", 1, 0, Integer.MAX_VALUE);
        STRESS_SHOCK_RANGE = builder.defineInRange("evolution_radius", 8.0D, 0.0D, 128.0D);
        STRESS_UTILITY_EFFECT_SECONDS = builder.defineInRange("evolution_utility_effect_duration", 10, 0, Integer.MAX_VALUE);
        STRESS_RESISTANCE_SECONDS = builder.defineInRange("evolution_resistance_duration", 8, 0, Integer.MAX_VALUE);
        STRESS_NORMAL_SLOW_LEVEL = builder.defineInRange("evolution_slow_level", 2, 1, 255);
        STRESS_NORMAL_SLOW_SECONDS = builder.defineInRange("evolution_slow_duration", 3, 0, Integer.MAX_VALUE);
        STRESS_ENHANCED_SLOW_LEVEL = builder.defineInRange("evolution_strong_slow_level", 4, 1, 255);
        STRESS_ENHANCED_SLOW_SECONDS = builder.defineInRange("evolution_strong_slow_duration", 4, 0, Integer.MAX_VALUE);
        STRESS_ENHANCED_KNOCKBACK = builder.defineInRange("evolution_strong_knockback_max", 5.0D, 0.0D, 128.0D);
        STRESS_NORMAL_KNOCKBACK = builder.defineInRange("evolution_knockback", 2.0D, 0.0D, 128.0D);
        STRESS_VISUAL_EXPLOSION_PARTICLES = builder.defineInRange("evolution_visual_explosion_particles", 1, 0, 64);
        STRESS_VISUAL_SHOCKWAVE_PARTICLES = builder.defineInRange("evolution_visual_shockwave_particles", 64, 0, 2048);
        INTIMIDATION_IMMUNITY = builder.defineList("intimidation_immunity",
                List.of("minecraft:wolf", "minecraft:cat", "minecraft:horse", "minecraft:parrot"),
                value -> value instanceof String);
        builder.pop();

        builder.push("diagnostics");
        RUNTIME_DIAGNOSTICS_ENABLED = builder.comment("记录带DS-DIAG前缀的限频运行诊断，便于定位UI、网络和能力异常。")
                .define("runtime_diagnostics_enabled", true);
        builder.pop();

        SPEC = builder.build();
    }

    private DarwinConfig() {
    }

    public static double adaptationReductionForLevel(int level) {
        return AdaptationRules.reductionForLevel(level, ADAPTATION_MAX_LEVEL.get(),
                ADAPTATION_LV1.get(), ADAPTATION_LV2.get(), ADAPTATION_LV3.get(),
                ADAPTATION_LV4.get(), ADAPTATION_LV5.get());
    }

    public static boolean migrateLegacyDefaults() {
        if (CONFIG_VERSION.get() >= CURRENT_CONFIG_VERSION) {
            return false;
        }

        replaceLegacyDefault(ADAPTATION_LV1, 0.10D, 0.05D);
        replaceLegacyDefault(ADAPTATION_LV2, 0.20D, 0.10D);
        replaceLegacyDefault(ADAPTATION_LV3, 0.30D, 0.15D);
        if (ADAPTATION_MAX_LEVEL.get() == 3) {
            ADAPTATION_MAX_LEVEL.set(5);
        }
        replaceLegacyDefault(HUNTING_VULNERABILITY_BONUS, 0.15D, 0.10D);
        CONFIG_VERSION.set(CURRENT_CONFIG_VERSION);
        CONFIG_VERSION.save();
        return true;
    }

    private static void replaceLegacyDefault(ForgeConfigSpec.DoubleValue value, double legacy, double replacement) {
        if (Double.compare(value.get(), legacy) == 0) {
            value.set(replacement);
        }
    }
}
