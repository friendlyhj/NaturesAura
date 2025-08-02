package de.ellpeck.naturesaura.chunk.effect;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.chunk.IDrainSpotEffect;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.potion.ModPotions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class BreathlessEffect implements IDrainSpotEffect {

    public static final ResourceLocation NAME = new ResourceLocation(NaturesAura.MOD_ID, "breathless");

    private int amp;

    private boolean calcValues(int aura) {
        if (aura >= 0)
            return false;
        int dist = Math.min(Math.abs(aura) / 5000, 75);
        if (dist < 10)
            return false;
        this.amp = Math.min(MathHelper.floor(Math.abs(aura) / 25600F), 3);
        return true;
    }

    @Override
    public int isActiveHere(EntityPlayer player, Chunk chunk, IAuraChunk auraChunk, int aura) {
        return this.calcValues(aura) && IDrainSpotEffect.isInChunk(player, chunk) ? 1 : -1;
    }

    @Override
    public ItemStack getDisplayIcon() {
        return new ItemStack(Blocks.WOOL);
    }

    @Override
    public void update(World world, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if (world.getTotalWorldTime() % 100 != 48)
            return;
        if (!this.calcValues(aura))
            return;
        for (EntityLivingBase entity : IDrainSpotEffect.getEntitiesInChunk(chunk, EntityLivingBase.class)) {
            entity.addPotionEffect(new PotionEffect(ModPotions.BREATHLESS, 300, this.amp));
        }
    }

    @Override
    public boolean appliesHere(Chunk chunk, IAuraChunk auraChunk, IAuraType type) {
        return ModConfig.enabledFeatures.breathlessEffect;
    }

    @Override
    public ResourceLocation getName() {
        return NAME;
    }
}
