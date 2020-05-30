package ocd.mc170012.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import ocd.mc170012.IPersistentChunk;
import ocd.mc170012.IPersistentChunkStatus;

@Mixin(Chunk.class)
public interface ChunkMixin extends IPersistentChunk
{
    @Shadow
    ChunkStatus getStatus();

    @Override
    default ChunkStatus getPersistentStatus()
    {
        return ((IPersistentChunkStatus) this.getStatus()).getPersistentStatus();
    }
}
