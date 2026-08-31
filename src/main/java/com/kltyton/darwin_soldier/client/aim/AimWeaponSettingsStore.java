package com.kltyton.darwin_soldier.client.aim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.client.aim.ballistics.ProjectileBallistics;
import com.kltyton.darwin_soldier.diagnostic.RuntimeDiagnostics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AimWeaponSettingsStore {
    private static final int SCHEMA_VERSION = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, AimWeaponSettings> SETTINGS = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean dirty;

    private AimWeaponSettingsStore() {
    }

    public static synchronized ResourceLocation weaponId(ItemStack stack) {
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static synchronized AimWeaponSettings get(ItemStack stack) {
        ResourceLocation weaponId = weaponId(stack);
        return weaponId == null ? AimWeaponSettings.defaults() : get(weaponId);
    }

    public static synchronized AimWeaponSettings get(ResourceLocation weaponId) {
        ensureLoaded();
        return SETTINGS.getOrDefault(weaponId.toString(), AimWeaponSettings.defaults());
    }

    public static synchronized void put(ResourceLocation weaponId, AimWeaponSettings settings) {
        ensureLoaded();
        AimWeaponSettings sanitized = settings.sanitized();
        SETTINGS.put(weaponId.toString(), sanitized);
        dirty = true;
        RuntimeDiagnostics.infoRateLimited("aim-store-put-" + weaponId, 500L, "aim_settings_update",
                () -> "weapon=" + weaponId + " mode=" + sanitized.mode()
                        + " active=" + sanitized.activeTuning()
                        + " trajectory=" + sanitized.trajectoryVisible());
    }

    public static synchronized void putBallistics(ResourceLocation weaponId, ProjectileBallistics ballistics) {
        AimWeaponSettings updated = get(weaponId).withBallistics(ballistics);
        put(weaponId, updated);
        flush();
    }

    public static synchronized void clearBallistics(ResourceLocation weaponId) {
        put(weaponId, get(weaponId).withBallistics(null));
    }

    public static synchronized void reset(ResourceLocation weaponId) {
        ensureLoaded();
        SETTINGS.remove(weaponId.toString());
        dirty = true;
        RuntimeDiagnostics.info("aim_settings_reset", "weapon=" + weaponId + " remainingEntries=" + SETTINGS.size());
    }

    public static synchronized void flush() {
        if (!dirty) {
            return;
        }
        Path file = settingsFile();
        JsonObject weapons = new JsonObject();
        SETTINGS.forEach((id, settings) -> weapons.add(id, encodeSettings(settings)));
        JsonObject root = new JsonObject();
        root.addProperty("version", SCHEMA_VERSION);
        root.add("weapons", weapons);

        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            RuntimeDiagnostics.info("aim_settings_saved", "path=" + file.toAbsolutePath()
                    + " schema=" + SCHEMA_VERSION + " entries=" + SETTINGS.size());
        } catch (IOException exception) {
            RuntimeDiagnostics.error("aim_settings_save_failed", "path=" + file.toAbsolutePath(), exception);
        }
    }

    private static JsonObject encodeSettings(AimWeaponSettings settings) {
        JsonObject entry = new JsonObject();
        entry.addProperty("mode", settings.mode().name());
        entry.addProperty("trajectoryVisible", settings.trajectoryVisible());
        entry.add("manual", encodeTuning(settings.manual()));
        entry.add("adaptive", encodeTuning(settings.adaptive()));
        if (settings.ballistics() != null) {
            JsonObject ballistics = new JsonObject();
            ballistics.addProperty("type", settings.ballistics().projectileType());
            ballistics.addProperty("speed", settings.ballistics().speed());
            ballistics.addProperty("gravity", settings.ballistics().gravity());
            ballistics.addProperty("hitscan", settings.ballistics().hitscan());
            entry.add("ballistics", ballistics);
        }
        return entry;
    }

    private static JsonObject encodeTuning(AimWeaponTuning tuning) {
        JsonObject value = new JsonObject();
        value.addProperty("lead", tuning.leadMultiplier());
        value.addProperty("drop", tuning.dropMultiplier());
        value.addProperty("smooth", tuning.smooth());
        return value;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = settingsFile();
        if (!Files.isRegularFile(file)) {
            RuntimeDiagnostics.info("aim_settings_load", "path=" + file.toAbsolutePath() + " exists=false entries=0");
            return;
        }
        boolean persistMigration = false;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            int storedSchemaVersion = readInt(root, "version", 1);
            JsonObject weapons = object(root, "weapons");
            for (Map.Entry<String, JsonElement> entry : weapons.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id != null && entry.getValue().isJsonObject()) {
                    SETTINGS.put(id.toString(), decodeSettings(entry.getValue().getAsJsonObject(), storedSchemaVersion));
                }
            }
            RuntimeDiagnostics.info("aim_settings_load", "path=" + file.toAbsolutePath()
                    + " exists=true schema=" + storedSchemaVersion + " entries=" + SETTINGS.size());
            if (storedSchemaVersion < SCHEMA_VERSION) {
                dirty = true;
                RuntimeDiagnostics.info("aim_settings_migrate", "path=" + file.toAbsolutePath()
                        + " fromSchema=" + storedSchemaVersion + " toSchema=" + SCHEMA_VERSION
                        + " trajectory=false mode=MANUAL entries=" + SETTINGS.size());
                persistMigration = true;
            }
        } catch (IOException | IllegalStateException | NumberFormatException exception) {
            preserveMalformedFile();
            SETTINGS.clear();
            RuntimeDiagnostics.error("aim_settings_load_failed", "path=" + file.toAbsolutePath(), exception);
        }
        if (persistMigration) {
            flush();
        }
    }

    static AimWeaponSettings decodeSettings(JsonObject value, int storedSchemaVersion) {
        AimWeaponTuning manual = value.has("manual")
                ? decodeTuning(object(value, "manual"), AimWeaponTuning.defaults())
                : decodeTuning(value, AimWeaponTuning.defaults());
        AimWeaponTuning adaptive = decodeTuning(object(value, "adaptive"), AimWeaponTuning.adaptiveDefaults());
        ProjectileBallistics ballistics = null;
        if (value.has("ballistics") && value.get("ballistics").isJsonObject()) {
            JsonObject learned = value.getAsJsonObject("ballistics");
            boolean hitscan = readBoolean(learned, "hitscan", false);
            ballistics = hitscan ? ProjectileBallistics.hitscanProfile() : ProjectileBallistics.detected(
                    readString(learned, "type", "minecraft:generic_projectile"),
                    readDouble(learned, "speed", ProjectileBallistics.REFERENCE_SPEED),
                    readDouble(learned, "gravity", ProjectileBallistics.REFERENCE_GRAVITY));
        }
        boolean legacyActivation = storedSchemaVersion < SCHEMA_VERSION;
        return new AimWeaponSettings(
                legacyActivation
                        ? AimMode.MANUAL
                        : AimMode.parse(readString(value, "mode", AimMode.MANUAL.name())),
                manual,
                adaptive,
                !legacyActivation && readBoolean(value, "trajectoryVisible", false),
                ballistics
        ).sanitized();
    }

    private static AimWeaponTuning decodeTuning(JsonObject value, AimWeaponTuning defaults) {
        return new AimWeaponTuning(
                readDouble(value, "lead", defaults.leadMultiplier()),
                readDouble(value, "drop", defaults.dropMultiplier()),
                readDouble(value, "smooth", defaults.smooth())
        ).sanitized();
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static double readDouble(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsDouble() : fallback;
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt() : fallback;
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean() : fallback;
    }

    private static String readString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString() : fallback;
    }

    private static void preserveMalformedFile() {
        Path file = settingsFile();
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // The original file remains untouched when a backup cannot be created.
        }
    }

    private static Path settingsFile() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Darwin_soldier.MODID)
                .resolve("aim_weapon_settings.json");
    }
}
