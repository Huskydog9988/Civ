package vg.civcraft.mc.civmodcore.config;

import java.util.Map;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * Class is intended to be used by implementations of {@link ConfigurationSerializable} during the deserialization
 * process.
 */
public class ConfigSection extends MemoryConfiguration {

    /**
     * @param data The data to create a new ConfigSection from.
     * @return Returns a new ConfigSection.
     */
    public static ConfigSection fromData(@NotNull final Map<String, Object> data) {
        final var section = new ConfigSection();
        for (var entry : data.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
        return section;
    }

}
