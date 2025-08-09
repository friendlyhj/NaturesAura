package de.ellpeck.naturesaura.chunk;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.chunk.IDrainSpotEffect;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.packet.PacketAuraChunk;
import de.ellpeck.naturesaura.packet.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class AuraChunk implements IAuraChunk {

    private final Chunk chunk;
    private final IAuraType type;
    private final AtomicInteger aura;
    private final List<IDrainSpotEffect> effects = new ArrayList<>();
    private boolean needsSync;

    public AuraChunk(Chunk chunk, IAuraType type) {
        this.chunk = chunk;
        this.type = type;
        this.aura = new AtomicInteger(DEFAULT_AURA);

        for (Supplier<IDrainSpotEffect> supplier : NaturesAuraAPI.DRAIN_SPOT_EFFECTS.values()) {
            IDrainSpotEffect effect = supplier.get();
            if (effect.appliesHere(this.chunk, this, this.type))
                this.effects.add(effect);
        }
    }

    @Override
    public int drainAura(BlockPos pos, int amount, boolean aimForZero, boolean simulate) {
        if (amount <= 0)
            return 0;
        int current = aura.get();
        if (current < 0 && current - amount > 0) // Underflow protection
            return 0;
        if (aimForZero) {
            if (current > 0 && current - amount < 0) {
                amount = current;
            }
        }
        if (!simulate) {
            aura.getAndAdd(-amount);
            this.markDirty();
        }
        return amount;
    }

    @Override
    public int drainAura(BlockPos pos, int amount) {
        return this.drainAura(pos, amount, false, false);
    }

    @Override
    public int storeAura(BlockPos pos, int amount, boolean aimForZero, boolean simulate) {
        if (amount <= 0)
            return 0;
        int current = aura.get();
        if (current > 0 && current + amount < 0) // Overflow protection
            return 0;
        if (aimForZero) {
            if (current < 0 && current + amount > 0) {
                amount = -current;
            }
        }
        if (!simulate) {
            aura.getAndAdd(amount);
            this.markDirty();
        }
        return amount;
    }

    @Override
    public int storeAura(BlockPos pos, int amount) {
        return this.storeAura(pos, amount, true, false);
    }

    @Override
    public int getDrainSpot(BlockPos pos) {
        return aura.get() - IAuraChunk.DEFAULT_AURA;
    }

    public void setAura(int aura) {
        this.aura.set(aura);
    }

    public int getAura() {
        return aura.get();
    }

    @Override
    public IAuraType getType() {
        return this.type;
    }

    @Override
    public void markDirty() {
        this.chunk.markDirty();
        this.needsSync = true;
    }

    public void update() {
        World world = this.chunk.getWorld();

        for (IDrainSpotEffect effect : this.effects) {
            world.profiler.func_194340_a(() -> effect.getName().toString());
            effect.update(world, this.chunk, this, aura.get());
            world.profiler.endSection();
        }

        if (this.needsSync) {
            PacketHandler.sendToAllLoaded(world,
                    new BlockPos(this.chunk.x * 16, 0, this.chunk.z * 16),
                    this.makePacket());
            this.needsSync = false;
        }
    }

    public IMessage makePacket() {
        return new PacketAuraChunk(this.chunk.x, this.chunk.z, this.aura.get());
    }

    public void getSpotsInArea(BiConsumer<BlockPos, Integer> consumer) {
        consumer.accept(new BlockPos(chunk.x << 4, chunk.getWorld().getSeaLevel(), chunk.z << 4), aura.get() - IAuraChunk.DEFAULT_AURA);
    }

    public void getActiveEffectIcons(EntityPlayer player, Map<ResourceLocation, Tuple<ItemStack, Boolean>> icons) {
        for (IDrainSpotEffect effect : this.effects) {
            Tuple<ItemStack, Boolean> alreadyThere = icons.get(effect.getName());
            if (alreadyThere != null && alreadyThere.getSecond())
                continue;
            int state = effect.isActiveHere(player, this.chunk, this, this.aura.get());
            if (state < 0)
                continue;
            ItemStack stack = effect.getDisplayIcon();
            if (stack.isEmpty())
                continue;
            icons.put(effect.getName(), new Tuple<>(stack, state == 0));
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("aura", this.aura.get());
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        if (compound.hasKey("aura")) {
            this.aura.set(compound.getInteger("aura"));
        }
    }

    public Chunk getChunk() {
        return chunk;
    }
}
