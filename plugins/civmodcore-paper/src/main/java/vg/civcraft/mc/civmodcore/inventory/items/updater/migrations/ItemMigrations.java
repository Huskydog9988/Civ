package vg.civcraft.mc.civmodcore.inventory.items.updater.migrations;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.inventory.items.custom.CustomItem;

public class ItemMigrations {
    private static final NamespacedKey VERSION_KEY = new NamespacedKey(JavaPlugin.getPlugin(CivModCorePlugin.class), "item_version");
    protected final TreeMap<Integer, ItemMigration<?>> migrations = new TreeMap<>(Integer::compareTo);

    /**
     * Create a new migration list for a given custom item key.
     * @implSpec This will automatically register a 0th migration that ensures any item being migrated has the correct custom key and a migration version, so you don't have to worry about that in your future migrations.
     * @param customKey The custom item key that this migration list is for.
     */
    public ItemMigrations(
        final @NotNull String customKey
    ) {
        // This is a deliberate 0th migration that ensures that any item
        // being migrated has a custom-item key and item version.
        this.migrations.put(0, (ItemMigration.OfItem) (item) -> {
            CustomItem.setCustomItemKey(item, customKey);
            setMigrationVersion(item, 0);
        });
    }

    public void registerMigration(
        final @Range(from = 1, to = Integer.MAX_VALUE) int migrationId,
        final @NotNull ItemMigration<?> updater
    ) {
        this.migrations.putIfAbsent(migrationId, Objects.requireNonNull(updater));
    }

    /** Convenience shortcut */
    public void registerDataMigration(
        final @Range(from = 1, to = Integer.MAX_VALUE) int migrationId,
        final @NotNull ItemMigration.OfData updater
    ) {
        registerMigration(migrationId, updater);
    }

    /** Convenience shortcut */
    public void registerMetaMigration(
        final @Range(from = 1, to = Integer.MAX_VALUE) int migrationId,
        final @NotNull ItemMigration.OfMeta updater
    ) {
        registerMigration(migrationId, updater);
    }

    public void clearMigrations() {
        this.migrations.clear();
    }

    /**
     * Retrieves the current migration version of a given item.
     *
     * @return Returns the current migration version, or null if it isn't set.
     */
    public @Nullable Integer getMigrationVersion(
        final @NotNull ItemStack item
    ) {
        // This is a readonly PDC
        return item.getPersistentDataContainer().get(VERSION_KEY, PersistentDataType.INTEGER);
    }

    /**
     * Permanently stores the given migration version onto the given item in a way that
     * {@link #getMigrationVersion(org.bukkit.inventory.ItemStack)} will detect.
     */
    public void setMigrationVersion(
        final @NotNull ItemStack item,
        final int version
    ) {
        item.editPersistentDataContainer((pdc) -> pdc.set(VERSION_KEY, PersistentDataType.INTEGER, version));
    }

    /**
     * Attempts to update the given item according to a series of pre-registered migrations.
     *
     * @param item The item to update, which MUST NOT be an empty item, as determined by a {@link vg.civcraft.mc.civmodcore.inventory.items.ItemUtils#isEmptyItem(ItemStack)} check.
     * @return Whether the item was updated.
     */
    public boolean attemptMigration(
        final @NotNull ItemStack item
    ) {
        final Integer currentVersion = getMigrationVersion(item);
        final Map<Integer, ItemMigration<?>> pendingMigrations = this.migrations.tailMap(
            Objects.requireNonNullElse(currentVersion, 0),
            currentVersion == null // Include the 0th migration if the version was missing
        );
        boolean updated = false;
        for (final Map.Entry<Integer, ItemMigration<?>> entry : pendingMigrations.entrySet()) {
            ItemMigration.migrate(item, entry.getValue());
            setMigrationVersion(item, entry.getKey());
            updated = true;
        }
        return updated;
    }
}
