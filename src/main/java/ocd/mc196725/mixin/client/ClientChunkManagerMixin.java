package ocd.mc196725.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import ocd.mc196725.ILightUpdatesHandler;

@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin
{
    @Shadow
    public abstract LightingProvider getLightingProvider();

    @Unique
    private void enableLightUpdates(final int x, final int z, final boolean complete)
    {
        if (complete)
            ((ILightUpdatesHandler) this.getLightingProvider()).enableLightUpdates(ChunkSectionPos.withZeroY(ChunkSectionPos.asLong(x, 0, z)));
    }

    @Inject(
        method = {
            "method_16020(Lnet/minecraft/class_1937;IILnet/minecraft/class_2540;Lnet/minecraft/class_2487;IZ)Lnet/minecraft/class_2818;", // 1.14
            "loadChunkFromPacket(IILnet/minecraft/world/biome/source/BiomeArray;Lnet/minecraft/network/PacketByteBuf;Lnet/minecraft/nbt/CompoundTag;IZ)Lnet/minecraft/world/chunk/WorldChunk;" // 1.15 - 1.16
        },
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/chunk/light/LightingProvider;setColumnEnabled(Lnet/minecraft/util/math/ChunkPos;Z)V"
            )
        ),
        at = @At(
            value = "RETURN",
            ordinal = 0
        )
    )
    private void enableLightUpdates(final int x, final int z, final @Coerce Object biomes, final PacketByteBuf buf, final CompoundTag tag, final int verticalStripBitmask, final boolean complete, final CallbackInfoReturnable<WorldChunk> ci)
    {
        this.enableLightUpdates(x, z, complete);
    }

    @Surrogate
    private void enableLightUpdates(final World world, final int x, final int z, final PacketByteBuf buf, final CompoundTag tag, final int verticalStripBitmask, final boolean complete, final CallbackInfoReturnable<WorldChunk> ci)
    {
        this.enableLightUpdates(x, z, complete);
    }
}
