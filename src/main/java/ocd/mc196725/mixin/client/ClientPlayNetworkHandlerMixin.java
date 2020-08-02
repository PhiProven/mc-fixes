package ocd.mc196725.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin
{
    @Redirect(
        method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/LightingProvider;setColumnEnabled(Lnet/minecraft/util/math/ChunkPos;Z)V"
        ),
        require = 0 // Only present in 1.16.1
    )
    private void cancelDisableLightUpdates(final LightingProvider lightingProvider, final ChunkPos pos, final boolean enable)
    {
    }
}
