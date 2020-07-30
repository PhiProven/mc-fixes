package ocd.mc196542.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import net.minecraft.world.chunk.light.SkyLightStorage;
import ocd.mc196542.LightStorageAccessor;

@Mixin(ChunkSkyLightProvider.class)
public abstract class ChunkSkyLightProviderMixin extends ChunkLightProvider<SkyLightStorage.Data, SkyLightStorage>
{
    private ChunkSkyLightProviderMixin(final ChunkProvider chunkProvider, final LightType type, final SkyLightStorage lightStorage)
    {
        super(chunkProvider, type, lightStorage);
    }

    @Inject(
        method = "getPropagatedLevel(JJI)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/SkyLightStorage;isTopmostBlock(J)Z"
        ),
        cancellable = true
    )
    private void disableSourceSkylight(final CallbackInfoReturnable<Integer> ci)
    {
        ci.setReturnValue(15);
    }

    // propagateLevel() is not called concurrently, hence we can store some variables as fields
    @Unique
    private long tmpSrcPos;

    @Redirect(
        method = "propagateLevel(JIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;add(JIII)J"
        )
    )
    private long captureSrcPos(final long srcPos, final int x, final int y, final int z)
    {
        this.tmpSrcPos = BlockPos.add(srcPos, 0, y - (x == 0 && z == 0 ? Integer.signum(y) : 0), 0);
        return BlockPos.add(srcPos, x, y, z);
    }

    @Redirect(
        method = "propagateLevel(JIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;offset(JLnet/minecraft/util/math/Direction;)J"
        )
    )
    private long captureSrcPos(final long srcPos, final Direction dir)
    {
        this.tmpSrcPos = srcPos;
        return BlockPos.offset(srcPos, dir);
    }

    @ModifyArg(
        method = "propagateLevel(JIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/ChunkSkyLightProvider;propagateLevel(JJIZ)V"
        ),
        index = 0
    )
    private long useDirectNeighborSrcPos(final long srcPos)
    {
        return this.tmpSrcPos;
    }

    // recalculateLevel() is not called concurrently, hence we can store some variables as fields
    @Unique
    private ChunkNibbleArray tmpSrcLightmap;

    @Unique
    private ChunkNibbleArray tmpNeighborLightmap;

    @Unique
    private static final ChunkNibbleArray DUMMY_LIGHTMAP_SRC = new ChunkNibbleArray();

    @Unique
    private static final ChunkNibbleArray DUMMY_LIGHTMAP_NEIGHBOR = new ChunkNibbleArray();

    @Redirect(
        method = "recalculateLevel(JJI)I",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Ljava/lang/Long;MAX_VALUE:J",
                opcode = Opcodes.GETSTATIC,
                ordinal = 0
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/ChunkSkyLightProvider;getPropagatedLevel(JJI)I",
            ordinal = 0
        )
    )
    private int excludeSourceSkylight(final ChunkSkyLightProvider lightProvider, final long srcPos, final long dstPos, final int level)
    {
        return 15;
    }

    @Redirect(
        method = "recalculateLevel(JJI)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/SkyLightStorage;getLightSection(JZ)Lnet/minecraft/world/chunk/ChunkNibbleArray;"
        )
    )
    private ChunkNibbleArray captureLightmapAndReturnNonnull(final SkyLightStorage lightStorage, final long sectionPos, final boolean cached, final long blockPos)
    {
        final ChunkNibbleArray lightmap = ((LightStorageAccessor) lightStorage).callGetLightSection(sectionPos, cached);

        if (ChunkSectionPos.fromBlockPos(blockPos) == sectionPos)
        {
            this.tmpSrcLightmap = lightmap;
            return DUMMY_LIGHTMAP_SRC;
        }
        else
        {
            this.tmpNeighborLightmap = lightmap;
            return DUMMY_LIGHTMAP_NEIGHBOR;
        }
    }

    @ModifyArg(
        method = "recalculateLevel(JJI)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/ChunkSkyLightProvider;getCurrentLevelFromSection(Lnet/minecraft/world/chunk/ChunkNibbleArray;J)I"
        ),
        index = 0
    )
    private ChunkNibbleArray lookupLightmap(final ChunkNibbleArray dummyLightmap)
    {
        return dummyLightmap == DUMMY_LIGHTMAP_SRC ? this.tmpSrcLightmap : this.tmpNeighborLightmap;
    }

    @Inject(
        method = "recalculateLevel(JJI)I",
        at = @At("RETURN")
    )
    private void cleanupTmpLightmaps(final CallbackInfoReturnable<Integer> ci)
    {
        this.tmpSrcLightmap = null;
        this.tmpNeighborLightmap = null;
    }

    @Inject(
        method = "resetLevel(J)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void resetPosOnly(final long blockPos, final CallbackInfo ci)
    {
        super.resetLevel(blockPos);
        ci.cancel();
    }
}
