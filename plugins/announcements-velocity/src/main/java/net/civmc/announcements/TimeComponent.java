package net.civmc.announcements;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public class TimeComponent {

    public static Component replace(Component component, Duration duration) {
        long minutes = Math.ceilDiv(duration.getSeconds(), 60);
        if (!(component instanceof TranslatableComponent translatable)) {
            return component;
        }

        Component time = Component.text(minutes);
        Component unit = Component.translatable(minutes == 1
            ? "civ.restart.minute"
            : "civ.restart.minutes");

        return Component.translatable(translatable.key(), time, unit)
            .style(component.style());
    }

}
