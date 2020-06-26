package ocd.mc170010.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import ocd.mc170010.InitialLightingAccessor;

@Mixin(ServerLightingProvider.class)
public abstract class ServerLightingProviderMixin
{
    @Shadow
    protected abstract void enqueue(int x, int z, ServerLightingProvider.Stage stage, Runnable task);

    @Dynamic(mixin = ocd.mc170012.mixin.ServerLightingProviderMixin.class)
    @Inject(
        method = "setupLightmaps(Lnet/minecraft/class_2791;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        remap = false
    )
    private void forceloadLightmap(final Chunk chunk, final CallbackInfoReturnable<CompletableFuture<Chunk>> ci)
    {
        if (chunk.isLightOn())
            return;

        final ChunkPos pos = chunk.getPos();

        this.enqueue(pos.x, pos.z, ServerLightingProvider.Stage.PRE_UPDATE, Util.debugRunnable(() -> {
            ((InitialLightingAccessor) this).forceloadLightmap(ChunkSectionPos.withZeroZ(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
        },
            () -> "preInitLighting " + pos
        ));
    }

    @Shadow
    protected abstract void enqueue(int x, int z, IntSupplier completedLevelSupplier, ServerLightingProvider.Stage stage, Runnable task);

    @Inject(
        method = "updateChunkStatus(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("HEAD")
    )
    private void unloadForcedLightmap(final ChunkPos pos, final CallbackInfo ci)
    {
        this.enqueue(pos.x, pos.z, () -> 0, ServerLightingProvider.Stage.PRE_UPDATE, Util.debugRunnable(() -> {
            ((InitialLightingAccessor) this).unloadForcedLightmap(ChunkSectionPos.withZeroZ(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
        },
            () -> "unloadLightmaps " + pos
        ));
    }
}
