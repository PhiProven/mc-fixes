package ocd.mc196542.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.SkyLightStorage;

@Mixin(SkyLightStorage.class)
public abstract class SkyLightStorageMixin extends LightStorageMixin
{
    @Shadow
    protected abstract boolean isAtOrAboveTopmostSection(final long sectionPos);

    @Shadow
    protected abstract boolean isSectionEnabled(final long sectionPos);

    @Override
    protected int getLightWithoutLightmap(final long blockPos)
    {
        long sectionPos = ChunkSectionPos.offset(ChunkSectionPos.fromBlockPos(blockPos), Direction.UP);

        if (this.isAtOrAboveTopmostSection(sectionPos))
            return this.isSectionEnabled(sectionPos) ? 15 : 0;

        ChunkNibbleArray lightmap;

        for (; (lightmap = this.getLightSection(sectionPos, true)) == null; sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP));

        return lightmap.get(ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos)), 0, ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos)));
    }
}
