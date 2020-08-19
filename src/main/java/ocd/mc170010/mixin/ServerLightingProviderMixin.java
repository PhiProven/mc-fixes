package ocd.mc170010.mixin;

import java.util.function.IntSupplier;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import ocd.mc170010.InitialLightingAccessor;

@Mixin(value = ServerLightingProvider.class, priority = 1001)
public abstract class ServerLightingProviderMixin
{
    @Dynamic(mixin = ocd.mc170012.mixin.ServerLightingProviderMixin.class)
    @Inject(
        method = "setupLightmaps(Lnet/minecraft/class_2791;)V",
        at = @At("HEAD"),
        remap = false
    )
    private void forceloadLightmap(final Chunk chunk, final CallbackInfo ci)
    {
        if (chunk.isLightOn())
            return;

        final ChunkPos pos = chunk.getPos();
        ((InitialLightingAccessor) this).forceloadLightmap(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
    }

    @Shadow
    protected abstract void enqueue(int x, int z, IntSupplier completedLevelSupplier, ServerLightingProvider.Stage stage, Runnable task);

    @Inject(
        method = "updateChunkStatus(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("TAIL")
    )
    private void unloadForcedLightmap(final ChunkPos pos, final CallbackInfo ci)
    {
        this.enqueue(pos.x, pos.z, () -> 0, ServerLightingProvider.Stage.PRE_UPDATE, Util.debugRunnable(() -> {
            ((InitialLightingAccessor) this).unloadForcedLightmap(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
        },
            () -> "unloadLightmaps " + pos
        ));
    }
}
