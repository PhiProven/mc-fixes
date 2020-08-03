package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import ocd.mc196725.ILightUpdatesHandler;

@Mixin(value = ServerLightingProvider.class, priority = 1001)
public abstract class ServerLightingProviderMixin
{
    @Dynamic(mixin = ocd.mc170012.mixin.ServerLightingProviderMixin.class)
    @Inject(
        method = "setupLightmaps(Lnet/minecraft/class_2791;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void enableLightUpdates(final Chunk chunk, final CallbackInfo ci)
    {
        final ChunkPos pos = chunk.getPos();
        ((ILightUpdatesHandler) this).enableLightUpdates(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
    }
}
