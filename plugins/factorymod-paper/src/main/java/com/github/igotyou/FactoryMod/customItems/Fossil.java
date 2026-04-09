package com.github.igotyou.FactoryMod.customItems;

import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.inventory.items.custom.CustomItemv2;
import vg.civcraft.mc.civmodcore.inventory.items.updater.migrations.ItemMigrations;

public class Fossil extends CustomItemv2 {

    @Override
    public String getCustomItemKey() {
        return "fossil";
    }

    @Override
    public ItemStack createItem() {
        ItemStack fossil = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = fossil.getItemMeta();

        meta.displayName(Component.text("Fossil", NamedTextColor.AQUA));
        meta.lore(List.of(
            Component.text("Crack me in a factory for a prize!", NamedTextColor.WHITE)
        ));

        fossil.setItemMeta(meta);
        return fossil;
    }

    // TODO: add something to return a list of items to trigger recipe discovery
    @Override
    protected List<CustomItemRecipe> getRecipes(Plugin plugin) {
        return Collections.emptyList();
    }

    @Override
    protected void registerMigrations(ItemMigrations migrations) {
        migrations.registerMetaMigration(1, (meta) -> {
            // move old
            meta.displayName(Component.text("Fossil", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Crack me in a factory for a prize!", NamedTextColor.WHITE)
            ));
        });
    }
}
