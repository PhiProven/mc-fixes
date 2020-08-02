package ocd.mc196725.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import ocd.mc196725.ILightUpdatesHandler;

@Mixin(value = ServerLightingProvider.class, priority = 1001)
public abstract class ServerLightingProviderMixin
{
    @Shadow
    protected abstract void enqueue(int x, int z, ServerLightingProvider.Stage stage, Runnable task);

    @Dynamic(mixin = ocd.mc170012.mixin.ServerLightingProviderMixin.class)
    @Inject(
        method = "setupLightmaps(Lnet/minecraft/class_2791;)Ljava/util/concurrent/CompletableFuture;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerLightingProvider;enqueue(IILnet/minecraft/server/world/ServerLightingProvider$Stage;Ljava/lang/Runnable;)V",
            ordinal = 0,
            shift = Shift.AFTER
        )
    )
    private void enableLightUpdates(final Chunk chunk, final CallbackInfoReturnable<CompletableFuture<Chunk>> ci)
    {
        final ChunkPos pos = chunk.getPos();

        this.enqueue(pos.x, pos.z, ServerLightingProvider.Stage.PRE_UPDATE, Util.debugRunnable(() -> {
                ((ILightUpdatesHandler) this).enableLightUpdates(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
        },
            () -> "enableLightUpdates " + pos
        ));
    }
}
