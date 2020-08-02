package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.BlockLightStorage;

@Mixin(BlockLightStorage.class)
public abstract class BlockLightStorageMixin extends LightStorageMixin
{
    @Override
    protected int getLightmapComplexityChange(final long blockPos, final int oldVal, final int newVal, final ChunkNibbleArray lightmap)
    {
        return newVal - oldVal;
    }

    @Override
    protected int getInitialLightmapComplexity(final long sectionPos, final ChunkNibbleArray lightmap)
    {
        int complexity = 0;

        for (int y = 0; y < 16; ++y)
            for (int z = 0; z < 16; ++z)
                for (int x = 0; x < 16; ++x)
                    complexity += lightmap.get(x, y, z);

        return complexity;
    }
}
