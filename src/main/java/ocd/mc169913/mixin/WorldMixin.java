package ocd.mc169913.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    private BlockPos tmpPos;
    @Unique
    private WorldChunk tmpChunk;

    @Redirect(
        method = {
            "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", // 1.14 - 1.15
            "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z" // 1.16
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getWorldChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/WorldChunk;"
        )
    )
    private WorldChunk captureChunk(final World world, final BlockPos pos)
    {
        this.tmpPos = pos;
        return this.tmpChunk = this.getWorldChunk(pos);
    }

    @Inject(
        method = {
            "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", // 1.14 - 1.15
            "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z" // 1.16
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/WorldChunk;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"
        )
    )
    private void addLightmap(final CallbackInfoReturnable<Boolean> ci)
    {
        if (ChunkSection.isEmpty(this.tmpChunk.getSectionArray()[this.tmpPos.getY() >> 4]))
            this.getChunkManager().getLightingProvider().setSectionStatus(this.tmpPos, false);
    }

    @Inject(
        method = {
            "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", // 1.14 - 1.15
            "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z" // 1.16
        },
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/chunk/WorldChunk;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"
            )
        ),
        at = @At("RETURN")
    )
    private void removeLightmap(final CallbackInfoReturnable<Boolean> ci)
    {
        if (this.tmpChunk == null)
            return;

        if (ChunkSection.isEmpty(this.tmpChunk.getSectionArray()[this.tmpPos.getY() >> 4]))
            this.getChunkManager().getLightingProvider().setSectionStatus(this.tmpPos, true);

        this.tmpChunk = null;
        this.tmpPos = null;
    }
}
