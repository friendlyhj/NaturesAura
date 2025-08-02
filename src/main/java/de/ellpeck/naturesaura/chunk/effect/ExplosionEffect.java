package de.ellpeck.naturesaura.chunk.effect;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.chunk.IDrainSpotEffect;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import jdk.nashorn.internal.ir.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ExplosionEffect implements IDrainSpotEffect {

    public static final ResourceLocation NAME = new ResourceLocation(NaturesAura.MOD_ID, "explosions");

    private float strength;
    private int dist;

    private boolean calcValues(World world, int aura) {
        if (aura > -IAuraChunk.DEFAULT_AURA / 2)
            return false;
        int chance = 140 - Math.abs(aura) / 20000;
        if (chance > 1 && world.rand.nextInt(chance) != 0)
            return false;
        this.strength = Math.min(Math.abs(aura) / 500000F, 5F);
        if (this.strength <= 0)
            return false;
        this.dist = MathHelper.clamp(Math.abs(aura) / 20000, 25, 100);
        return true;
    }

    @Override
    public int isActiveHere(EntityPlayer player, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if (aura > -IAuraChunk.DEFAULT_AURA / 2)
            return -1;
        if (!IDrainSpotEffect.isInChunk(player, chunk))
            return -1;
        return 1;
    }

    @Override
    public ItemStack getDisplayIcon() {
        return new ItemStack(Blocks.TNT);
    }

    @Override
    public void update(World world, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if(world.getTotalWorldTime() % 40 != 0)
            return;
        if(!this.calcValues(world, aura))
            return;

        BlockPos pos = new BlockPos(chunk.x << 4 + 7, chunk.getHeightValue(7, 7), chunk.z << 4 + 7);

        int x = MathHelper.floor(pos.getX() + world.rand.nextGaussian() * this.dist);
        int z = MathHelper.floor(pos.getZ() + world.rand.nextGaussian() * this.dist);
        BlockPos chosenPos = new BlockPos(x, world.getHeight(x, z), z);
        if (chosenPos.distanceSq(pos) <= this.dist * this.dist && world.isBlockLoaded(chosenPos)) {
            world.newExplosion(null,
                    chosenPos.getX() + 0.5, chosenPos.getY() + 0.5, chosenPos.getZ() + 0.5,
                    this.strength, false, true);
        }
    }

    @Override
    public boolean appliesHere(Chunk chunk, IAuraChunk auraChunk, IAuraType type) {
        return ModConfig.enabledFeatures.explosionEffect;
    }

    @Override
    public ResourceLocation getName() {
        return NAME;
    }
}
