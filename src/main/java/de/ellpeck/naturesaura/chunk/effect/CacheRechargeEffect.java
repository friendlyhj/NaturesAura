package de.ellpeck.naturesaura.chunk.effect;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.chunk.IDrainSpotEffect;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.potion.ModPotions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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

public class CacheRechargeEffect implements IDrainSpotEffect {

    public static final ResourceLocation NAME = new ResourceLocation(NaturesAura.MOD_ID, "cache_recharge");

    private int amount;

    private boolean calcValues(int aura) {
        if (aura < 1500000)
            return false;
        this.amount = Math.min(MathHelper.ceil(aura / 250F), aura - 1500000);
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
        return new ItemStack(ModItems.AURA_CACHE);
    }

    @Override
    public void update(World world, Chunk chunk, IAuraChunk auraChunk, int aura) {
        if (!this.calcValues(aura))
            return;
        for (ClassInheritanceMultiMap<Entity> entityList : chunk.getEntityLists()) {
            for (EntityPlayer player : entityList.getByClass(EntityPlayer.class)) {
                if (NaturesAuraAPI.instance().isEffectPowderActive(world, player.getPosition(), NAME))
                    continue;
                if (NaturesAuraAPI.instance().insertAuraIntoPlayer(player, this.amount, true)) {
                    NaturesAuraAPI.instance().insertAuraIntoPlayer(player, this.amount, false);
                    auraChunk.drainAura(BlockPos.ORIGIN, this.amount);
                }
            }
        }
    }

    @Override
    public boolean appliesHere(Chunk chunk, IAuraChunk auraChunk, IAuraType type) {
        return ModConfig.enabledFeatures.cacheRechargeEffect;
    }

    @Override
    public ResourceLocation getName() {
        return NAME;
    }
}
