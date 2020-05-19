package ocd.mc170010.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc170010.InitialLightingAccessor;

@Mixin(LightStorage.class)
public abstract class LightStorageMixin implements InitialLightingAccessor
{
    @Override
    public void forceloadLightmap(final long pos)
    {
    }

    @Override
    public void unloadForcedLightmap(final long pos)
    {
    }
}
