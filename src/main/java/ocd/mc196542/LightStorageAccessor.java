package ocd.mc196542;

import net.minecraft.world.chunk.ChunkNibbleArray;

public interface LightStorageAccessor
{
    ChunkNibbleArray callGetLightSection(long sectionPos, boolean cached);

    int getLight(ChunkNibbleArray lightmap, long blockPos, int x, int y, int z);
}
