package ocd.mc170012.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.BlockLightStorage;
import net.minecraft.world.chunk.light.BlockLightStorage.Data;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import ocd.mc170012.BlockLightStorageAccessor;

@Mixin(ChunkBlockLightProvider.class)
public abstract class ChunkBlockLightProviderMixin extends ChunkLightProvider<Data, BlockLightStorage>
{
    private ChunkBlockLightProviderMixin(final ChunkProvider chunkProvider, final LightType type, final BlockLightStorage lightStorage)
    {
        super(chunkProvider, type, lightStorage);
    }

    @Inject(
        method = "getLightSourceLuminance(J)I",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void disableBlocklightSources(final long blockPos, final CallbackInfoReturnable<Integer> ci)
    {
        if (!((BlockLightStorageAccessor) this.lightStorage).isLightEnabled(ChunkSectionPos.withZeroY(ChunkSectionPos.fromBlockPos(blockPos))))
            ci.setReturnValue(0);
    }
}
