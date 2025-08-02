package de.ellpeck.naturesaura.chunk.effect;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.chunk.IDrainSpotEffect;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class GrassDieEffect implements IDrainSpotEffect {

    public static final ResourceLocation NAME = new ResourceLocation(NaturesAura.MOD_ID, "grass_die");

    private int amount;

    private boolean calcValues(int aura) {
        if (aura < 0) {
            this.amount = Math.min(300, MathHelper.ceil(Math.abs(aura) / 10000F));
        }
        return false;
    }

    @Override
    public int isActiveHere(EntityPlayer player, Chunk chunk, IAuraChunk auraChunk, int aura) {
        return this.calcValues(aura) && IDrainSpotEffect.isInChunk(player, chunk) ? 1 : -1;
    }

    @Override
    public ItemStack getDisplayIcon() {
        return new ItemStack(ModBlocks.DECAYED_LEAVES);
    }

    @Override
    public void update(World world, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if (!this.calcValues(aura))
            return;
        for (int i = this.amount / 2 + world.rand.nextInt(this.amount / 2); i >= 0; i--) {
            int xInChunk = world.rand.nextInt(16);
            int zInChunk = world.rand.nextInt(16);
            BlockPos grassPos = new BlockPos(
                    chunk.x << 4 + xInChunk,
                    world.rand.nextInt(chunk.getHeightValue(xInChunk, zInChunk)),
                    chunk.z << 4 + zInChunk
            );
            if (world.isBlockLoaded(grassPos)) {
                IBlockState state = world.getBlockState(grassPos);
                Block block = state.getBlock();

                IBlockState newState = null;
                if (block instanceof BlockLeaves) {
                    newState = ModBlocks.DECAYED_LEAVES.getDefaultState();
                } else if (block instanceof BlockGrass) {
                    newState = Blocks.DIRT.getDefaultState()
                            .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
                } else if (block instanceof BlockBush) {
                    newState = Blocks.AIR.getDefaultState();
                }
                if (newState != null)
                    world.setBlockState(grassPos, newState);
            }
        }
    }

    @Override
    public boolean appliesHere(Chunk chunk, IAuraChunk auraChunk, IAuraType type) {
        return ModConfig.enabledFeatures.grassDieEffect && type.isSimilar(NaturesAuraAPI.TYPE_OVERWORLD);
    }

    @Override
    public ResourceLocation getName() {
        return NAME;
    }
}
