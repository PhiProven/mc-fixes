package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(value = ServerLightingProvider.class, priority = 1001)
public abstract class ServerLightingProviderMixin extends LightingProviderMixin
{
    @Dynamic(mixin = ocd.mc170012.mixin.ServerLightingProviderMixin.class)
    @Redirect(
        method = {
            "setupLightmaps(Lnet/minecraft/class_2791;)V",
            "processInitialLighting(Lnet/minecraft/class_2791;Z)V"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/LightingProvider;setColumnEnabled(Lnet/minecraft/util/math/ChunkPos;Z)V"
        ),
        require = 2
    )
    private void enableSourceLight(final LightingProvider lightingProvider, final ChunkPos pos, final boolean enabled)
    {
        super.enableSourceLight(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
    }

    @Dynamic(mixin = ocd.mc170012.mixin.ServerLightingProviderMixin.class)
    @Inject(
        method = "setupLightmaps(Lnet/minecraft/class_2791;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void enableLightUpdates(final Chunk chunk, final CallbackInfo ci)
    {
        final ChunkPos pos = chunk.getPos();
        super.enableLightUpdates(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
    }
}
