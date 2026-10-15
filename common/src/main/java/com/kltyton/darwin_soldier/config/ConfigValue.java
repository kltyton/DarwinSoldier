package com.kltyton.darwin_soldier.config;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** A typed value backed by the active Loader's native configuration. */
public final class ConfigValue<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Runnable saver;
    private final Supplier<T> defaults;
    private final List<String> path;
    private final Function<String, T> parser;
    private final Supplier<Validation> validation;

    public ConfigValue(Supplier<T> getter, Consumer<T> setter, Runnable saver, Supplier<T> defaults,
                       List<String> path, Function<String, T> parser, Supplier<Validation> validation) {
        this.getter = getter;
        this.setter = setter;
        this.saver = saver;
        this.defaults = defaults;
        this.path = List.copyOf(path);
        this.parser = parser;
        this.validation = validation;
    }

    public T get() { return getter.get(); }
    public void set(T value) { setter.accept(value); }
    public void save() { saver.run(); }
    public T getDefault() { return defaults.get(); }
    public List<String> getPath() { return path; }
    public Comparable<?> minimum() { return validation.get().minimum(); }
    public Comparable<?> maximum() { return validation.get().maximum(); }

    public Update<T> parse(String raw) {
        T parsed = parser.apply(raw);
        if (!validation.get().predicate().test(parsed)) throw new IllegalArgumentException("Invalid configuration value");
        return new Update<>(this, parsed);
    }

    public record Validation(Predicate<Object> predicate, Comparable<?> minimum, Comparable<?> maximum) {}

    public record Update<T>(ConfigValue<T> target, T value) {
        public void apply() { target.set(value); }
    }
}
