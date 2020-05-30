package ocd.mc170012;

import net.minecraft.world.chunk.ChunkStatus;

public interface IPersistentChunkStatus
{
    boolean isPersistent();

    ChunkStatus getPersistentStatus();
}
