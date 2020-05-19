package ocd.mc170010.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightStorage;
import net.minecraft.world.chunk.light.SkyLightStorage;
import net.minecraft.world.chunk.light.SkyLightStorage.Data;
import ocd.mc170010.InitialLightingAccessor;

@Mixin(SkyLightStorage.class)
public abstract class SkyLightStorageMixin extends LightStorage<SkyLightStorage.Data> implements InitialLightingAccessor
{
    protected SkyLightStorageMixin(final LightType lightType, final ChunkProvider chunkProvider, final Data lightData)
    {
        super(lightType, chunkProvider, lightData);
    }

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

    @Inject(
        method = "method_20809(J)V",
        at = @At(
            value = "HEAD"
        ),
        cancellable = true
    )
    private void disable_method_20809(final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Inject(
        method = "method_20810(J)V",
        at = @At(
            value = "HEAD"
        ),
        cancellable = true
    )
    private void disable_method_20810(final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Unique
    private final LongSet preInitSkylightChunks = new LongOpenHashSet();

    @Override
    public void forceloadLightmap(final long pos)
    {
        this.preInitSkylightChunks.add(pos);
        this.updateLevel(Long.MAX_VALUE, ChunkSectionPos.asLong(ChunkSectionPos.getX(pos), 16, ChunkSectionPos.getZ(pos)), 1, true);
    }

    @Override
    public void unloadForcedLightmap(final long pos)
    {
        if (this.preInitSkylightChunks.remove(pos))
            this.updateLevel(Long.MAX_VALUE, ChunkSectionPos.asLong(ChunkSectionPos.getX(pos), 16, ChunkSectionPos.getZ(pos)), 2, false);
    }

    @Override
    protected int getInitialLevel(final long id)
    {
        return Math.min(super.getInitialLevel(id), ChunkSectionPos.getY(id) == 16 && this.preInitSkylightChunks.contains(ChunkSectionPos.withZeroZ(id)) ? 1 : 2);
    }
}
