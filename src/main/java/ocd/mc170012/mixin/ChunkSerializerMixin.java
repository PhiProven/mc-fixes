package ocd.mc170012.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.ChunkStatus;
import ocd.mc170012.IPersistentChunk;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin
{
    @Redirect(
        method = "serialize(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/Chunk;)Lnet/minecraft/nbt/CompoundTag;",
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=Status")
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getStatus()Lnet/minecraft/world/chunk/ChunkStatus;",
            ordinal = 0
        )
    )
    private static ChunkStatus usePersistentStatus(final @Coerce IPersistentChunk chunk)
    {
        return chunk.getPersistentStatus();
    }
}
