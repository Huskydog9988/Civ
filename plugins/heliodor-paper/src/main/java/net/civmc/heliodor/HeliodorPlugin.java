package net.civmc.heliodor;

import net.civmc.heliodor.command.HeliodorDebugCommand;
import net.civmc.heliodor.heliodor.infusion.InfusionListener;
import net.civmc.heliodor.heliodor.infusion.InfusionManager;
import net.civmc.heliodor.heliodor.infusion.chunkmeta.CauldronDao;
import net.civmc.heliodor.heliodor.infusion.chunkmeta.CauldronInfuseData;
import net.civmc.heliodor.heliodor.infusion.chunkmeta.CauldronInfusion;
import net.civmc.heliodor.vein.data.MeteoricIronVeinConfig;
import net.civmc.heliodor.vein.OrePredicate;
import net.civmc.heliodor.vein.SqlVeinDao;
import net.civmc.heliodor.vein.VeinCache;
import net.civmc.heliodor.vein.data.VeinConfig;
import net.civmc.heliodor.vein.VeinSpawner;
import net.civmc.heliodor.vein.data.VerticalBlockPos;
import net.civmc.heliodor.vein.listener.OreBreakListener;
import net.civmc.heliodor.vein.listener.VeinBreakListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.BlockBasedChunkMetaView;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.ChunkMetaAPI;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableStorageEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class HeliodorPlugin extends ACivMod {

    private ManagedDatasource database;
    private BlockProtector protector;
    private BlockBasedChunkMetaView<CauldronInfuseData, TableBasedDataObject, TableStorageEngine<CauldronInfusion>> chunkMetaView;
    private HeliodorRecipeGiver recipes;
    private VeinSpawner veinSpawner;
    private NamespacedKey oreLocationsKey;
    private VeinCache veinCache;

    @Override
    public void onEnable() {
        super.onEnable();

        saveDefaultConfig();

        database = ManagedDatasource.construct(this, (DatabaseCredentials) getConfig().get("database"));
        if (database == null) {
            Bukkit.shutdown();
            return;
        }

        protector = new BlockProtector();
        getServer().getPluginManager().registerEvents(protector, this);

        InfusionManager infusionManager = new InfusionManager();
        CauldronDao dao = new CauldronDao(this.getLogger(), database, infusionManager);
        dao.registerMigrations();

        initVeins();

        if (!database.updateDatabase()) {
            Bukkit.shutdown();
        }

        this.veinCache.load();

        Supplier<CauldronInfuseData> newData = () -> new CauldronInfuseData(false, dao, infusionManager);
        this.chunkMetaView = ChunkMetaAPI.registerBlockBasedPlugin(this, newData, dao, true);

        protector.addPredicate(l -> chunkMetaView.get(l) != null);
        getServer().getPluginManager().registerEvents(new InfusionListener(infusionManager, chunkMetaView), this);

        this.recipes = new HeliodorRecipeGiver(this);

        getCommand("heliodor").setExecutor(new HeliodorDebugCommand(veinCache, veinSpawner, oreLocationsKey));
    }

    public BlockBasedChunkMetaView<CauldronInfuseData, TableBasedDataObject, TableStorageEngine<CauldronInfusion>> getChunkMetaView() {
        return chunkMetaView;
    }

    public HeliodorRecipeGiver getRecipes() {
        return recipes;
    }

    private void initVeins() {
        ConfigurationSection meteoricIronConfigSection = getConfig().getConfigurationSection("meteoric_iron_vein");
        if (!meteoricIronConfigSection.getBoolean("enabled")) {
            return;
        }

        List<VerticalBlockPos> configPosList = new ArrayList<>();
        List<Map<?, ?>> positions = meteoricIronConfigSection.getMapList("positions");
        for (Map<?, ?> position : positions) {
            configPosList.add(new VerticalBlockPos((int) position.get("x"), (int) position.get("z")));
        }

        MeteoricIronVeinConfig meteoriteIronConfig = new MeteoricIronVeinConfig(
            new VeinConfig(
                meteoricIronConfigSection.getString("world"),
                meteoricIronConfigSection.getInt("frequency_minutes"),
                meteoricIronConfigSection.getInt("spawn_radius"),
                meteoricIronConfigSection.getInt("min_ore"),
                meteoricIronConfigSection.getInt("max_ore"),
                meteoricIronConfigSection.getInt("low_distance"),
                meteoricIronConfigSection.getInt("high_distance"),
                meteoricIronConfigSection.getInt("veinfinder_inaccuracy"),
                meteoricIronConfigSection.getInt("max_spawns"),
                meteoricIronConfigSection.getInt("min_blocks")
            ),
            configPosList,
            meteoricIronConfigSection.getInt("min_position_radius"),
            meteoricIronConfigSection.getInt("max_position_radius"),
            meteoricIronConfigSection.getInt("max_bury")
        );

        SqlVeinDao veinDao = new SqlVeinDao(database);
        veinDao.registerMigrations();
        veinCache = new VeinCache(this, veinDao);
        oreLocationsKey = new NamespacedKey(this, "ore_locations");
        getServer().getPluginManager().registerEvents(new OreBreakListener(oreLocationsKey), this);
        getServer().getPluginManager().registerEvents(new VeinBreakListener(oreLocationsKey, veinCache), this);
        protector.addPredicate(new OrePredicate(oreLocationsKey));
        veinSpawner = new VeinSpawner(this, veinDao, veinCache, meteoriteIronConfig);
        veinSpawner.start();
    }
}
