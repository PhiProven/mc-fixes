package ocd.mc196542.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc196542.LightStorageAccessor;

@Mixin(LightStorage.class)
public abstract class LightStorageMixin implements LightStorageAccessor
{
    @Shadow
    protected abstract ChunkNibbleArray getLightSection(final long sectionPos, final boolean cached);

    @Override
    @Invoker("getLightSection")
    public abstract ChunkNibbleArray callGetLightSection(final long sectionPos, final boolean cached);

    @Redirect(
        method = "get(J)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ChunkNibbleArray;get(III)I"
        )
    )
    private int getLight(final ChunkNibbleArray lightmap, final int x, final int y, final int z, final long blockPos)
    {
        return this.getLight(lightmap, blockPos, x, y, z);
    }

    @Override
    public int getLight(final ChunkNibbleArray lightmap, final long blockPos, final int x, final int y, final int z)
    {
        return lightmap == null ? this.getLightWithoutLightmap(blockPos) : lightmap.get(x, y, z);
    }

    @Unique
    protected int getLightWithoutLightmap(final long blockPos)
    {
        return 0;
    }
}
