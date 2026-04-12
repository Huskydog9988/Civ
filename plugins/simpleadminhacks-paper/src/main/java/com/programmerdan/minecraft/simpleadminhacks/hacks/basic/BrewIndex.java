package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.dre.brewery.Brew;
import com.dre.brewery.api.events.brew.BrewDrinkEvent;
import com.dre.brewery.recipe.BRecipe;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import vg.civcraft.mc.civmodcore.commands.CommandManager;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;

public final class BrewIndex extends BasicHack {

    private static final String PDC_SEPARATOR = ";";

    private final NamespacedKey drunkBrewsKey;
    private final CommandManager commands;
    private boolean breweryEnabled;

    public BrewIndex(final SimpleAdminHacks plugin, final BasicHackConfig config) {
        super(plugin, config);
        this.drunkBrewsKey = new NamespacedKey(plugin, "drunk_brews");
        this.commands = new CommandManager(plugin()) {
            @Override
            public void registerCommands() {
                registerCommand(new BrewIndexCommand());
            }
        };
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.breweryEnabled = Bukkit.getPluginManager().isPluginEnabled("BreweryX");
        if (!this.breweryEnabled) {
            plugin().warning("BrewIndex: BreweryX not found, /brews command will be unavailable");
        }
        this.commands.init();
    }

    @Override
    public void onDisable() {
        this.commands.reset();
        super.onDisable();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewDrink(final BrewDrinkEvent event) {
        final Brew brew = event.getBrew();
        final BRecipe recipe = brew.getCurrentRecipe();
        if (recipe == null) {
            return;
        }
        final String recipeName = recipe.getRecipeName();
        if (recipeName == null || recipeName.isEmpty()) {
            return;
        }
        recordDrunkBrew(event.getPlayer(), recipeName, event.getQuality());
    }

    /**
     * Reads the player's PDC to get a map of recipe name -> best quality drunk.
     * Format stored in PDC: "name:quality;name:quality;..."
     */
    private Map<String, Integer> getDrunkBrews(final Player player) {
        final PersistentDataContainer pdc = player.getPersistentDataContainer();
        final String raw = pdc.get(this.drunkBrewsKey, PersistentDataType.STRING);
        final Map<String, Integer> brews = new HashMap<>();
        if (raw != null && !raw.isEmpty()) {
            for (final String entry : raw.split(PDC_SEPARATOR, -1)) {
                if (entry.isEmpty()) {
                    continue;
                }
                final int colonIndex = entry.lastIndexOf(':');
                final String name = entry.substring(0, colonIndex);
                try {
                    final int quality = Integer.parseInt(entry.substring(colonIndex + 1));
                    brews.put(name, quality);
                } catch (final NumberFormatException ignored) {
                    brews.put(entry, 0);
                }
            }
        }
        return brews;
    }

    private void saveDrunkBrews(final Player player, final Map<String, Integer> brews) {
        final PersistentDataContainer pdc = player.getPersistentDataContainer();
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<String, Integer> entry : brews.entrySet()) {
            if (!first) {
                builder.append(PDC_SEPARATOR);
            }
            builder.append(entry.getKey()).append(':').append(entry.getValue());
            first = false;
        }
        pdc.set(this.drunkBrewsKey, PersistentDataType.STRING, builder.toString());
    }

    private void recordDrunkBrew(final Player player, final String recipeName, final int quality) {
        final Map<String, Integer> brews = getDrunkBrews(player);
        final Integer existing = brews.get(recipeName);
        if (existing == null || quality > existing) {
            brews.put(recipeName, quality);
            saveDrunkBrews(player, brews);
        }
    }

    private void openBrewGui(final Player player) {
        if (!this.breweryEnabled) {
            player.sendMessage(Component.text("BreweryX is not installed on this server.")
                .color(NamedTextColor.RED));
            return;
        }

        final List<BRecipe> allRecipes = BRecipe.getAllRecipes();
        if (allRecipes == null || allRecipes.isEmpty()) {
            player.sendMessage(Component.text("No brews are configured on this server.")
                .color(NamedTextColor.RED));
            return;
        }

        final List<BRecipe> sortedRecipes = new ArrayList<>(allRecipes);
        sortedRecipes.sort(Comparator.comparing(BRecipe::getRecipeName, String.CASE_INSENSITIVE_ORDER));

        final Map<String, Integer> drunkBrews = getDrunkBrews(player);
        final int totalBrews = sortedRecipes.size();
        final int discoveredBrews = (int) sortedRecipes.stream()
            .filter(recipe -> drunkBrews.containsKey(recipe.getRecipeName()))
            .count();

        final List<IClickable> clickables = new ArrayList<>();
        for (final BRecipe recipe : sortedRecipes) {
            final String recipeName = recipe.getRecipeName();
            final Integer bestQuality = drunkBrews.get(recipeName);
            final boolean hasDrunk = bestQuality != null;

            ItemStack displayItem;
            try {
                final Brew brew = recipe.createBrew(10);
                displayItem = brew.createItem(recipe);
            } catch (final Exception exception) {
                displayItem = new ItemStack(Material.POTION);
            }

            ItemUtils.setComponentDisplayName(displayItem, Component.text(recipeName)
                .color(hasDrunk ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

            final List<Component> lore = new ArrayList<>();
            final ItemMeta meta = displayItem.getItemMeta();
            if (hasDrunk) {
                lore.add(Component.text("\u2714 Discovered")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("\u2605 Best quality: " + bestQuality + "/10")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
                meta.setEnchantmentGlintOverride(true);
            } else {
                lore.add(Component.text("\u2718 Not yet discovered")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            displayItem.setItemMeta(meta);

            clickables.add(new DecorationStack(displayItem));
        }

        final String title = "Brew Log (" + discoveredBrews + "/" + totalBrews + " discovered)";
        final MultiPageView view = new MultiPageView(player, clickables, title, true);
        view.showScreen();
    }

    public class BrewIndexCommand extends BaseCommand {

        @CommandAlias("brews")
        @Description("Opens the brew log showing all brews and which ones you have drunk")
        public void onBrews(final Player sender) {
            openBrewGui(sender);
        }
    }
}
