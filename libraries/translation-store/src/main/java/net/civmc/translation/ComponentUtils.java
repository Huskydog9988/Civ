package net.civmc.translation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

public class ComponentUtils {
    private static final PlainTextComponentSerializer plainText =  PlainTextComponentSerializer.plainText();

    /**
     * Translates and converts a {@link Component} to plain text
     * @param component The component to render
     * @return Plain text of the component
     */
    public static String stringify(Component component) {
        return plainText.serialize(GlobalTranslator.render(component, CivTranslationStore.DEFAULT_LOCALE));
    }
}
