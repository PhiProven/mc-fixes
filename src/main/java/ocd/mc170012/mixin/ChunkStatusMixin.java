package ocd.mc170012.mixin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import ocd.mc170012.ServerLightingProviderAccessor;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin
{
    @Shadow
    private static ChunkStatus register(String id, ChunkStatus previous, int taskMargin, EnumSet<Type> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.Task task, ChunkStatus.NoGenTask noGenTask)
    {
        return null;
    }

    @Shadow
    @Final
    private static EnumSet<Heightmap.Type> POST_CARVER_HEIGHTMAPS;

    @Final
    @Mutable
    private static ChunkStatus PRE_LIGHT;

    @Unique
    private static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getPreLightFuture(final ChunkStatus chunkStatus, final ServerLightingProvider lightingProvider, final Chunk chunk)
    {
        return ((ServerLightingProviderAccessor) lightingProvider).setupLightmaps(chunk).thenApply((chunk_) -> {
            if (!chunk_.getStatus().isAtLeast(chunkStatus))
                ((ProtoChunk)chunk_).setStatus(chunkStatus);

            return Either.left(chunk_);
        });
    }

    @ModifyArg(
        method = "<clinit>",
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=light")
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/chunk/ChunkStatus;ILjava/util/EnumSet;Lnet/minecraft/world/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/chunk/ChunkStatus$Task;Lnet/minecraft/world/chunk/ChunkStatus$NoGenTask;)Lnet/minecraft/world/chunk/ChunkStatus;",
            ordinal = 0
        ),
        index = 1
    )
    private static ChunkStatus injectPreLightStatus(final ChunkStatus preStatus)
    {
        PRE_LIGHT = register(
            "pre_light",
            preStatus,
            1, // We want to be in the same state as the LIGHT stage was before, since ProtoChunk does not update lightmaps...
            POST_CARVER_HEIGHTMAPS,
            ChunkStatus.ChunkType.PROTOCHUNK,
            (chunkStatus, serverWorld, chunkGenerator, structureManager, serverLightingProvider, function, list, chunk) -> getPreLightFuture(chunkStatus, serverLightingProvider, chunk),
            (chunkStatus, serverWorld, structureManager, serverLightingProvider, function, chunk) -> getPreLightFuture(chunkStatus, serverLightingProvider, chunk)
        );

        return PRE_LIGHT;
    }

    @Shadow
    @Final
    @Mutable
    private static List<ChunkStatus> DISTANCE_TO_TARGET_GENERATION_STATUS;

    @Redirect(
        method = "<clinit>",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTSTATIC,
            target = "Lnet/minecraft/world/chunk/ChunkStatus;DISTANCE_TO_TARGET_GENERATION_STATUS:Ljava/util/List;"
        )
    )
    private static void injectGenerationStage(final List<ChunkStatus> generationStages)
    {
        final List<ChunkStatus> ret = new ArrayList<>(generationStages);

        // LIGHT stage requires chunks around it in PRE_LIGHT stage
        // We want to be in the same state as the LIGHT stage was before, since ProtoChunk does not update lightmaps...
        ret.add(1, PRE_LIGHT);

        DISTANCE_TO_TARGET_GENERATION_STATUS = ImmutableList.copyOf(ret);
    }
}
