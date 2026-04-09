package vg.civcraft.mc.civmodcore.inventory.items.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.inventory.items.updater.ItemUpdater;
import vg.civcraft.mc.civmodcore.inventory.items.updater.listeners.DefaultItemUpdaterListeners;
import vg.civcraft.mc.civmodcore.inventory.items.updater.migrations.ItemMigrations;

public class CustomItemsUpdater implements ItemUpdater {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Map<String, ItemMigrations> targets = new HashMap<>();

    /**
     * Retrieves a migration list for the given custom key, creating one if it didn't already exist.
     */
    public @NotNull ItemMigrations getMigrationsFor(
        final @NotNull String customKey
    ) {
        return getMigrationsFor(Objects.requireNonNull(customKey), true);
    }

    /**
     * Retrieves a migration list for the given custom key.
     *
     * @param createIfAbsent Whether to create the list if one doesn't already exist.
     */
    @Contract("_, true -> !null")
    public @Nullable ItemMigrations getMigrationsFor(
        final @NotNull String customKey,
        final boolean createIfAbsent
    ) {
        Objects.requireNonNull(customKey);
        if (createIfAbsent) {
            return this.targets.computeIfAbsent(customKey, ItemMigrations::new);
        }
        return this.targets.get(customKey);
    }

    public void removeMigrationsFor(
        final @NotNull String customKey
    ) {
        final ItemMigrations migrations = this.targets.remove(Objects.requireNonNull(customKey));
        if (migrations != null) {
            migrations.clearMigrations();
        }
    }

    public void clearMigrations() {
        final List<ItemMigrations> migrations = List.copyOf(this.targets.values());
        this.targets.clear();
        for (final ItemMigrations migration : migrations) {
            migration.clearMigrations();
        }
    }

    /**
     * This is how this class determines whether a given item is a custom item.
     *
     * @param item The item to check.
     * @return The custom key of the item, if it is a custom item, or null if it isn't a custom item.
     */
    protected @Nullable String getCustomKeyFrom(
        @NotNull ItemStack item
    ) {
        // TODO: add special check for compacted items as they should be wrappers around other items
        if (CustomItem.getCustomItemKey(item) instanceof final String customKey) {
            return customKey;
        }

        // check if it's a fossil
        if (item.getType() == Material.PRISMARINE_SHARD &&
            ItemUtils.hasDisplayName(item, "§3Fossil") &&
            ItemUtils.hasLoreLine(item, "Crack me in a factory for a prize!")) {
            return "fossil";
        }

        return null;
    }

    @Override
    public boolean updateItem(
        final @NotNull ItemStack item
    ) {
        final String customKey = getCustomKeyFrom(item);
        if (customKey == null) {
            return false;
        }
        final ItemMigrations migrations = this.targets.get(customKey);
        if (migrations == null) {
            return false;
        }
        return migrations.attemptMigration(item);
    }

    /**
     * Register listeners
     * @param plugin The plugin to register the listeners on
     */
    public void init(
        final @NotNull JavaPlugin plugin
    ) {
        Bukkit.getPluginManager().registerEvents(DefaultItemUpdaterListeners.wrap(this), plugin);
    }
}
