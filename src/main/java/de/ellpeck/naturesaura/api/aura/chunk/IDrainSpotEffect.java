package de.ellpeck.naturesaura.api.aura.chunk;

import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

public interface IDrainSpotEffect {
    static <T extends Entity> List<T> getEntitiesInChunk(Chunk chunk, Class<T> entityClass) {
        List<T> list = new ArrayList<>();
        for (ClassInheritanceMultiMap<Entity> entities : chunk.getEntityLists()) {
            entities.getByClass(entityClass).forEach(list::add);
        }
        return list;
    }

    static boolean isInChunk(Entity entity, Chunk chunk) {
        return entity.world.getChunk(entity.getPosition()) == chunk;
    }

    void update(World world, Chunk chunk, IAuraChunk auraChunk, int aura);

    boolean appliesHere(Chunk chunk, IAuraChunk auraChunk, IAuraType type);

    ResourceLocation getName();

    default int isActiveHere(EntityPlayer player, Chunk chunk, IAuraChunk auraChunk, int aura) {
        return -1;
    }

    default ItemStack getDisplayIcon() {
        return ItemStack.EMPTY;
    }
}
