package vg.civcraft.mc.civmodcore.inventory.items.custom;

import java.util.List;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vg.civcraft.mc.civmodcore.inventory.items.updater.migrations.ItemMigrations;

public abstract class CustomItemv2 {

    /**
     * Get item's unique custom item key
     *
     * @return Returns this item's unique key
     */
    abstract public String getCustomItemKey();

    /**
     * Creates the item
     *
     * @return Returns the created item
     */
    abstract public ItemStack createItem();

    /**
     * Container for a crafting recipe and its associated triggers
     *
     * @param recipe   The crafting recipe to be registered
     * @param triggers Items that when obtained by the player will trigger the recipe to be added to their recipe book
     */
    protected record CustomItemRecipe(CraftingRecipe recipe, List<ItemStack> triggers) {

    }

    /**
     * Get all crafting recipes associated with the item
     *
     * @param plugin The plugin to register the recipes too
     * @return Returns a list of crafting recipes
     */
    abstract protected List<CustomItemRecipe> getRecipes(Plugin plugin);

    abstract protected void registerMigrations(@NotNull ItemMigrations migrations);

    /**
     * Asserts if the provided item is the same type as the class
     *
     * @param item The item to check
     * @return Returns if the item provided is this custom item
     */
    public boolean isItem(@Nullable ItemStack item) {
        return CustomItem.isCustomItem(item, getCustomItemKey());
    }
}
