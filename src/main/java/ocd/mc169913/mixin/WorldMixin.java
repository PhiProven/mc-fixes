package ocd.mc169913.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess
{
    @Shadow
    public abstract WorldChunk getWorldChunk(final BlockPos blockPos);

    // setBlockState() is not called concurrently, hence we can store some variables as fields
    @Unique
    private WorldChunk tmpChunk;

    @Redirect(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getWorldChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/WorldChunk;"
        )
    )
    private WorldChunk captureChunk(final World world, final BlockPos pos)
    {
        return this.tmpChunk = this.getWorldChunk(pos);
    }

    @Inject(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/WorldChunk;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"
        )
    )
    private void addLightmap(final BlockPos pos, final BlockState state, final int flags, final CallbackInfoReturnable<Boolean> cir)
    {
        if (ChunkSection.isEmpty(this.tmpChunk.getSectionArray()[pos.getY() >> 4]))
            this.getChunkManager().getLightingProvider().updateSectionStatus(pos, false);
    }

    @Inject(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
        at = @At(
            value = "RETURN"
        )
    )
    private void removeLightmap(final BlockPos pos, final BlockState state, final int flags, final CallbackInfoReturnable<Boolean> cir)
    {
        if (this.tmpChunk == null)
            return;

        if (ChunkSection.isEmpty(this.tmpChunk.getSectionArray()[pos.getY() >> 4]))
            this.getChunkManager().getLightingProvider().updateSectionStatus(pos, true);

        this.tmpChunk = null;
    }
}
