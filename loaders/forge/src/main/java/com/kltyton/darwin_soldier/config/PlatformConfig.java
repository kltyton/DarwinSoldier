package com.kltyton.darwin_soldier.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Keeps Forge's config registration, validation and persistence native. */
public final class PlatformConfig {
    private final ForgeConfigSpec nativeSpec;
    private final List<ConfigValue<?>> values;

    private PlatformConfig(ForgeConfigSpec nativeSpec, List<ConfigValue<?>> values) {
        this.nativeSpec = nativeSpec;
        this.values = List.copyOf(values);
    }

    public ForgeConfigSpec nativeSpec() { return nativeSpec; }
    public List<ConfigValue<?>> values() { return values; }
    public void save() { nativeSpec.save(); }
    public void correct(CommentedConfig config) { nativeSpec.correct(config); }
    public void setConfig(CommentedConfig config) { nativeSpec.setConfig(config); }

    public static final class Builder {
        private final ForgeConfigSpec.Builder delegate = new ForgeConfigSpec.Builder();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private ForgeConfigSpec spec;

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
        private <T> ConfigValue<T> bind(ForgeConfigSpec.ConfigValue<T> nativeValue, Function<String, T> parser) {
            ConfigValue<T> value = new ConfigValue<>(nativeValue::get, nativeValue::set, nativeValue::save,
                    nativeValue::getDefault, nativeValue.getPath(), parser, () -> {
                        ForgeConfigSpec.ValueSpec metadata = spec.getSpec().getRaw(nativeValue.getPath());
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
