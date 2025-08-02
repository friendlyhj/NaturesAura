package de.ellpeck.naturesaura.events;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.graph.Graph;
import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.chunk.AuraChunk;
import de.ellpeck.naturesaura.chunk.AuraChunkProvider;
import de.ellpeck.naturesaura.misc.WorldData;
import de.ellpeck.naturesaura.packet.PacketHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class CommonEvents {

    @SubscribeEvent
    public void onChunkCapsAttach(AttachCapabilitiesEvent<Chunk> event) {
        Chunk chunk = event.getObject();
        event.addCapability(new ResourceLocation(NaturesAura.MOD_ID, "aura"), new AuraChunkProvider(chunk));
    }

    @SubscribeEvent
    public void onWorldCapsAttach(AttachCapabilitiesEvent<World> event) {
        event.addCapability(new ResourceLocation(NaturesAura.MOD_ID, "data"), new WorldData());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote && event.phase == TickEvent.Phase.END) {
            if (event.world.getTotalWorldTime() % 20 == 0) {
                event.world.profiler.func_194340_a(() -> NaturesAura.MOD_ID + ":onWorldTick");
                Iterator<Chunk> chunks = event.world.getPersistentChunkIterable(((WorldServer) event.world).getPlayerChunkMap()
                        .getChunkIterator());
                List<AuraChunk> auraChunks = new ArrayList<>();
                while (chunks.hasNext()) {
                    Chunk chunk = chunks.next();
                    if (chunk.hasCapability(NaturesAuraAPI.capAuraChunk, null)) {
                        AuraChunk auraChunk = (AuraChunk) chunk.getCapability(NaturesAuraAPI.capAuraChunk, null);
                        auraChunks.add(auraChunk);
                    }
                }
                spreadAura(auraChunks);
                auraChunks.forEach(AuraChunk::update);
                event.world.profiler.endSection();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.world.isRemote && event.phase == TickEvent.Phase.END) {
            if (event.player.world.getTotalWorldTime() % 200 != 0)
                return;

            int aura = IAuraChunk.triangulateAuraInArea(event.player.world, event.player.getPosition(), 25);
            if (aura <= 0)
                Helper.addAdvancement(event.player, new ResourceLocation(NaturesAura.MOD_ID, "negative_imbalance"), "triggered_in_code");
            else if (aura >= 1500000)
                Helper.addAdvancement(event.player, new ResourceLocation(NaturesAura.MOD_ID, "positive_imbalance"), "triggered_in_code");
        }
    }

    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.getChunkInstance();
        if (!chunk.getWorld().isRemote && chunk.hasCapability(NaturesAuraAPI.capAuraChunk, null)) {
            AuraChunk auraChunk = (AuraChunk) chunk.getCapability(NaturesAuraAPI.capAuraChunk, null);
            PacketHandler.sendTo(event.getPlayer(), auraChunk.makePacket());
        }
    }

    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent event) {
        if (NaturesAura.MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(NaturesAura.MOD_ID, Config.Type.INSTANCE);
            ModConfig.initOrReload(true);
        }
    }

    private void spreadAura(List<AuraChunk> chunks) {
        if (chunks.isEmpty())
            return;
        List<AuraChunk> ordered = new ArrayList<>(chunks);
        ordered.sort(Comparator.comparingInt(AuraChunk::getAura));
        Deque<AuraChunk> deque = new ArrayDeque<>(ordered);
        Table<Integer, Integer, AuraChunk> chunkTable = HashBasedTable.create();
        for (AuraChunk chunk : ordered) {
            chunkTable.put(chunk.getChunk().x, chunk.getChunk().z, chunk);
        }
        boolean shouldDrain = true;
        boolean shouldFlow = true;
        while (shouldDrain || shouldFlow) {
            if (deque.isEmpty()) {
                break;
            }
            AuraChunk drain = deque.peekFirst();
            if (drain.getAura() >= 0) {
                shouldDrain = false;
            } else {
                deque.pollFirst();
                int toMove = Math.min(90000, -drain.getAura() / 8);
                List<IAuraChunk> drainSrcs = new ArrayList<>();
                for (int xOffset = -1; xOffset <= 1; xOffset++) {
                    for (int zOffset = -1; zOffset <= 1; zOffset++) {
                        if (xOffset == 0 && zOffset == 0)
                            continue;
                        AuraChunk neighbor = chunkTable.get(drain.getChunk().x + xOffset, drain.getChunk().z + zOffset);
                        if (neighbor != null && neighbor.getAura() > IAuraChunk.DEFAULT_AURA / 2) {
                            drainSrcs.add(neighbor);
                        }
                    }
                }
                for (IAuraChunk drainSrc : drainSrcs) {
                    drainSrc.drainAura(BlockPos.ORIGIN, toMove);
                }
                drain.storeAura(BlockPos.ORIGIN, toMove * drainSrcs.size());
            }
            if (deque.isEmpty()) {
                break;
            }
            AuraChunk flow = deque.peekLast();
            if (flow.getAura() <= IAuraChunk.DEFAULT_AURA * 2) {
                shouldFlow = false;
            } else {
                deque.pollLast();
                int toMove = Math.min(90000, (flow.getAura() - IAuraChunk.DEFAULT_AURA * 2) / 8);
                List<AuraChunk> flowDests = new ArrayList<>();
                List<AuraChunk> neighbors = new ArrayList<>();
                for (int xOffset = -1; xOffset <= 1; xOffset++) {
                    for (int zOffset = -1; zOffset <= 1; zOffset++) {
                        if (xOffset == 0 && zOffset == 0)
                            continue;
                        AuraChunk neighbor = chunkTable.get(flow.getChunk().x + xOffset, flow.getChunk().z + zOffset);
                        if (neighbor != null) {
                            neighbors.add(neighbor);
                            if (neighbor.getAura() < IAuraChunk.DEFAULT_AURA * 2) {
                                flowDests.add(neighbor);
                            }
                        }
                    }
                }

                if (!flowDests.isEmpty()) {
                    for (IAuraChunk flowDest : flowDests) {
                        flowDest.storeAura(BlockPos.ORIGIN, toMove);
                    }
                    flow.drainAura(BlockPos.ORIGIN, toMove * flowDests.size());
                } else if (!neighbors.isEmpty()) {
                    int totalAura = flow.getAura();
                    for (AuraChunk neighbor : neighbors) {
                        totalAura += neighbor.getAura();
                        chunkTable.remove(neighbor.getChunk().x, neighbor.getChunk().z);
                    }
                    int avgAura = totalAura / (neighbors.size() + 1);
                    int toFlow = Math.min(720000, flow.getAura() - avgAura);
                    flow.drainAura(BlockPos.ORIGIN, toFlow);
                    for (IAuraChunk neighbor : neighbors) {
                        int toStore = Math.min(720000 / neighbors.size(), avgAura - neighbor.getAura());
                        neighbor.storeAura(BlockPos.ORIGIN, toStore);
                    }
                }
            }
        }
    }
}
