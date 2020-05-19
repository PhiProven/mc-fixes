package ocd.mc170010.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.SkyLightStorage;

@Mixin(SkyLightStorage.class)
public abstract class SkyLightStorageMixin
{
    @Shadow
    protected abstract boolean isLightEnabled(long sectionPos);

    @Redirect(
        method = "createLightArray(J)Lnet/minecraft/world/chunk/ChunkNibbleArray;",
        at = @At(
            value = "NEW",
            target = "()Lnet/minecraft/world/chunk/ChunkNibbleArray;"
        )
    )
    private ChunkNibbleArray initializeLightmap(final long pos)
    {
        final ChunkNibbleArray ret = new ChunkNibbleArray();

        if (this.isLightEnabled(pos))
            Arrays.fill(ret.asByteArray(), (byte) -1);

        return ret;
    }
}
