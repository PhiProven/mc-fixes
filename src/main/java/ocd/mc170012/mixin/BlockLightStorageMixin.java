package ocd.mc170012.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.BlockLightStorage;
import net.minecraft.world.chunk.light.BlockLightStorage.Data;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc170012.BlockLightStorageAccessor;

@Mixin(BlockLightStorage.class)
public abstract class BlockLightStorageMixin extends LightStorage<BlockLightStorage.Data> implements BlockLightStorageAccessor
{
    private BlockLightStorageMixin(final LightType lightType, final ChunkProvider chunkProvider, final Data lightData)
    {
        super(lightType, chunkProvider, lightData);
    }

    @Unique
    private final LongSet lightEnabled = new LongOpenHashSet();

    @Override
    protected void setColumnEnabled(final long chunkPos, final boolean enable)
    {
        if (enable)
            this.lightEnabled.add(chunkPos);
        else
            this.lightEnabled.remove(chunkPos);
    }

    @Override
    public boolean isLightEnabled(final long chunkPos)
    {
        return this.lightEnabled.contains(chunkPos);
    }
}
