package ocd.mc170010.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import ocd.mc170010.InitialLightingAccessor;

@Mixin(LightingProvider.class)
public abstract class LightingProviderMixin implements InitialLightingAccessor
{
    @Shadow
    @Final
    private ChunkLightProvider<?, ?> skyLightProvider;

    @Override
    public void forceloadLightmap(final long pos)
    {
        if (this.skyLightProvider != null)
            ((InitialLightingAccessor) this.skyLightProvider).forceloadLightmap(pos);
    }

    @Override
    public void unloadForcedLightmap(final long pos)
    {
        if (this.skyLightProvider != null)
            ((InitialLightingAccessor) this.skyLightProvider).unloadForcedLightmap(pos);
    }
}
