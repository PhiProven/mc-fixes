package ocd.mc170010.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

import net.minecraft.world.chunk.light.ChunkSkyLightProvider;

@Mixin(ChunkSkyLightProvider.class)
public abstract class ChunkSkyLightProviderMixin
{
    @ModifyConstant(
        method = "recalculateLevel(JJI)I",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/chunk/light/SkyLightStorage;isLightEnabled(J)Z"
            )
        ),
        constant = @Constant(
            intValue = 0,
            ordinal = 0
        )
    )
    private static int propagateDirectSkylightNeighbor(final int i)
    {
        return 1;
    }
}
