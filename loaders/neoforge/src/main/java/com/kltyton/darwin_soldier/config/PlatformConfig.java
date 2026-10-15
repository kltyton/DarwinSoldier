package com.kltyton.darwin_soldier.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Keeps NeoForge's config registration, validation and persistence native. */
public final class PlatformConfig {
    private final ModConfigSpec nativeSpec;
    private final List<ConfigValue<?>> values;

    private PlatformConfig(ModConfigSpec nativeSpec, List<ConfigValue<?>> values) {
        this.nativeSpec = nativeSpec;
        this.values = List.copyOf(values);
    }

    public ModConfigSpec nativeSpec() { return nativeSpec; }
    public List<ConfigValue<?>> values() { return values; }
    public void save() { nativeSpec.save(); }

    public static final class Builder {
        private final ModConfigSpec.Builder delegate = new ModConfigSpec.Builder();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private ModConfigSpec spec;

        public Builder push(String category) { delegate.push(category); return this; }
        public Builder pop() { delegate.pop(); return this; }
        public Builder comment(String text) { delegate.comment(text); return this; }
        public ConfigValue<Boolean> define(String key, boolean value) {
            return bind(delegate.define(key, value), Boolean::parseBoolean);
        }
        public ConfigValue<Integer> defineInRange(String key, int value, int min, int max) {
            return bind(delegate.defineInRange(key, value, min, max), raw -> Integer.parseInt(raw.trim()));
        }
        public ConfigValue<Double> defineInRange(String key, double value, double min, double max) {
            return bind(delegate.defineInRange(key, value, min, max), raw -> Double.parseDouble(raw.trim()));
        }
        public ConfigValue<List<? extends String>> defineList(String key, List<? extends String> value,
                                                              Predicate<Object> validator) {
            return bind(delegate.defineList(key, value, validator), raw ->
                    List.of(raw.split("[\\r\\n,]+")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
        private <T> ConfigValue<T> bind(ModConfigSpec.ConfigValue<T> nativeValue, Function<String, T> parser) {
            ConfigValue<T> value = new ConfigValue<>(nativeValue::get, nativeValue::set, nativeValue::save,
                    nativeValue::getDefault, nativeValue.getPath(), parser, () -> {
                        ModConfigSpec.ValueSpec metadata = spec.getSpec().getRaw(nativeValue.getPath());
                        return new ConfigValue.Validation(metadata::test,
                                metadata.getRange() == null ? null : metadata.getRange().getMin(),
                                metadata.getRange() == null ? null : metadata.getRange().getMax());
                    });
            values.add(value);
            return value;
        }
        public PlatformConfig build() {
            spec = delegate.build();
            return new PlatformConfig(spec, values);
        }
    }
}
