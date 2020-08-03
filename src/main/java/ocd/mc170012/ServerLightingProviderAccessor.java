package ocd.mc170012;

import java.util.concurrent.CompletableFuture;

import net.minecraft.world.chunk.Chunk;

public interface ServerLightingProviderAccessor
{
    CompletableFuture<Chunk> enqueueSetupLightmaps(Chunk chunk);
}
