package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import ocd.mc196725.ILightUpdatesHandler;

@Mixin(LightingProvider.class)
public abstract class LightingProviderMixin implements ILightUpdatesHandler
{
    @Shadow
    @Final
    private ChunkLightProvider<?, ?> blockLightProvider;

    @Shadow
    @Final
    private ChunkLightProvider<?, ?> skyLightProvider;

    @Override
    public void enableLightUpdates(final long chunkPos)
    {
        if (this.blockLightProvider != null)
            ((ILightUpdatesHandler) this.blockLightProvider).enableLightUpdates(chunkPos);

        if (this.skyLightProvider != null)
            ((ILightUpdatesHandler) this.skyLightProvider).enableLightUpdates(chunkPos);
    }
}
