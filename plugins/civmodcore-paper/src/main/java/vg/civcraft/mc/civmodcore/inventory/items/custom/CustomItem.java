package vg.civcraft.mc.civmodcore.inventory.items.custom;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;

/**
 * Since Minecraft doesn't [yet] offer a means of registering custom item materials, this is the intended means of
 * defining custom items in the meantime. Keep in mind that each custom item must correlate 1:1 with its key, ie, that
 * custom-item keys should be treated like item materials. Do NOT use custom-item keys as custom-item categories, such
 * as compacted items. You must always be able to receive the same item from the same key.
 */
public final class CustomItem {
    public static NamespacedKey CUSTOM_ITEM_KEY = new NamespacedKey(JavaPlugin.getPlugin(CivModCorePlugin.class), "custom_item");

    private static final Map<String, Supplier<ItemStack>> customItems = new HashMap<>();
    private static final CustomItemsUpdater updater = new CustomItemsUpdater();
    private static final RecipeGiver recipeGiver = new RecipeGiver();

    /**
     * Register custom item
     * @deprecated Use {@link #registerCustomItem(JavaPlugin, CustomItemv2)} instead, which also registers recipes and migrations for the custom item.
     * @param key Custom item key
     * @param factory Item factory
     */
    @Deprecated
    public static void registerCustomItem(
        final @NotNull String key,
        final @NotNull Supplier<@NotNull ItemStack> factory
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);
        customItems.putIfAbsent(key, factory);
    }

    /**
     * Register custom item
     * @deprecated Use {@link #registerCustomItem(JavaPlugin, CustomItemv2)} instead, which also registers recipes and migrations for the custom item.
     * @param key Custom item key
     * @param template ItemStack to register as custom item
     */
    @Deprecated
    public static void registerCustomItem(
        final @NotNull String key,
        final @NotNull ItemStack template
    ) {
        registerCustomItem(key, template::clone);
    }

    /**
     * Registers a custom item
     * @param plugin The plugin to register the custom item too
     * @param item The custom item
     */
    public static void registerCustomItem(@NotNull JavaPlugin plugin, @NotNull CustomItemv2 item) {
        // TODO: once fully on CustomItemV2, prevent duplicate custom item keys from being registered
        registerCustomItem(item.getCustomItemKey(), item::createItem);
        item.registerMigrations(updater.getMigrationsFor(item.getCustomItemKey()));
        recipeGiver.registerRecipes(item.getRecipes(plugin));
    }

    public static @Nullable ItemStack getCustomItem(
        final @NotNull String key
    ) {
        if (customItems.get(Objects.requireNonNull(key)) instanceof final Supplier<ItemStack> factory) {
            final ItemStack item = factory.get();
            setCustomItemKey(item, key);
            return item;
        }
        return null;
    }

    /**
     * Just remember that has-then-get is an anti-pattern: use {@link #isCustomItem(org.bukkit.inventory.ItemStack, String)}
     * or {@link #getCustomItemKey(org.bukkit.inventory.ItemStack)} instead.
     */
    public static boolean isCustomItem(
        final ItemStack item
    ) {
        return !ItemUtils.isEmptyItem(item) && item.getPersistentDataContainer().has(CUSTOM_ITEM_KEY);
    }

    public static boolean isCustomItem(
        final ItemStack item,
        final @NotNull String key
    ) {
        return key.equals(getCustomItemKey(item));
    }

    public static @Nullable String getCustomItemKey(
        final ItemStack item
    ) {
        if (!ItemUtils.isEmptyItem(item)) {
            return item.getPersistentDataContainer().get(CUSTOM_ITEM_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    @ApiStatus.Internal
    public static void setCustomItemKey(
        final @NotNull ItemStack item,
        final @NotNull String key
    ) {
        item.editPersistentDataContainer((pdc) -> pdc.set(CUSTOM_ITEM_KEY, PersistentDataType.STRING, key));
    }

    public static @NotNull Set<@NotNull String> getRegisteredKeys() {
        return Collections.unmodifiableSet(customItems.keySet());
    }

    /**
     * Class responsible for registering recipes to the server, and giving them to players when they have the required trigger items in their inventory.
     */
    public static class RecipeGiver implements Runnable {
        private final HashMap<ItemStack, NamespacedKey> triggersForRecipes = new HashMap<>();

        @Override
        public void run() {
            try {
                if (triggersForRecipes.isEmpty()) {
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (var triggerItem : triggersForRecipes.keySet()) {
                        if (player.getInventory().containsAtLeast(triggerItem, 1)) {
                            player.discoverRecipe(triggersForRecipes.get(triggerItem));
                        }
                    }
                }
            } catch (RuntimeException ex) {
                JavaPlugin.getPlugin(CivModCorePlugin.class).getLogger().log(Level.WARNING, "Iterating inventories for heliodor", ex);
            }
        }

        /**
         * Register recipes to the server, and so they can be 'discovered' by players
         * @param recipes The recipies to register
         */
        protected void registerRecipes(List<CustomItemv2.CustomItemRecipe> recipes) {
            for (var recipePair : recipes) {
                // add recipe triggers so we can check if a player has that item to then give them the recipe
                for (var trigger : recipePair.triggers()) {
                    triggersForRecipes.put(trigger, recipePair.recipe().getKey());
                }
                // finally add the recipe to the server
                Bukkit.getServer().addRecipe(recipePair.recipe());
            }
        }
    }

    /**
     * Register listeners and tasks for custom items
     * @param plugin The plugin to attribute the listeners and tasks to
     */
    public static void init(JavaPlugin plugin) {
        updater.init(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, CustomItem.recipeGiver, 15 * 20, 15 * 20);
    }
}
