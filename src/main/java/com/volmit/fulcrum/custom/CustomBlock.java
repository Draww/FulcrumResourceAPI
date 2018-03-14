package com.volmit.fulcrum.custom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.volmit.fulcrum.Fulcrum;
import com.volmit.fulcrum.sfx.Audible;

public class CustomBlock implements ICustomBlock
{
	private Audible breakSound;
	private Audible placeSound;
	private Audible stepSound;
	private BlockRenderType renderType;
	private String name;
	private final String id;
	private short durabilityLock;
	private Material material;
	private boolean shaded;
	private int sid;
	private String matt;
	private boolean ee;

	public CustomBlock(String id)
	{
		ee = false;
		this.id = id;
		sid = 0;
		setName("fulcrum:" + id);
		shaded = false;
		matt = "";
		renderType = BlockRenderType.ALL;
	}

	public CustomBlock(String id, BlockRenderType renderType)
	{
		this(id);
		this.renderType = renderType;
	}

	public void setBreakSound(Audible breakSound)
	{
		this.breakSound = breakSound;
	}

	public void setPlaceSound(Audible placeSound)
	{
		this.placeSound = placeSound;
	}

	public void setStepSound(Audible stepSound)
	{
		this.stepSound = stepSound;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public Audible getBreakSound()
	{
		return breakSound;
	}

	@Override
	public Audible getPlaceSound()
	{
		return placeSound;
	}

	@Override
	public Audible getStepSound()
	{
		return stepSound;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public void setDurabilityLock(short d)
	{
		this.durabilityLock = d;
	}

	@Override
	public short getDurabilityLock()
	{
		return durabilityLock;
	}

	@Override
	public ItemStack getItem()
	{
		ItemStack is = new ItemStack(getType());
		is.setDurability(getDurabilityLock());
		ItemMeta im = is.getItemMeta();
		im.setUnbreakable(true);
		im.setDisplayName(getName());

		if(ee)
		{
			im.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 0, true);
		}

		im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		im.addItemFlags(ItemFlag.HIDE_DESTROYS);
		im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		im.addItemFlags(ItemFlag.HIDE_PLACED_ON);
		im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		im.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		is.setItemMeta(im);

		return is;
	}

	@Override
	public void set(Location location)
	{
		Fulcrum.adapter.setSpawnerType(location, getMatt(), getDurabilityLock(), ee);
	}

	@Override
	public BlockRenderType getRenderType()
	{
		return renderType;
	}

	public void setRenderType(BlockRenderType renderType)
	{
		this.renderType = renderType;
	}

	@Override
	public Material getType()
	{
		return material;
	}

	@Override
	public void setType(Material type)
	{
		material = type;
	}

	@Override
	public boolean isShaded()
	{
		return shaded;
	}

	public Material getMaterial()
	{
		return material;
	}

	public void setMaterial(Material material)
	{
		this.material = material;
	}

	public void setShaded(boolean shaded)
	{
		this.shaded = shaded;
	}

	@Override
	public int getSuperID()
	{
		return sid;
	}

	@Override
	public void setSuperID(int f)
	{
		sid = f;
	}

	@Override
	public String getMatt()
	{
		return matt;
	}

	@Override
	public void setMatt(String matt)
	{
		this.matt = matt;
	}

	@Override
	public void setEnchanted(boolean boolean1)
	{
		ee = boolean1;
	}

	@Override
	public boolean isEnchanted()
	{
		return ee;
	}
}