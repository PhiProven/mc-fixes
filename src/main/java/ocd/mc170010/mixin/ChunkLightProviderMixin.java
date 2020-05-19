package ocd.mc170010.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc170010.InitialLightingAccessor;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin implements InitialLightingAccessor
{
    @Shadow
    @Final
    protected LightStorage<?> lightStorage;

    @Override
    public void forceloadLightmap(final long pos)
    {
        ((InitialLightingAccessor) this.lightStorage).forceloadLightmap(pos);
    }

    @Override
    public void unloadForcedLightmap(final long pos)
    {
        ((InitialLightingAccessor) this.lightStorage).unloadForcedLightmap(pos);
    }
}
