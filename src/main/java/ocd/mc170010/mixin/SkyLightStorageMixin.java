package ocd.mc170010.mixin;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkLightProvider;
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
        at = @At("HEAD"),
        cancellable = true
    )
    private void disable_method_20809(final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Inject(
        method = "method_20810(J)V",
        at = @At("HEAD"),
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

    @Unique
    private final LongSet initSkylightChunks = new LongOpenHashSet();

    @Shadow
    @Final
    private LongSet lightEnabled;

    @Shadow
    protected abstract void checkForUpdates();

    @Inject(
        method = "setLightEnabled(JZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void setLightEnabled(final long pos, final boolean enabled, final CallbackInfo ci)
    {
        if (enabled)
        {
            if (preInitSkylightChunks.contains(pos))
            {
                initSkylightChunks.add(pos);
                this.checkForUpdates();
            }
            else
                this.lightEnabled.add(pos);
        }
        else
        {
            this.lightEnabled.remove(pos);
            this.initSkylightChunks.remove(pos);
            this.checkForUpdates();
        }

        ci.cancel();
    }

    @Shadow
    protected abstract boolean isAboveMinHeight(int blockY);

    @Unique
    private static void spreadSourceSkylight(final LevelPropagatorAccessor lightProvider, final long src, final Direction dir)
    {
        final long dst = BlockPos.offset(src, dir);
        lightProvider.invokeUpdateLevel(src, dst, lightProvider.callGetPropagatedLevel(src, dst, 0), true);
    }

    @Inject(
        method = "updateLightArrays(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V",
        at = @At(
            value = "INVOKE",
            opcode = Opcodes.INVOKESPECIAL,
            shift = Shift.AFTER,
            target = "Lnet/minecraft/world/chunk/light/LightStorage;updateLightArrays(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V"
        )
    )
    private void initSkylight(final ChunkLightProvider<Data, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation, final CallbackInfo ci)
    {
        for (final LongIterator it = this.initSkylightChunks.iterator(); it.hasNext(); )
        {
            final long chunkPos = it.nextLong();

            final LevelPropagatorAccessor levelPropagator = (LevelPropagatorAccessor) lightProvider;
            final int minY = this.fillSkylightColumn(lightProvider, chunkPos);

            this.lightEnabled.add(chunkPos);
            this.preInitSkylightChunks.remove(chunkPos);
            this.updateLevel(Long.MAX_VALUE, ChunkSectionPos.asLong(ChunkSectionPos.getX(chunkPos), 16, ChunkSectionPos.getZ(chunkPos)), 2, false);

            if (this.hasLight(ChunkSectionPos.asLong(ChunkSectionPos.getX(chunkPos), minY, ChunkSectionPos.getZ(chunkPos))))
            {
                final long blockPos = BlockPos.asLong(ChunkSectionPos.getWorldCoord(ChunkSectionPos.getX(chunkPos)), ChunkSectionPos.getWorldCoord(minY), ChunkSectionPos.getWorldCoord(ChunkSectionPos.getZ(chunkPos)));

                for (int x = 0; x < 16; ++x)
                    for (int z = 0; z < 16; ++z)
                        spreadSourceSkylight(levelPropagator, BlockPos.add(blockPos, x, 16, z), Direction.DOWN);
            }

            for (int y = 16; y > minY; --y)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.getX(chunkPos), y, ChunkSectionPos.getZ(chunkPos));
                final long blockPos = BlockPos.asLong(ChunkSectionPos.getWorldCoord(ChunkSectionPos.getX(sectionPos)), ChunkSectionPos.getWorldCoord(y), ChunkSectionPos.getWorldCoord(ChunkSectionPos.getZ(sectionPos)));

                for (final Direction dir : Direction.Type.HORIZONTAL)
                {
                    if (!this.hasLight(ChunkSectionPos.offset(sectionPos, dir)))
                        continue;

                    final int ox = 15 * Math.max(dir.getOffsetX(), 0);
                    final int oz = 15 * Math.max(dir.getOffsetZ(), 0);

                    final int dx = Math.abs(dir.getOffsetZ());
                    final int dz = Math.abs(dir.getOffsetX());

                    for (int t = 0; t < 16; ++t)
                        for (int dy = 0; dy < 16; ++dy)
                            spreadSourceSkylight(levelPropagator, BlockPos.add(blockPos, ox + t * dx, dy, oz + t * dz), dir);
                }
            }
        }

        this.initSkylightChunks.clear();
    }

    private int fillSkylightColumn(final ChunkLightProvider<Data, ?> lightProvider, final long chunkPos)
    {
        int y = 16;

        for (; this.isAboveMinHeight(y); --y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.getX(chunkPos), y, ChunkSectionPos.getZ(chunkPos));

            if (this.nonEmptySections.contains(sectionPos))
                break;

            this.removeChunkData(lightProvider, sectionPos);

            if (this.hasLight(sectionPos))
            {
                if (this.field_15802.add(sectionPos))
                    this.lightArrays.replaceWithCopy(sectionPos);

                Arrays.fill(this.getLightArray(sectionPos, true).asByteArray(), (byte) -1);
            }
        }

        return y;
    }

    @Shadow
    private volatile boolean hasSkyLightUpdates;

    @Redirect(
        method = "checkForUpdates()V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/chunk/light/SkyLightStorage;hasSkyLightUpdates:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void checkInitSkylight(final SkyLightStorage lightStorage, final boolean hasSkyLightUpdates)
    {
        this.hasSkyLightUpdates = hasSkyLightUpdates || !this.initSkylightChunks.isEmpty();
    }
}
