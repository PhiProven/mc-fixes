package ocd.mc196725.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.SkyLightStorage;
import net.minecraft.world.chunk.light.SkyLightStorage.Data;
import ocd.mc196725.SkyLightStorageDataAccessor;

@Mixin(value = SkyLightStorage.class, priority = 1001)
public abstract class SkyLightStorageMixin extends LightStorageMixin
{
    @Unique
    private final LongSet markedOptimizableSections = new LongOpenHashSet();

    @Override
    public boolean hasSection(final long sectionPos)
    {
        return super.hasSection(sectionPos) && (this.hasLightmap(sectionPos) || this.nonOptimizableSections.contains(sectionPos) || this.markedOptimizableSections.contains(sectionPos));
    }

    // Queued lightmaps are only added to the world via updateLightmaps()
    @Redirect(
        method = "createSection(J)Lnet/minecraft/world/chunk/ChunkNibbleArray;",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/chunk/light/SkyLightStorage;queuedSections:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",
                opcode = Opcodes.GETFIELD
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            ordinal = 0,
            remap = false
        )
    )
    private Object cancelLightmapLookupFromQueue(final Long2ObjectMap<ChunkNibbleArray> lightmapArray, final long pos)
    {
        return null;
    }

    @Unique
    private static int getComplexityChange(final int val, final int oldNeighborVal, final int newNeighborVal)
    {
        return Math.abs(newNeighborVal - val) - Math.abs(oldNeighborVal - val);
    }

    /**
     * @author PhiPro
     * @reason Needs to be completely restructured
     */
    @Dynamic(mixin = ocd.mc170010.mixin.SkyLightStorageMixin.class)
    @Overwrite(remap = false)
    private int fillSkylightColumn(final ChunkLightProvider<Data, ?> lightProvider, final long chunkPos)
    {
        int minY = 16;
        ChunkNibbleArray lightmapAbove = null;

        // First need to remove all pending light updates before changing any light value

        for (; this.isAboveMinHeight(minY); --minY)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), minY, ChunkSectionPos.unpackZ(chunkPos));

            if (this.readySections.contains(sectionPos))
                break;

            if (this.hasSection(sectionPos))
                this.removeSection(lightProvider, sectionPos);

            final ChunkNibbleArray lightmap = this.getLightSection(sectionPos, true);

            if (lightmap != null)
                lightmapAbove = lightmap;
        }

        // Set up a lightmap and adjust the complexity for the section below

        final long sectionPosBelow = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), minY, ChunkSectionPos.unpackZ(chunkPos));

        if (this.hasSection(sectionPosBelow))
        {
            final ChunkNibbleArray lightmapBelow = this.getLightSection(sectionPosBelow, true);

            if (lightmapBelow == null)
            {
                int complexity = 15 * 16 * 16;

                if (lightmapAbove != null)
                    for (int z = 0; z < 16; ++z)
                        for (int x = 0; x < 16; ++x)
                            complexity -= lightmapAbove.get(x, 0, z);

                this.addLightmap(sectionPosBelow);
                this.setLightmapComplexity(sectionPosBelow, complexity);
            }
            else
            {
                int amount = 0;

                for (int z = 0; z < 16; ++z)
                    for (int x = 0; x < 16; ++x)
                        amount += getComplexityChange(lightmapBelow.get(x, 15, z), lightmapAbove == null ? 0 : lightmapAbove.get(x, 0, z), 15);

                this.changeLightmapComplexity(sectionPosBelow, amount);
            }
        }

        // Now light values can be changed
        // Delete lightmaps so the sections inherit direct skylight

        int sections = 0;

        for (int y = 16; y > minY; --y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), y, ChunkSectionPos.unpackZ(chunkPos));

            if (this.removeLightmap(sectionPos))
                sections |= 1 << (y + 1);
        }

        // Calling onUnloadSection() after removing all the lightmaps is slightly more efficient

        this.storage.clearCache();

        for (int y = 16; y > minY; --y)
            if ((sections & (1 << (y + 1))) != 0)
                this.onUnloadSection(ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), y, ChunkSectionPos.unpackZ(chunkPos)));

        return minY;
    }

    @Override
    protected void beforeLightChange(final long blockPos, final int oldVal, final int newVal, final ChunkNibbleArray lightmap)
    {
        if (ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos)) != 0)
            return;

        final long sectionPosBelow = this.getSectionBelow(ChunkSectionPos.fromBlockPos(blockPos));

        if (sectionPosBelow == Long.MAX_VALUE)
            return;

        final ChunkNibbleArray lightmapBelow = this.getOrAddLightmap(sectionPosBelow);

        final int x = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos));
        final int z = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos));

        this.changeLightmapComplexity(sectionPosBelow, getComplexityChange(lightmapBelow.get(x, 15, z), oldVal, newVal));
    }

    @Shadow
    protected abstract boolean isAboveMinHeight(final int sectionY);

    /**
     * Returns the first section below the provided <code>sectionPos</code> that {@link #hasSection(long) supports light propagations} or {@link Long#MAX_VALUE} if no such section exists.
     */
    @Unique
    private long getSectionBelow(long sectionPos)
    {
        for (int y = ChunkSectionPos.unpackY(sectionPos); this.isAboveMinHeight(y); --y)
            if (this.hasSection(sectionPos = ChunkSectionPos.offset(sectionPos, Direction.DOWN)))
                return sectionPos;

        return Long.MAX_VALUE;
    }

    @Override
    protected int getLightmapComplexityChange(final long blockPos, final int oldVal, final int newVal, final ChunkNibbleArray lightmap)
    {
        final long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);
        final int x = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos));
        final int y = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos));
        final int z = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos));

        final int valAbove;

        if (y < 15)
            valAbove = lightmap.get(x, y + 1, z);
        else
        {
            final ChunkNibbleArray lightmapAbove = this.getLightmapAbove(sectionPos);
            valAbove = lightmapAbove == null ? this.getDirectSkylight(sectionPos) : lightmapAbove.get(x, 0, z);
        }

        int amount = getComplexityChange(valAbove, oldVal, newVal);

        if (y > 0)
            amount += getComplexityChange(lightmap.get(x, y - 1, z), oldVal, newVal);

        return amount;
    }

    @Shadow
    protected abstract boolean isAtOrAboveTopmostSection(final long sectionPos);

    /**
     * Returns the first lightmap above the provided <code>sectionPos</code> or <code>null</code> if none exists.
     */
    @Unique
    private ChunkNibbleArray getLightmapAbove(long sectionPos)
    {
        sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP);

        if (this.isAtOrAboveTopmostSection(sectionPos))
            return null;

        while (true)
        {
            final ChunkNibbleArray lightmap = this.getLightSection(sectionPos, true);

            if (lightmap != null)
                return lightmap;

            sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP);
        }
    }

    @Shadow
    protected abstract boolean isSectionEnabled(final long sectionPos);

    @Unique
    private int getDirectSkylight(final long sectionPos)
    {
        return this.isSectionEnabled(sectionPos) ? 15 : 0;
    }

    @Override
    protected void beforeLightmapChange(final long sectionPos, final ChunkNibbleArray oldLightmap, final ChunkNibbleArray newLightmap)
    {
        final long sectionPosBelow = this.getSectionBelow(sectionPos);

        if (sectionPosBelow == Long.MAX_VALUE)
            return;

        final ChunkNibbleArray lightmapBelow = this.getLightSection(sectionPosBelow, true);
        final ChunkNibbleArray lightmapAbove = oldLightmap == null ? this.getLightmapAbove(sectionPos) : oldLightmap;

        final int skyLight = this.getDirectSkylight(sectionPos);

        if (lightmapBelow == null)
        {
            int complexity = 0;

            for (int z = 0; z < 16; ++z)
                for (int x = 0; x < 16; ++x)
                    complexity += Math.abs(newLightmap.get(x, 0, z) - (lightmapAbove == null ? skyLight : lightmapAbove.get(x, 0, z)));

            if (complexity != 0)
            {
                this.addLightmap(sectionPosBelow);
                this.setLightmapComplexity(sectionPosBelow, complexity);
            }
        }
        else
        {
            int amount = 0;

            for (int z = 0; z < 16; ++z)
                for (int x = 0; x < 16; ++x)
                    amount += getComplexityChange(lightmapBelow.get(x, 15, z), lightmapAbove == null ? skyLight : lightmapAbove.get(x, 0, z), newLightmap.get(x, 0, z));

            this.changeLightmapComplexity(sectionPosBelow, amount);
        }
    }

    @Override
    protected int getInitialLightmapComplexity(final long sectionPos, final ChunkNibbleArray lightmap)
    {
        int complexity = 0;

        for (int y = 0; y < 15; ++y)
            for (int z = 0; z < 16; ++z)
                for (int x = 0; x < 16; ++x)
                    complexity += Math.abs(lightmap.get(x, y + 1, z) - lightmap.get(x, y, z));

        final ChunkNibbleArray lightmapAbove = this.getLightmapAbove(sectionPos);
        final int skyLight = this.getDirectSkylight(sectionPos);

        for (int z = 0; z < 16; ++z)
            for (int x = 0; x < 16; ++x)
                complexity += Math.abs((lightmapAbove == null ? skyLight : lightmapAbove.get(x, 0, z)) - lightmap.get(x, 15, z));

        return complexity;
    }

    @Redirect(
        method = "onUnloadSection(J)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/SkyLightStorage;hasSection(J)Z"
        )
    )
    private boolean hasActualLightmap(final SkyLightStorage lightStorage, long sectionPos)
    {
        return this.hasLightmap(sectionPos);
    }

    @Override
    public void setLevel(final long id, final int level)
    {
        final int oldLevel = this.getLevel(id);

        if (oldLevel >= 2 && level < 2)
        {
            ((SkyLightStorageDataAccessor) this.storage).updateMinHeight(ChunkSectionPos.unpackY(id));
            this.markedOptimizableSections.remove(id);
        }

        if (oldLevel < 2 && level >= 2)
            this.markedOptimizableSections.add(id);

        super.setLevel(id, level);
    }

    @Inject(
        method = "updateLight(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V",
        at = @At(
            value = "INVOKE",
            opcode = Opcodes.INVOKESPECIAL,
            shift = Shift.AFTER,
            target = "Lnet/minecraft/world/chunk/light/LightStorage;updateLight(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V"
        )
    )
    private void makeSectionsSkylightOptimizable(final ChunkLightProvider<Data, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation, final CallbackInfo ci)
    {
        for (final LongIterator it = this.markedOptimizableSections.iterator(); it.hasNext(); )
        {
            final long sectionPos = it.nextLong();

            it.remove();

            // Remove pending light updates for sections that no longer support light propagations

            if (!this.hasSection(sectionPos))
                this.removeSection(lightProvider, sectionPos);
        }
    }
}
