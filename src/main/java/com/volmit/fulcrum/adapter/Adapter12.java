package com.volmit.fulcrum.adapter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.volmit.fulcrum.bukkit.BlockType;
import com.volmit.fulcrum.bukkit.P;
import com.volmit.fulcrum.bukkit.Task;
import com.volmit.fulcrum.lang.GList;
import com.volmit.fulcrum.lang.GMap;
import com.volmit.fulcrum.lang.GSet;

import net.minecraft.server.v1_12_R1.BiomeBase;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_12_R1.ChunkSection;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntity;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_12_R1.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_12_R1.PacketPlayOutUnloadChunk;

public final class Adapter12 implements IAdapter
{
	private GMap<Chunk, GSet<Location>> update;
	private GMap<Chunk, GSet<Integer>> dirty;

	public Adapter12()
	{
		update = new GMap<Chunk, GSet<Location>>();
		dirty = new GMap<Chunk, GSet<Integer>>();

		new Task(0)
		{
			@Override
			public void run()
			{
				onTick();
			}
		};
	}

	private void onTick()
	{
		for(Chunk i : update.k())
		{
			if(update.get(i).size() > 24)
			{
				for(Location j : update.get(i))
				{
					makeDirty(i, getSection(j.getBlockY()));
				}

				continue;
			}

			if(update.get(i).size() > 1)
			{
				try
				{
					sendMultiBlockChange(i, new GList<Location>(update.get(i)));
					continue;
				}

				catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e)
				{
					System.out.println("Failed to send multiblock change, sending block changes instead.");
					e.printStackTrace();
				}
			}

			for(Player j : P.getPlayersWithinViewOf(i))
			{
				for(Location k : update.get(i))
				{
					sendBlockChange(k, new BlockType(k.getBlock()), j);
				}
			}
		}

		for(Chunk i : dirty.k())
		{
			boolean[] bits = new boolean[16];
			Arrays.fill(bits, false);

			for(int j : dirty.get(i))
			{
				bits[j] = true;
			}

			sendChunkSection(i, getBitMask(bits));
		}

		dirty.clear();
		update.clear();
	}

	@Override
	public int getBiomeId(Biome biome)
	{
		BiomeBase mcBiome = CraftBlock.biomeToBiomeBase((Biome) biome);
		return mcBiome != null ? BiomeBase.a((BiomeBase) mcBiome) : 0;
	}

	@Override
	public Biome getBiome(int id)
	{
		BiomeBase mcBiome = BiomeBase.getBiome((int) id);
		return CraftBlock.biomeBaseToBiome((BiomeBase) mcBiome);
	}

	@Override
	public BlockType getBlock(Location location)
	{
		return new BlockType(location);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setBlock(Location l, BlockType m)
	{
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		net.minecraft.server.v1_12_R1.World w = ((CraftWorld) l.getWorld()).getHandle();
		net.minecraft.server.v1_12_R1.Chunk chunk = w.getChunkAt(x >> 4, z >> 4);
		BlockPosition bp = new BlockPosition(x, y, z);
		int combined = m.getMaterial().getId() + (m.getData() << 12);
		IBlockData ibd = net.minecraft.server.v1_12_R1.Block.getByCombinedId(combined);
		chunk.a(bp, ibd);
		makeDirty(l);
	}

	@Override
	public void makeDirty(Location l)
	{
		if(!update.containsKey(l.getChunk()))
		{
			update.put(l.getChunk(), new GSet<Location>());
		}

		update.get(l.getChunk()).add(l);
	}

	@Override
	public void makeDirty(Chunk c, int section)
	{
		if(!dirty.containsKey(c))
		{
			dirty.put(c, new GSet<Integer>());
		}

		dirty.get(c).add(section);
	}

	@Override
	public void makeDirty(Chunk c)
	{
		boolean[] s = getValidSections(c);

		for(int i = 0; i < s.length; i++)
		{
			if(s[i])
			{
				makeDirty(c, i);
			}
		}
	}

	@Override
	public void sendChunkSection(Chunk c, int bitmask)
	{
		for(Player i : P.getPlayersWithinViewOf(c))
		{
			sendChunkSection(c, bitmask, i);
		}
	}

	@Override
	public void sendChunkSection(Chunk c, int bitmask, Player p)
	{
		PacketPlayOutMapChunk map = new PacketPlayOutMapChunk(((CraftChunk) c).getHandle(), bitmask);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(map);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void sendBlockChange(Location l, BlockType t, Player player)
	{
		player.sendBlockChange(l, t.getMaterial(), t.getData());
	}

	@Override
	public void sendMultiBlockChange(Chunk c, GList<Location> points) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException
	{
		PacketPlayOutMultiBlockChange mb = new PacketPlayOutMultiBlockChange();
		PacketPlayOutMultiBlockChange.MultiBlockChangeInfo[] chs = new PacketPlayOutMultiBlockChange.MultiBlockChangeInfo[points.size()];
		ChunkCoordIntPair cp = new ChunkCoordIntPair(c.getX(), c.getZ());
		Field fcp = PacketPlayOutMultiBlockChange.class.getDeclaredField("a");
		Field fchs = PacketPlayOutMultiBlockChange.class.getDeclaredField("b");
		fcp.setAccessible(true);
		fchs.setAccessible(true);

		for(int i = 0; i < points.size(); i++)
		{
			net.minecraft.server.v1_12_R1.Block b = CraftMagicNumbers.getBlock((CraftBlock) points.get(i).getBlock());
			IBlockData d = b.getBlockData();
			int x = points.get(i).getBlockX();
			int y = points.get(i).getBlockY();
			int z = points.get(i).getBlockZ();
			x &= 15;
			z &= 15;
			chs[i] = mb.new MultiBlockChangeInfo((short) (x << 12 | z << 8 | y), d);
		}

		fcp.set(mb, cp);
		fchs.set(mb, chs);

		for(Player i : P.getPlayersWithinViewOf(c))
		{
			((CraftPlayer) i).getHandle().playerConnection.sendPacket(mb);
		}
	}

	private int getBitMask(boolean[] modifiedSections)
	{
		int bitMask = 0;

		for(int section = 0; section < modifiedSections.length; section++)
		{
			if(modifiedSections[section])
			{
				bitMask += 1 << section;
			}
		}

		return bitMask;
	}

	private int getSection(int y)
	{
		return y >> 4;
	}

	@Override
	public int getBitmask(Chunk c)
	{
		return getBitMask(getValidSections(c));
	}

	@Override
	public boolean[] getValidSections(Chunk c)
	{
		boolean[] f = new boolean[16];
		Arrays.fill(f, false);

		for(ChunkSection i : ((CraftChunk) c).getHandle().getSections())
		{
			if(i == null)
			{
				continue;
			}

			System.out.println(i.getYPosition() / 16);
			f[i.getYPosition() / 16] = true;
		}

		return f;
	}

	@Override
	public void notifyEntity(Entity e)
	{
		for(Player i : P.getPlayersWithinViewOf(e.getLocation().getChunk()))
		{
			notifyEntity(e, i);
		}
	}

	@Override
	public void sendUnload(Chunk c)
	{
		for(Player i : P.getPlayersWithinViewOf(c))
		{
			sendUnload(c, i);
		}
	}

	@Override
	public void sendReload(Chunk c)
	{
		sendUnload(c);
		boolean[] bits = new boolean[16];
		Arrays.fill(bits, true);
		sendChunkSection(c, getBitMask(bits));
	}

	@Override
	public void makeFullyDirty(Chunk c)
	{
		for(int i = 0; i < 16; i++)
		{
			makeDirty(c, i);
		}
	}

	@Override
	public void sendReload(Chunk c, Player p)
	{
		sendUnload(c, p);
		boolean[] bits = new boolean[16];
		Arrays.fill(bits, true);
		sendChunkSection(c, getBitMask(bits), p);
	}

	@Override
	public void notifyEntity(Entity e, Player p)
	{
		PacketPlayOutEntity px = new PacketPlayOutEntity(e.getEntityId());
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(px);
	}

	@Override
	public void sendUnload(Chunk c, Player p)
	{
		PacketPlayOutUnloadChunk px = new PacketPlayOutUnloadChunk(c.getX(), c.getZ());
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(px);
	}

	@Override
	public void setBiome(World w, int x, int z, Biome b)
	{
		w.setBiome(x, z, b);
		makeFullyDirty(w.getChunkAt(w.getBlockAt(x, 0, z)));
	}
}