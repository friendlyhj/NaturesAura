package de.ellpeck.naturesaura.packet;

import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.chunk.AuraChunk;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.HashMap;
import java.util.Map;

public class PacketAuraChunk implements IMessage {

    private int chunkX;
    private int chunkZ;
    private int aura;

    public PacketAuraChunk(int chunkX, int chunkZ, int aura) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.aura = aura;
    }

    public PacketAuraChunk() {

    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.aura = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.chunkX);
        buf.writeInt(this.chunkZ);
        buf.writeInt(this.aura);
    }

    public static class Handler implements IMessageHandler<PacketAuraChunk, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketAuraChunk message, MessageContext ctx) {
            NaturesAura.proxy.scheduleTask(() -> {
                World world = Minecraft.getMinecraft().world;
                if (world != null) {
                    Chunk chunk = world.getChunk(message.chunkX, message.chunkZ);
                    if (chunk.hasCapability(NaturesAuraAPI.capAuraChunk, null)) {
                        AuraChunk auraChunk = (AuraChunk) chunk.getCapability(NaturesAuraAPI.capAuraChunk, null);
                        auraChunk.setAura(message.aura);
                    }
                }
            });

            return null;
        }
    }
}