package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc196725.InitialLightingHandler;
import ocd.mc196725.LightStorageAccessor;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin implements InitialLightingHandler
{
    @Shadow
    @Final
    protected LightStorage<?> lightStorage;

    @Override
    public void enableSourceLight(final long chunkPos)
    {
        ((LightStorageAccessor) this.lightStorage).invokeSetColumnEnabled(chunkPos, true);
    }

    @Override
    public void enableLightUpdates(final long chunkPos)
    {
        ((LightStorageAccessor) this.lightStorage).setLightUpdatesEnabled(chunkPos, true);
    }

    /**
     * Change specification of the method.
     * Now controls both source light and light updates. Disabling now additionally removes all data associated to the chunk.
    */
    @Redirect(
        method = "setColumnEnabled(Lnet/minecraft/util/math/ChunkPos;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/LightStorage;setColumnEnabled(JZ)V"
        )
    )
    private void setColumnEnabled(final LightStorage<?> lightStorage, long chunkPos, final boolean enabled)
    {
        if (enabled)
        {
            this.enableSourceLight(chunkPos);
            this.enableLightUpdates(chunkPos);
        }
        else
            ((LightStorageAccessor) lightStorage).setLightUpdatesEnabled(chunkPos, false);
    }
}
