package de.ellpeck.naturesaura.chunk.effect;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.chunk.IDrainSpotEffect;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.packet.PacketHandler;
import de.ellpeck.naturesaura.packet.PacketParticles;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class PlantBoostEffect implements IDrainSpotEffect {

    public static final ResourceLocation NAME = new ResourceLocation(NaturesAura.MOD_ID, "plant_boost");

    private int amount;

    private boolean calcValues(int aura) {
        if (aura < 1500000)
            return false;
        this.amount = Math.min(45, MathHelper.ceil(Math.abs(aura) / 100000F));
        if (this.amount <= 1)
            return false;
        return true;
    }

    @Override
    public int isActiveHere(EntityPlayer player, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if (!this.calcValues(aura))
            return -1;
        if (!IDrainSpotEffect.isInChunk(player, chunk))
            return -1;
        if (NaturesAuraAPI.instance().isEffectPowderActive(player.world, player.getPosition(), NAME))
            return 0;
        return 1;
    }

    @Override
    public ItemStack getDisplayIcon() {
        return new ItemStack(Items.WHEAT_SEEDS);
    }

    @Override
    public void update(World world, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if (!this.calcValues(aura))
            return;
        for (int i = this.amount / 2 + world.rand.nextInt(this.amount / 2); i >= 0; i--) {
            int x = chunk.x << 4 + world.rand.nextInt(16);
            int z = chunk.z << 4 + world.rand.nextInt(16);
            BlockPos plantPos = new BlockPos(x, world.getHeight(x, z), z);
            if (world.isBlockLoaded(plantPos)) {
                if (NaturesAuraAPI.instance().isEffectPowderActive(world, plantPos, NAME))
                    continue;

                IBlockState state = world.getBlockState(plantPos);
                Block block = state.getBlock();
                if (block instanceof IGrowable &&
                        block != Blocks.TALLGRASS && block != Blocks.GRASS && block != Blocks.DOUBLE_PLANT) {
                    IGrowable growable = (IGrowable) block;
                    if (growable.canGrow(world, plantPos, state, false)) {
                        growable.grow(world, world.rand, plantPos, state);

                        BlockPos closestSpot = IAuraChunk.getHighestSpot(world, plantPos, 25, plantPos);
                        IAuraChunk.getAuraChunk(world, closestSpot).drainAura(closestSpot, 3500);

                        PacketHandler.sendToAllAround(world, plantPos, 32,
                                new PacketParticles(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 6));
                    }
                }
            }
        }
    }

    @Override
    public boolean appliesHere(Chunk chunk, IAuraChunk auraChunk, IAuraType type) {
        return ModConfig.enabledFeatures.plantBoostEffect && type.isSimilar(NaturesAuraAPI.TYPE_OVERWORLD);
    }

    @Override
    public ResourceLocation getName() {
        return NAME;
    }
}
