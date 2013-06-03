package com.github.igotyou.FactoryMod.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.FactoryObject.FactoryType;
import com.github.igotyou.FactoryMod.Factorys.PrintingPress;
import com.github.igotyou.FactoryMod.Factorys.ProductionFactory;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.Manager;
import com.github.igotyou.FactoryMod.properties.PrintingPressProperties;
import com.github.igotyou.FactoryMod.properties.ProductionProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.recipes.ProductionRecipe;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;
import java.util.Iterator;

//original file:
/**
* Manager.java
* Purpose: Interface for Manager objects for basic manager functionality
*
* @author MrTwiggy
* @version 0.1 1/08/13
*/
//edited version:
/**
* Manager.java	 
* Purpose: Interface for Manager objects for basic manager functionality
* @author igotyou
*
*/

public class PrintingPressManager implements Manager
{
	private FactoryModPlugin plugin;
	private List<PrintingPress> producers;
	private long repairTime;
	
	public PrintingPressManager(FactoryModPlugin plugin)
	{
		this.plugin = plugin;
		producers = new ArrayList<PrintingPress>();
		//Set maintenance clock to 0
		updateFactorys();
	}
	
	public void save(File file) throws IOException 
	{
		//Takes difference between last repair update and current one and scales repair accordingly
		updateRepair(System.currentTimeMillis()-repairTime);
		repairTime=System.currentTimeMillis();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fileOutputStream);
		oos.writeInt(producers.size());
		for (PrintingPress production : producers)
		{
			//order: subFactoryType world recipe1,recipe2 central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			
			Location centerlocation = production.getCenterLocation();
			Location inventoryLocation = production.getInventoryLocation();
			Location powerLocation = production.getPowerSourceLocation();
			
			oos.writeUTF(centerlocation.getWorld().getName());
			
			oos.writeInt(centerlocation.getBlockX());
			oos.writeInt(centerlocation.getBlockY());
			oos.writeInt(centerlocation.getBlockZ());

			oos.writeInt(inventoryLocation.getBlockX());
			oos.writeInt(inventoryLocation.getBlockY());
			oos.writeInt(inventoryLocation.getBlockZ());

			oos.writeInt(powerLocation.getBlockX());
			oos.writeInt(powerLocation.getBlockY());
			oos.writeInt(powerLocation.getBlockZ());
			
			oos.writeBoolean(production.getActive());
			oos.writeInt(production.getProductionTimer());
			oos.writeInt(production.getEnergyTimer());
			oos.writeDouble(production.getCurrentRepair());
			oos.writeLong(production.getTimeDisrepair());
		}
		oos.flush();
		fileOutputStream.close();
	}

	public void load(File file) throws IOException 
	{
		repairTime=System.currentTimeMillis();
		FileInputStream fileInputStream = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fileInputStream);
		int count = ois.readInt();
		int i = 0;
		for (i = 0; i < count; i++)
		{
			
			String worldName = ois.readUTF();
			World world = plugin.getServer().getWorld(worldName);

			Location centerLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
			Location inventoryLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
			Location powerLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
			boolean active = ois.readBoolean();
			int productionTimer = ois.readInt();
			int energyTimer = ois.readInt();
			double currentRepair = ois.readDouble();
			long timeDisrepair  = ois.readLong();

			PrintingPress production = new PrintingPress(centerLocation, inventoryLocation, powerLocation,
					active, productionTimer,
					energyTimer, currentRepair, timeDisrepair, plugin.getPrintingPressProperties());
			addFactory(production);
		}
		fileInputStream.close();
	}

	public void updateFactorys() 
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				for (PrintingPress production: producers)
				{
					production.update();
				}
			}
		}, 0L, FactoryModPlugin.PRODUCER_UPDATE_CYCLE);
	}

	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			HashMap<String, ProductionProperties> properties = plugin.productionProperties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				ItemList<NamedItemStack> inputs = entry.getValue().getInputs();
				if(inputs.exactlyIn(chestInventory))
				{
					subFactoryType = entry.getKey();
				}
			}
			if (subFactoryType != null)
			{
				ProductionFactory production = new ProductionFactory(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (properties.get(subFactoryType).getInputs().allIn(production.getInventory()))
				{
					addFactory(production);
					properties.get(subFactoryType).getInputs().removeFrom(production.getInventory());
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + production.getProductionFactoryProperties().getName());
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Incorrect materials in chest! Stacks must match perfectly.");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a factory there!");
	}
	
	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation, int productionTimer, int energyTimer) 
	{
		PrintingPressProperties printingPressProperties = plugin.getPrintingPressProperties();
		
		if (!factoryExistsAt(factoryLocation))
		{
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			ItemList<NamedItemStack> inputs = printingPressProperties.getConstructionMaterials();
			boolean hasMaterials = inputs.allIn(chestInventory);
			if (hasMaterials)
			{
				PrintingPress production = new PrintingPress(factoryLocation, inventoryLocation, powerSourceLocation, false, plugin.getPrintingPressProperties());
				if (printingPressProperties.getConstructionMaterials().removeFrom(production.getInventory()))
				{
					addFactory(production);
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + printingPressProperties.getName());
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Not enough materials in chest!");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a " + printingPressProperties.getName() + " there!");
	}

	public InteractionResponse addFactory(Factory factory) 
	{
		PrintingPress production = (PrintingPress) factory;
		if (production.getCenterLocation().getBlock().getType().equals(FactoryModPlugin.CENTRAL_BLOCK_MATERIAL) && (!factoryExistsAt(production.getCenterLocation()))
				|| !factoryExistsAt(production.getInventoryLocation()) || !factoryExistsAt(production.getPowerSourceLocation()))
		{
			producers.add(production);
			return new InteractionResponse(InteractionResult.SUCCESS, "");
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, "");
		}
	}

	public PrintingPress getFactory(Location factoryLocation) 
	{
		for (PrintingPress production : producers)
		{
			if (production.getCenterLocation().equals(factoryLocation) || production.getInventoryLocation().equals(factoryLocation)
					|| production.getPowerSourceLocation().equals(factoryLocation))
				return production;
		}
		return null;
	}
	
	public boolean factoryExistsAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = true;
		}
		return returnValue;
	}
	
	public boolean factoryWholeAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = getFactory(factoryLocation).isWhole();
		}
		return returnValue;
	}

	public void removeFactory(Factory factory) 
	{
		producers.remove((ProductionFactory)factory);
	}
	
	public void updateRepair(long time)
	{
		for (PrintingPress production: producers)
		{
			production.updateRepair(time/((double)FactoryModPlugin.REPAIR_PERIOD));
		}
		long currentTime=System.currentTimeMillis();
		Iterator<PrintingPress> itr=producers.iterator();
		while(itr.hasNext())
		{
			PrintingPress producer=itr.next();
			if(currentTime>(producer.getTimeDisrepair()+FactoryModPlugin.DISREPAIR_PERIOD))
			{
				itr.remove();
			}
		}
	}
	
	public String getSavesFileName() 
	{
		return FactoryModPlugin.PRODUCTION_SAVES_FILE;
	}

}
