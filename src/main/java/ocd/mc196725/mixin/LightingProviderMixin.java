package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import ocd.mc196725.InitialLightingHandler;

@Mixin(LightingProvider.class)
public abstract class LightingProviderMixin implements InitialLightingHandler
{
    @Shadow
    @Final
    private ChunkLightProvider<?, ?> blockLightProvider;

    @Shadow
    @Final
    private ChunkLightProvider<?, ?> skyLightProvider;

    @Override
    public void enableSourceLight(final long chunkPos)
    {
        if (this.blockLightProvider != null)
            ((InitialLightingHandler) this.blockLightProvider).enableSourceLight(chunkPos);

        if (this.skyLightProvider != null)
            ((InitialLightingHandler) this.skyLightProvider).enableSourceLight(chunkPos);
    }

    @Override
    public void enableLightUpdates(final long chunkPos)
    {
        if (this.blockLightProvider != null)
            ((InitialLightingHandler) this.blockLightProvider).enableLightUpdates(chunkPos);

        if (this.skyLightProvider != null)
            ((InitialLightingHandler) this.skyLightProvider).enableLightUpdates(chunkPos);
    }
}
