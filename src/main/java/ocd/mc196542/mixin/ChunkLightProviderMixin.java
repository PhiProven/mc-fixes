package ocd.mc196542.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc196542.LightStorageAccessor;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin
{
    @Shadow
    @Final
    protected LightStorage<?> lightStorage;

    @Redirect(
        method = "getCurrentLevelFromSection(Lnet/minecraft/world/chunk/ChunkNibbleArray;J)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ChunkNibbleArray;get(III)I"
        )
    )
    private int getLight(final ChunkNibbleArray lightmap, final int x, final int y, final int z, final ChunkNibbleArray lightmap_, final long blockPos)
    {
        return ((LightStorageAccessor) this.lightStorage).getLight(lightmap, blockPos, x, y, z);
    }
}
