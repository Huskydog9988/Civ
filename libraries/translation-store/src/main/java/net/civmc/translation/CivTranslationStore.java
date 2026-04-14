package net.civmc.translation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.kyori.adventure.internal.Internals;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.TriState;
import net.kyori.examination.Examinable;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class CivTranslationStore implements Translator {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final String BASE_PREFIX = "civ";
    private static final String[] LANG_FILES = {"en_us.json"};
    public static final @NotNull Locale DEFAULT_LOCALE = Locale.US;

    protected final Map<String, Translation<String>> translations = new ConcurrentHashMap<>();
    private final Logger logger;
    private final Key translatorName;

    public CivTranslationStore(final @NotNull Logger logger, final @NotNull Key name) {
        this.logger = logger;
        this.translatorName = name;
    }

    /**
     * A key identifying this translation source.
     *
     * <p>Intended to be used for display to users.</p>
     *
     * @return an identifier for this translation source
     * @since 4.0.0
     */
    @Override
    public @NotNull Key name() {
        return this.translatorName;
    }

    /**
     * Checks if the translator has any translation
     * @return If there are any translations
     */
    public @NotNull TriState hasAnyTranslations() {
        if (this.translations.isEmpty()) return TriState.FALSE;
        else return TriState.TRUE;
    }

    /**
     * Gets a message format from a key and locale.
     *
     * <p>When used in the {@link GlobalTranslator}, this method is called only if
     * {@link #translate(TranslatableComponent, Locale)} returns {@code null}.</p>
     *
     * @param key    a translation key
     * @param locale a locale
     * @return a message format or {@code null} to skip translation
     */
    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        // this.logger.warn("No translation (MessageFormat) found for key {} and locale {}", key, locale);
        return null;
    }

    @Override
    public final @Nullable Component translate(final @NotNull TranslatableComponent component, final @NotNull Locale locale) {
        final @Nullable Translation<String> translation = this.translations.get(component.key());
        if (translation == null) {
            // this.logger.warn("No translation found for key {} and locale {}", component.key(), locale);
            return null;
        }

        final @Nullable String miniMessageString = translation.translate(locale);
        // this.logger.info("Translating key {} for locale {}: {}", component.key(), locale, miniMessageString);
        if (miniMessageString == null) {
            return null;
        }

        // TODO: handle custom args if we want that
        return mm.deserialize(miniMessageString);
    }

    /**
     * Load all translation files for the calling plugin
     */
    public void loadTranslations() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        // this.logger.info("Loading translations for caller: {}", caller.getName());
        this.loadTranslations(caller);
    }

    /**
     * Load all translation files for the provided plugin
     *
     * @param caller The class to load file from
     */
    public void loadTranslations(final @NotNull Class<?> caller) {
        for (var filePath : LANG_FILES) {
            this.logger.info("Loading translation file: {}", filePath);
            this.loadTranslationFile(caller, "langs/" + filePath);
        }
    }

    /**
     * Attempts to parse the Locale name from the provide file name
     *
     * @param fileName The file name to parse
     * @return The detected Locale
     */
    private @Nullable Locale detectLocale(final @NotNull String fileName) {
        final String localePart = fileName.toLowerCase().replaceAll(".json$", "");
        // TODO: maybe use #parseLocale instead??
        try {
            return Locale.forLanguageTag(localePart.replace('_', '-'));
        } catch (Exception e) {
            this.logger.error("Could not parse locale from file name: " + fileName, e);
            return null;
        }
    }

    /**
     * Load the specified language file from the filePath from the caller's resources
     *
     * @param caller   The calling class
     * @param filePath The path to the translation file, relative to the caller's resources (e.g. "langs/en_us.json")
     */
    private void loadTranslationFile(final @NotNull Class<?> caller, final @NotNull String filePath) {
        var filePathParts = filePath.split("/");
        var fileName = filePathParts[filePathParts.length - 1];
        Locale locale = this.detectLocale(fileName);
        if (locale == null) {
            throw new RuntimeException("Could not detect locale from file name: " + fileName);
        }

        try (InputStream in = caller.getClassLoader().getResourceAsStream(filePath)) {
            if (in == null) {
                this.logger.warn("Translation file not found: {}", filePath);
                return;
            }

            JsonReader reader = new JsonReader(new InputStreamReader(in));
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject())
                throw new JsonParseException("Expected JSON object in translation file " + filePath);
            Map<String, String> flattened = flattenJson(root.getAsJsonObject());

            flattened.forEach((key, value) -> {
                this.translations.computeIfAbsent(key, Translation::new).register(locale, value);
            });
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts standard json structure into a flattened structure
     *
     * @param root The root json object
     * @return A map of the flattened keys to their values
     */
    private static Map<String, String> flattenJson(JsonObject root) {
        Map<String, String> result = new LinkedHashMap<>();
        flattenObject(BASE_PREFIX, root, result);
        return result;
    }

    private static void flattenObject(String prefix, JsonObject obj, Map<String, String> result) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = prefix.isEmpty()
                ? entry.getKey()
                : prefix + "." + entry.getKey();

            JsonElement value = entry.getValue();

            if (value.isJsonObject()) {
                flattenObject(key, value.getAsJsonObject(), result);
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                result.put(key, value.getAsString());
            } else {
                throw new IllegalArgumentException(
                    "Invalid JSON: only string values are allowed at key = " + key
                );
            }
        }
    }

    // basically a copy of the Translation class from AbstractTranslationStore
    protected final class Translation<T> implements Examinable {

        private final String key;
        private final Map<Locale, T> translations;

        private Translation(final @NotNull String key) {
            this.key = requireNonNull(key, "key");
            this.translations = new ConcurrentHashMap<>();
        }

        private @Nullable T translate(final @NotNull Locale locale) {
            T format = this.translations.get(requireNonNull(locale, "locale"));
            if (format == null) {
                format = this.translations.get(Locale.of(locale.getLanguage())); // try without country
                if (format == null) {
                    format = this.translations.get(CivTranslationStore.this.DEFAULT_LOCALE); // try local default locale
                }
            }
            return format;
        }

        private void register(final @NotNull Locale locale, final @NotNull T translation) {
            CivTranslationStore.this.logger.info("Registering translation for key {} and locale {}", this.key, locale);
            if (this.translations.putIfAbsent(requireNonNull(locale, "locale"), requireNonNull(translation, "translation")) != null) {
                throw new IllegalArgumentException(String.format("Translation already exists: %s for %s", this.key, locale));
            }
        }

        @Override
        public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
            return Stream.of(
                ExaminableProperty.of("key", this.key),
                ExaminableProperty.of("translations", this.translations)
            );
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) return true;
            if (!(other instanceof Translation)) return false;
            final Translation<?> that = (Translation<?>) other;
            return this.key.equals(that.key) &&
                this.translations.equals(that.translations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.key, this.translations);
        }

        @Override
        public String toString() {
            return Internals.toString(this);
        }
    }
}
