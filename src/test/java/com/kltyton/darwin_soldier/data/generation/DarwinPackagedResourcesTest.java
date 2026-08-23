package com.kltyton.darwin_soldier.data.generation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarwinPackagedResourcesTest {
    private static final String[] DAMAGE_TYPES = {
            "hunting_shock",
            "internal_injury",
            "minimum_damage_correction"
    };
    private static final String GUI_STYLESHEET =
            "assets/apricityui/apricity/darwin_soldier/styles/darwin.css";
    private static final Pattern GLOBAL_BOLD_RULE = Pattern.compile(
            "(?s)\\.ore-theme\\.darwin-screen\\s*,\\s*\\.ore-theme\\.darwin-screen\\s+\\*\\s*\\{"
                    + "[^}]*font-weight\\s*:\\s*(?:bold|700)");

    @Test
    void packagedDamageTypesExistBeforeServerTickUsesThem() {
        ClassLoader loader = getClass().getClassLoader();
        for (String damageType : DAMAGE_TYPES) {
            String path = "data/darwin_soldier/damage_type/" + damageType + ".json";
            assertNotNull(loader.getResource(path),
                    () -> "Missing packaged damage type would crash the server tick: " + path);
        }
    }

    @Test
    void sharedGuiStylesRenderEveryPageInBold() throws IOException {
        ClassLoader loader = getClass().getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(GUI_STYLESHEET)) {
            assertNotNull(stream, "Missing shared Darwin Soldier GUI stylesheet");
            String css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(GLOBAL_BOLD_RULE.matcher(css).find(),
                    "The shared GUI root and descendants must use a bold font weight");
        }
    }

    @Test
    void runtimeConfigCategoriesHaveTranslationsInBothLanguages() throws IOException {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            String path = "assets/darwin_soldier/lang/" + language + ".json";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing language resource: " + path);
                String translations = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(translations.contains("\"config.darwin_soldier.category.abilities\""),
                        "Missing abilities category translation in " + language);
            }
        }
    }
}
