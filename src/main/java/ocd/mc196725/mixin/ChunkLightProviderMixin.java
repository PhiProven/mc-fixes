package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc196725.ILightUpdatesHandler;
import ocd.mc196725.LightStorageAccessor;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin implements ILightUpdatesHandler
{
    @Shadow
    @Final
    protected LightStorage<?> lightStorage;

    @Override
    public void enableLightUpdates(final long chunkPos)
    {
        ((ILightUpdatesHandler) this.lightStorage).enableLightUpdates(chunkPos);
    }

    @Redirect(
        method = "setColumnEnabled(Lnet/minecraft/util/math/ChunkPos;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/LightStorage;setColumnEnabled(JZ)V"
        )
    )
    private void disableLightUpdates(final LightStorage<?> lightStorage, long chunkPos, final boolean enabled)
    {
        if (enabled)
            ((LightStorageAccessor) lightStorage).invokeSetColumnEnabled(chunkPos, true);
        else
            ((LightStorageAccessor) lightStorage).disableLightUpdates(chunkPos);
    }
}
