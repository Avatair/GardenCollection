package com.jaquadro.minecraft.modularpots.block;

import com.jaquadro.minecraft.modularpots.ModularPots;
import com.jaquadro.minecraft.modularpots.client.ClientProxy;
import com.jaquadro.minecraft.modularpots.tileentity.TileEntityLargePot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;

import static com.jaquadro.minecraft.modularpots.block.LargePot.Direction.*;

public class LargePot extends BlockContainer
{
    public enum Direction {
        North (1 << 0),
        East (1 << 1),
        South (1 << 2),
        West (1 << 3),
        NorthWest (1 << 4),
        NorthEast (1 << 5),
        SouthEast (1 << 6),
        SouthWest (1 << 7);

        private final int flag;

        Direction (int flag) {
            this.flag = flag;
        }

        public int getFlag () {
            return this.flag;
        }

        public static boolean isSet (int bitflags, Direction direction) {
            return (bitflags & direction.flag) != 0;
        }

        public static int set (int bitflags, Direction direction) {
            return bitflags | direction.flag;
        }

        public static int clear (int bitflags, Direction direction) {
            return bitflags & ~direction.flag;
        }

        public static int setOrClear (int bitflags, Direction direction, boolean set) {
            return set ? set(bitflags, direction) : clear(bitflags, direction);
        }
    }

    private boolean colored;

    @SideOnly(Side.CLIENT)
    private IIcon iconSide;

    @SideOnly(Side.CLIENT)
    private IIcon[] iconArray;

    public LargePot (boolean colored) {
        super(Material.clay);

        this.colored = colored;
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public void addCollisionBoxesToList (World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity colliding) {
        float dim = .0625f;

        TileEntityLargePot te = getTileEntity(world, x, y, z);
        if (te == null || te.getSubstrate() == null)
            setBlockBounds(0, 0, 0, 1, dim, 1);
        else
            setBlockBounds(0, 0, 0, 1, 1 - dim, 1);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, colliding);

        int connected = (te != null) ? te.getConnectedFlags() : 0;
        if (!Direction.isSet(connected, West)) {
            setBlockBounds(0, 0, 0, dim, 1, 1);
            super.addCollisionBoxesToList(world, x, y, z, mask, list, colliding);
        }
        if (!Direction.isSet(connected, North)) {
            setBlockBounds(0, 0, 0, 1, 1, dim);
            super.addCollisionBoxesToList(world, x, y, z, mask, list, colliding);
        }
        if (!Direction.isSet(connected, East)) {
            setBlockBounds(1 - dim, 0, 0, 1, 1, 1);
            super.addCollisionBoxesToList(world, x, y, z, mask, list, colliding);
        }
        if (!Direction.isSet(connected, South)) {
            setBlockBounds(0, 0, 1 - dim, 1, 1, 1);
            super.addCollisionBoxesToList(world, x, y, z, mask, list, colliding);
        }

        setBlockBoundsForItemRender();
    }

    public void setRenderStep (int step) {
        float dim = .0625f;

        switch (step) {
            case 1:
                setBlockBounds(0, 0, 0, dim, 1, 1);
                break;
            case 2:
                setBlockBounds(1 - dim, 0, 0, 1, 1, 1);
                break;
            case 3:
                setBlockBounds(0, 0, 0, 1, 1, dim);
                break;
            case 4:
                setBlockBounds(0, 0, 1 - dim, 1, 1, 1);
                break;
        }
    }

    @Override
    public void setBlockBoundsForItemRender () {
        setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    @Override
    public boolean isOpaqueCube () {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock () {
        return false;
    }

    @Override
    public int getRenderType () {
        return ClientProxy.largePotRenderID;
    }

    @Override
    public boolean shouldSideBeRendered (IBlockAccess blockAccess, int x, int y, int z, int side) {
        switch (side) {
            case 0:
                y++;
                break;
            case 1:
                y--;
                break;
            case 2:
                z++;
                break;
            case 3:
                z--;
                break;
            case 4:
                x++;
                break;
            case 5:
                x--;
                break;
        }

        if (side >= 2 && side < 6) {
            TileEntityLargePot te = getTileEntity(blockAccess, x, y, z);
            if (te != null) {
                int flags = te.getConnectedFlags();
                switch (side) {
                    case 2:
                        return !Direction.isSet(flags, North);
                    case 3:
                        return !Direction.isSet(flags, Direction.South);
                    case 4:
                        return !Direction.isSet(flags, Direction.West);
                    case 5:
                        return !Direction.isSet(flags, Direction.East);
                }
            }
            return true;
        }
        return side != 1;
    }

    @Override
    public boolean canSustainPlant (IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
        TileEntityLargePot te = getTileEntity(world, x, y, z);
        if (te == null || te.getSubstrate() == null)
            return false;

        EnumPlantType plantType = plantable.getPlantType(world, x, y + 1, z);
        Block plant = plantable.getPlant(world, x, y + 1, z);
        Block substrate = Block.getBlockFromItem(te.getSubstrate());

        if (plant == Blocks.cactus)
            return substrate == Blocks.sand;

        return plantType == EnumPlantType.Crop
            && (substrate == Blocks.farmland || substrate == Blocks.dirt);
    }

    @Override
    public boolean isFertile (World world, int x, int y, int z) {
        return true;
    }

    @Override
    public void onPostBlockPlaced (World world, int x, int y, int z, int meta) {
        calculateConnectedness(world, x, y, z);
        notify8Neighbors(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange (World world, int x, int y, int z, Block block) {
        if (world.isRemote)
            return;

        //if (block == this)
        //    calculateConnectedness(world, x, y, z);

        if (y >= world.getHeight() - 1)
            return;

        Block overBlock = world.getBlock(x, y + 1, z);
        if (overBlock.isAir(world, x, y, z)) {
            TileEntityLargePot tileEntity = getTileEntity(world, x, y, z);
            if (tileEntity != null) {
                tileEntity.setItem(null, 0);
                tileEntity.markDirty();
            }
        }
    }

    private void calculateConnectedness (World world, int x, int y, int z) {
        calculateConnectedness(world, x, y, z, getTileEntity(world, x, y, z), 0);
    }

    private void calculateConnectedness (World world, int x, int y, int z, int invalid) {
        calculateConnectedness(world, x, y, z, getTileEntity(world, x, y, z), invalid);
    }

    private void calculateConnectedness (World world, int x, int y, int z, TileEntityLargePot te, int invalid) {
        if (te == null)
            return;

        int flags = te.getConnectedFlags();
        flags = Direction.setOrClear(flags, North, isCompatibleNeighbor(world, x, y, z, 0, -1));
        flags = Direction.setOrClear(flags, Direction.East, isCompatibleNeighbor(world, x, y, z, 1, 0));
        flags = Direction.setOrClear(flags, Direction.South, isCompatibleNeighbor(world, x, y, z, 0, 1));
        flags = Direction.setOrClear(flags, Direction.West, isCompatibleNeighbor(world, x, y, z, -1, 0));
        flags = Direction.setOrClear(flags, Direction.NorthWest, isCompatibleNeighbor(world, x, y, z, -1, -1));
        flags = Direction.setOrClear(flags, Direction.NorthEast, isCompatibleNeighbor(world, x, y, z, 1, -1));
        flags = Direction.setOrClear(flags, Direction.SouthEast, isCompatibleNeighbor(world, x, y, z, 1, 1));
        flags = Direction.setOrClear(flags, Direction.SouthWest, isCompatibleNeighbor(world, x, y, z, -1, 1));

        flags &= ~invalid;

        if (flags != te.getConnectedFlags()) {
            te.setConnectedFlags(flags);
            te.markDirty();
            world.markBlockForUpdate(x, y, z);
        }
    }

    private boolean isCompatibleNeighbor (World world, int x, int y, int z, int dx, int dz) {
        Block block = world.getBlock(x + dx, y, z + dz);
        if (block != this)
            return false;
        if (world.getBlockMetadata(x, y, z) != world.getBlockMetadata(x + dx, y, z + dz))
            return false;

        LargePot pot = (LargePot) block;
        TileEntityLargePot teThis = getTileEntity(world, x, y, z);
        TileEntityLargePot teThat = getTileEntity(world, x + dx, y, z + dz);
        if (teThis == null || teThat == null)
            return false;

        if (teThis.getSubstrate() != teThat.getSubstrate() || teThis.getSubstrateData() != teThat.getSubstrateData())
            return false;

        return true;
    }

    private void notify8Neighbors (World world, int x, int y, int z) {
        calculateConnectedness(world, x - 1, y, z);
        calculateConnectedness(world, x + 1, y, z);
        calculateConnectedness(world, x, y, z - 1);
        calculateConnectedness(world, x, y, z + 1);
        calculateConnectedness(world, x - 1, y, z - 1);
        calculateConnectedness(world, x - 1, y, z + 1);
        calculateConnectedness(world, x + 1, y, z - 1);
        calculateConnectedness(world, x + 1, y, z + 1);
    }

    private void notify8NeighborsRemoval (World world, int x, int y, int z) {
        calculateConnectedness(world, x - 1, y, z, East.getFlag());
        calculateConnectedness(world, x + 1, y, z, West.getFlag());
        calculateConnectedness(world, x, y, z - 1, South.getFlag());
        calculateConnectedness(world, x, y, z + 1, North.getFlag());
        calculateConnectedness(world, x - 1, y, z - 1, SouthEast.getFlag());
        calculateConnectedness(world, x - 1, y, z + 1, NorthEast.getFlag());
        calculateConnectedness(world, x + 1, y, z - 1, SouthWest.getFlag());
        calculateConnectedness(world, x + 1, y, z + 1, NorthWest.getFlag());
    }

    @Override
    public void breakBlock (World world, int x, int y, int z, Block block, int data) {
        TileEntityLargePot te = getTileEntity(world, x, y, z);
        if (te != null && te.getSubstrate() != null) {
            ItemStack item = new ItemStack(te.getSubstrate(), 1, te.getSubstrateData());
            dropBlockAsItem(world, x, y, z, item);
        }

        if (te != null && te.getFlowerPotItem() != null) {
            ItemStack item = new ItemStack(te.getFlowerPotItem(), 1, te.getFlowerPotData());
            dropBlockAsItem(world, x, y, z, item);
        }

        super.breakBlock(world, x, y, z, block, data);
    }

    @Override
    public void onBlockPreDestroy (World world, int x, int y, int z, int data) {
        notify8NeighborsRemoval(world, x, y, z);
    }

    @Override
    public boolean onBlockActivated (World world, int x, int y, int z, EntityPlayer player, int side, float vx, float vy, float vz) {
        if (side != 1)
            return false;

        ItemStack itemStack = player.inventory.getCurrentItem();
        if (itemStack == null)
            return false;

        TileEntityLargePot tileEntity = getTileEntity(world, x, y, z);
        if (tileEntity == null) {
            tileEntity = new TileEntityLargePot();
            world.setTileEntity(x, y, z, tileEntity);
        }

        IPlantable plantable = null;
        Item item = itemStack.getItem();
        if (item instanceof IPlantable)
            plantable = (IPlantable) item;
        else if (item instanceof ItemBlock) {
            Block itemBlock = Block.getBlockFromItem(item);
            if (itemBlock instanceof IPlantable)
                plantable = (IPlantable) itemBlock;
        }

        if (tileEntity.getSubstrate() == null && isValidSubstrate(item)) {
            addSubstrate(tileEntity, itemStack.getItem(), itemStack.getItemDamage());
            world.markBlockForUpdate(x, y, z);

            calculateConnectedness(world, x, y, z);
            notify8Neighbors(world, x, y, z);
        }
        else if (plantable != null && canSustainPlantActivated(world, x, y, z, plantable)) {
            if (!enoughAirAbove(world, x, y, z, plantable))
                return false;

            Block itemBlock = plantable.getPlant(world, x, y, z);

            world.setBlock(x, y + 1, z, ModularPots.largePotPlantProxy, itemStack.getItemDamage(), 3);
            if (itemBlock instanceof BlockDoublePlant || itemBlock.getRenderType() == 40)
                world.setBlock(x, y + 2, z, ModularPots.largePotPlantProxy, itemStack.getItemDamage() | 8, 3);

            tileEntity.setItem(itemStack.getItem(), itemStack.getItemDamage());
            tileEntity.markDirty();
        }
        else
            return false;

        if (!player.capabilities.isCreativeMode && --itemStack.stackSize <= 0)
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);

        return true;
    }

    private boolean enoughAirAbove (IBlockAccess world, int x, int y, int z, IPlantable plant) {
        Block plantBlock = plant.getPlant(world, x, y + 1, z);
        int height = (plantBlock instanceof BlockDoublePlant) ? 2 : 1;

        boolean enough = true;
        for (int i = 0; i < height; i++)
            enough &= world.isAirBlock(x, y + 1 + i, z);

        return enough;
    }

    private void addSubstrate (TileEntityLargePot tileEntity, Item item, int data) {
        tileEntity.setSubstrate(item, data);
        tileEntity.markDirty();
    }

    private boolean isValidSubstrate (Item item) {
        Block block = Block.getBlockFromItem(item);
        if (block == null)
            return false;

        return block == Blocks.dirt
            || block == Blocks.sand
            || block == Blocks.gravel
            || block == Blocks.soul_sand
            || block == Blocks.grass;
    }

    private boolean canSustainPlantActivated (IBlockAccess world, int x, int y, int z, IPlantable plantable) {
        TileEntityLargePot te = getTileEntity(world, x, y, z);
        if (te == null || te.getSubstrate() == null)
            return false;

        Block substrate = Block.getBlockFromItem(te.getSubstrate());
        if (substrate == null)
            return false;

        EnumPlantType plantType = plantable.getPlantType(world, x, y + 1, z);
        Block plant = plantable.getPlant(world, x, y + 1, z);

        if (plant == Blocks.cactus)
            return false;

        switch (plantType) {
            case Desert:
                return substrate == Blocks.sand;
            case Nether:
                return substrate == Blocks.soul_sand;
            //case Crop:
            //    return substrate == Blocks.dirt || substrate == Blocks.farmland;
            case Cave:
                return true;
            case Plains:
                return substrate == Blocks.grass || substrate == Blocks.dirt;
            case Beach:
                return substrate == Blocks.grass || substrate == Blocks.dirt || substrate == Blocks.sand;
            default:
                return false;
        }
    }

    public TileEntityLargePot getTileEntity (IBlockAccess world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        return (tileEntity != null && tileEntity instanceof  TileEntityLargePot) ? (TileEntityLargePot) tileEntity : null;
    }

    @Override
    public TileEntity createNewTileEntity (World world, int data) {
        return new TileEntityLargePot();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon (int side, int data) {
        if (colored)
            return iconArray[data % 16];

        return iconSide;
    }

    @Override
    public void getSubBlocks (Item item, CreativeTabs creativeTabs, List blockList) {
        if (colored) {
            for (int i = 0; i < 16; i++)
                blockList.add(new ItemStack(item, 1, i));
        }
        else
            blockList.add(new ItemStack(item, 1, 0));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons (IIconRegister iconRegister) {
        iconSide = iconRegister.registerIcon(ModularPots.MOD_ID + ":large_pot");

        if (colored) {
            iconArray = new IIcon[16];
            for (int i = 0; i < 16; i++) {
                String colorName = ItemDye.field_150921_b[getBlockFromDye(i)];
                iconArray[i] = iconRegister.registerIcon(ModularPots.MOD_ID + ":large_pot_" + colorName);
            }
        }
    }

    public static int getBlockFromDye (int index) {
        return index & 15;
    }
}
