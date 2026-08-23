package com.kltyton.darwin_soldier.command;

import com.kltyton.darwin_soldier.data.AbilityType;
import com.kltyton.darwin_soldier.data.AdaptationRecord;
import com.kltyton.darwin_soldier.data.GrowthAttributes;
import com.kltyton.darwin_soldier.data.GrowthSavedData;
import com.kltyton.darwin_soldier.data.PlayerGrowthData;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import com.kltyton.darwin_soldier.network.ModNetwork;
import com.kltyton.darwin_soldier.nutrition.NutritionFood;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class DarwinCommand {
    private static final String[] GROWTH_FIELDS = {"current", "total"};
    private static final String[] POINT_FIELDS = {
            "total",
            "health",
            "attack",
            "defense",
            "perception",
            "nutrition",
            "cumulative_health",
            "cumulative_attack",
            "cumulative_defense",
            "cumulative_perception",
            "cumulative_nutrition"
    };
    private static final String[] ABILITY_IDS = {
            "damage_adaptation",
            "adaptation",
            "hunting_instinct",
            "hunt",
            "hunting",
            "hunting_internal_impact",
            "internal_impact",
            "impact",
            "stress_evolution",
            "stress",
            "super_perception",
            "perception",
            "battle_instinct",
            "battle",
            "instinct",
            "combat_instinct",
            "efficient_metabolism",
            "metabolism",
            "nutrition_fullness",
            "nutrition",
            "fullness"
    };
    private static final String[] ADAPTATION_KEY_EXAMPLES = {
            "entity:minecraft:zombie",
            "entity:minecraft:skeleton",
            "entity:minecraft:creeper",
            "entity:minecraft:enderman",
            "entity:minecraft:ender_dragon",
            "entity:minecraft:wither",
            "player:<uuid>"
    };
    private static final String[] GROWTH_AMOUNT_EXAMPLES = {"0", "1", "10", "200", "1000"};
    private static final String[] POINT_VALUE_EXAMPLES = {"0", "1", "5", "10", "50", "100"};
    private static final String[] COOLDOWN_SECOND_EXAMPLES = {"0", "10", "20", "60", "120"};

    private DarwinCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRoot("darwin"));
        dispatcher.register(buildRoot("darwin_soldier"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildRoot(String name) {
        var root = Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enabled")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> apply(context.getSource(), EntityArgument.getPlayers(context, "targets"), data -> {
                                            data.setEnabled(BoolArgumentType.getBool(context, "value"));
                                        }, true)))))
                .then(Commands.literal("growth")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                                                .suggests(DarwinCommand::suggestGrowthAmounts)
                                                .executes(context -> apply(context.getSource(), EntityArgument.getPlayers(context, "targets"), data -> {
                                                    data.addGrowth(DoubleArgumentType.getDouble(context, "amount"));
                                                }, true)))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("field", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestGrowthFields)
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D))
                                                        .suggests(DarwinCommand::suggestGrowthAmounts)
                                                        .executes(context -> setGrowth(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "field"), DoubleArgumentType.getDouble(context, "value"))))))))
                .then(Commands.literal("points")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("field", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestPointFields)
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .suggests(DarwinCommand::suggestPointValues)
                                                        .executes(context -> setPoints(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "field"), IntegerArgumentType.getInteger(context, "value"))))))))
                .then(Commands.literal("ability")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestAbilities)
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setAbilityUnlock(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "ability"), BoolArgumentType.getBool(context, "value")))))))
                        .then(Commands.literal("enabled")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestAbilities)
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setAbilityEnabled(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "ability"), BoolArgumentType.getBool(context, "value")))))))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestCooldownAbilities)
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                                        .suggests(DarwinCommand::suggestCooldownSeconds)
                                                        .executes(context -> setCooldown(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "ability"), IntegerArgumentType.getInteger(context, "seconds"))))))))
                .then(Commands.literal("adaptation")
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> apply(context.getSource(), EntityArgument.getPlayers(context, "targets"), PlayerGrowthData::clearAdaptationRecords, false))))
                        .then(Commands.literal("level")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestAdaptationKeys)
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                        .suggests(DarwinCommand::suggestAdaptationLevels)
                                                        .executes(context -> setAdaptationLevel(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "key"), IntegerArgumentType.getInteger(context, "level")))))))
                        .then(Commands.literal("enabled")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .suggests(DarwinCommand::suggestAdaptationKeys)
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setAdaptationEnabled(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "key"), BoolArgumentType.getBool(context, "value"))))))));
        return root;
    }

    private static int setGrowth(CommandSourceStack source, Collection<ServerPlayer> players, String field, double value) {
        if (!"current".equals(field) && !"total".equals(field)) {
            source.sendFailure(Component.translatable("commands.darwin_soldier.invalid_growth_field"));
            return 0;
        }
        return apply(source, players, data -> {
            switch (field) {
                case "current" -> data.setCurrentGrowth(value);
                case "total" -> data.setTotalGrowth(value);
                default -> {
                }
            }
        }, true);
    }

    private static int setPoints(CommandSourceStack source, Collection<ServerPlayer> players, String field, int value) {
        if (!"total".equals(field) && !"health".equals(field) && !"attack".equals(field) && !"defense".equals(field)
                && !"perception".equals(field) && !"cumulative_health".equals(field) && !"cumulative_attack".equals(field)
                && !"nutrition".equals(field) && !"cumulative_defense".equals(field)
                && !"cumulative_perception".equals(field) && !"cumulative_nutrition".equals(field)) {
            source.sendFailure(Component.translatable("commands.darwin_soldier.invalid_points_field"));
            return 0;
        }
        return apply(source, players, data -> {
            switch (field) {
                case "total" -> data.setTotalPoints(value);
                case "health" -> data.setHealthPointsDebug(value);
                case "attack" -> data.setAttackPointsDebug(value);
                case "defense" -> data.setDefensePointsDebug(value);
                case "perception" -> data.setPerceptionPointsDebug(value);
                case "nutrition" -> data.setNutritionPointsDebug(value);
                case "cumulative_health" -> data.setCumulativeHealthPoints(value);
                case "cumulative_attack" -> data.setCumulativeAttackPoints(value);
                case "cumulative_defense" -> data.setCumulativeDefensePoints(value);
                case "cumulative_perception" -> data.setCumulativePerceptionPoints(value);
                case "cumulative_nutrition" -> data.setCumulativeNutritionPoints(value);
                default -> {
                }
            }
        }, true);
    }

    private static int setAbilityUnlock(CommandSourceStack source, Collection<ServerPlayer> players, String abilityId, boolean value) {
        AbilityType ability = parseAbility(source, abilityId);
        if (ability == null) {
            return 0;
        }
        return apply(source, players, data -> data.setAbilityUnlocked(ability, value), false);
    }

    private static int setAbilityEnabled(CommandSourceStack source, Collection<ServerPlayer> players, String abilityId, boolean value) {
        AbilityType ability = parseAbility(source, abilityId);
        if (ability == null) {
            return 0;
        }
        return apply(source, players, data -> {
            switch (ability) {
                case DAMAGE_ADAPTATION -> data.setDamageAdaptationEnabled(value);
                case HUNTING_INSTINCT -> data.setHuntingInstinctEnabled(value);
                case HUNTING_INTERNAL_IMPACT -> data.setHuntingInternalImpactEnabled(value);
                case STRESS_EVOLUTION -> data.setStressEvolutionEnabled(value);
                case SUPER_PERCEPTION -> data.setSuperPerceptionEnabled(value);
                case BATTLE_INSTINCT -> data.setBattleInstinctEnabled(value);
                case EFFICIENT_METABOLISM -> data.getNutrition().setEfficientMetabolismEnabled(value);
                case NUTRITION_FULLNESS -> data.getNutrition().setNutritionFullnessEnabled(value);
            }
        }, false);
    }

    private static int setCooldown(CommandSourceStack source, Collection<ServerPlayer> players, String abilityId, int seconds) {
        AbilityType ability = parseAbility(source, abilityId);
        if (ability == null) {
            return 0;
        }
        return apply(source, players, (player, data) -> {
            long until = player.serverLevel().getGameTime() + seconds * 20L;
            switch (ability) {
                case HUNTING_INSTINCT -> data.setHuntingInstinctCooldownUntil(until);
                case HUNTING_INTERNAL_IMPACT -> {
                }
                case STRESS_EVOLUTION -> data.setStressEvolutionCooldownUntil(until);
                case DAMAGE_ADAPTATION -> {
                }
                case SUPER_PERCEPTION -> {
                }
                case BATTLE_INSTINCT -> {
                }
                case EFFICIENT_METABOLISM -> {
                }
                case NUTRITION_FULLNESS -> {
                }
            }
        }, false);
    }

    private static int setAdaptationLevel(CommandSourceStack source, Collection<ServerPlayer> players, String key, int level) {
        return apply(source, players, data -> {
            AdaptationRecord record = data.getOrCreateAdaptationRecord(key, displayFromKey(key), idFromKey(key), key.startsWith("player:"));
            record.setLevel(level);
        }, false);
    }

    private static int setAdaptationEnabled(CommandSourceStack source, Collection<ServerPlayer> players, String key, boolean value) {
        return apply(source, players, data -> {
            AdaptationRecord record = data.getOrCreateAdaptationRecord(key, displayFromKey(key), idFromKey(key), key.startsWith("player:"));
            record.setEnabled(value);
        }, false);
    }

    private static int apply(CommandSourceStack source, Collection<ServerPlayer> players, DataEdit edit, boolean applyAttributes) {
        return apply(source, players, (player, data) -> edit.apply(data), applyAttributes);
    }

    private static int apply(CommandSourceStack source, Collection<ServerPlayer> players, PlayerDataEdit edit, boolean applyAttributes) {
        for (ServerPlayer player : players) {
            GrowthSavedData savedData = GrowthSavedData.get(player);
            PlayerGrowthData data = savedData.getOrCreate(player.getUUID());
            edit.apply(player, data);
            if (applyAttributes) {
                GrowthAttributes.apply(player, data);
            }
            NutritionFood.clampToMaximum(player, data);
            savedData.setDirty();
            ModNetwork.syncTo(player, data);
        }
        source.sendSuccess(() -> Component.translatable("commands.darwin_soldier.updated", players.size()), true);
        return players.size();
    }

    private static AbilityType parseAbility(CommandSourceStack source, String abilityId) {
        try {
            return AbilityType.byId(abilityId);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("commands.darwin_soldier.invalid_ability", abilityId));
            return null;
        }
    }

    private static String displayFromKey(String key) {
        return idFromKey(key);
    }

    private static String idFromKey(String key) {
        int index = key.indexOf(':');
        if (index < 0) {
            return key;
        }
        int second = key.indexOf(':', index + 1);
        return second < 0 ? key.substring(index + 1) : key.substring(index + 1);
    }

    private static CompletableFuture<Suggestions> suggestGrowthFields(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(GROWTH_FIELDS, builder);
    }

    private static CompletableFuture<Suggestions> suggestPointFields(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(POINT_FIELDS, builder);
    }

    private static CompletableFuture<Suggestions> suggestAbilities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ABILITY_IDS, builder);
    }

    private static CompletableFuture<Suggestions> suggestCooldownAbilities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{"hunting_instinct", "hunt", "hunting", "stress_evolution", "stress"}, builder);
    }

    private static CompletableFuture<Suggestions> suggestAdaptationKeys(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Set<String> suggestions = new LinkedHashSet<>();
        try {
            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                PlayerGrowthData data = GrowthSavedData.get(player).getOrCreate(player.getUUID());
                data.getAdaptationRecords().stream()
                        .map(AdaptationRecord::getKey)
                        .forEach(suggestions::add);
            }
        } catch (CommandSyntaxException ignored) {
            // Target suggestions are provided by EntityArgument before this argument is parsed.
        }
        for (String example : ADAPTATION_KEY_EXAMPLES) {
            suggestions.add(example);
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static CompletableFuture<Suggestions> suggestAdaptationLevels(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        int maxLevel = Math.max(0, DarwinConfig.ADAPTATION_MAX_LEVEL.get());
        String[] levels = new String[maxLevel + 1];
        for (int i = 0; i <= maxLevel; i++) {
            levels[i] = Integer.toString(i);
        }
        return SharedSuggestionProvider.suggest(levels, builder);
    }

    private static CompletableFuture<Suggestions> suggestGrowthAmounts(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(GROWTH_AMOUNT_EXAMPLES, builder);
    }

    private static CompletableFuture<Suggestions> suggestPointValues(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(POINT_VALUE_EXAMPLES, builder);
    }

    private static CompletableFuture<Suggestions> suggestCooldownSeconds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(COOLDOWN_SECOND_EXAMPLES, builder);
    }

    @FunctionalInterface
    private interface DataEdit {
        void apply(PlayerGrowthData data);
    }

    @FunctionalInterface
    private interface PlayerDataEdit {
        void apply(ServerPlayer player, PlayerGrowthData data);
    }
}
