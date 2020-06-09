package ocd.mc170012.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin
{
    @Shadow
    public abstract ChunkSection getSection(final int y);

    // setBlockState() is not called concurrently, hence we can store some variables as fields
    @Unique
    private ChunkSection tmpSection;

    @Redirect(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ProtoChunk;getSection(I)Lnet/minecraft/world/chunk/ChunkSection;"
        )
    )
    private ChunkSection captureSection(final ProtoChunk chunk, final int y)
    {
        return this.tmpSection = this.getSection(y);
    }

    @Shadow
    public abstract LightingProvider getLightingProvider();

    @Shadow
    public abstract ChunkStatus getStatus();

    @Unique
    private static final ChunkStatus PRE_LIGHT = ChunkStatus.LIGHT.getPrevious();


    @Inject(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ChunkSection;setBlockState(IIILnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
        )
    )
    private void addLightmap(final BlockPos pos, final BlockState state, final boolean moved, final CallbackInfoReturnable<BlockState> ci)
    {
        if (this.getStatus().isAtLeast(PRE_LIGHT) && ChunkSection.isEmpty(this.tmpSection))
            this.getLightingProvider().updateSectionStatus(pos, false);
    }

    @Inject(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
        at = @At(
            value = "RETURN"
        )
    )
    private void removeLightmap(final BlockPos pos, final BlockState state, final boolean moved, final CallbackInfoReturnable<BlockState> ci)
    {
        if (this.tmpSection == null)
            return;

        if (this.getStatus().isAtLeast(PRE_LIGHT) && ChunkSection.isEmpty(this.tmpSection))
            this.getLightingProvider().updateSectionStatus(pos, true);

        this.tmpSection = null;
    }

    @Redirect(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/world/chunk/ChunkStatus;FEATURES:Lnet/minecraft/world/chunk/ChunkStatus;"
        )
    )
    private ChunkStatus usePreLightStatus()
    {
        return PRE_LIGHT;
    }
}
