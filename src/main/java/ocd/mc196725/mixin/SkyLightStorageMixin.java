package ocd.mc196725.mixin;

import java.util.Arrays;

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

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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
import ocd.mc196725.EmptyChunkNibbleArray;
import ocd.mc196725.IReadonly;
import ocd.mc196725.SkyLightChunkNibbleArray;
import ocd.mc196725.SkyLightStorageDataAccessor;

@Mixin(value = SkyLightStorage.class, priority = 1001)
public abstract class SkyLightStorageMixin extends LightStorageMixin
{
    @Unique
    private static final ChunkNibbleArray DIRECT_SKYLIGHT_MAP = createDirectSkyLightMap();

    @Unique
    private final Long2IntMap vanillaLightmapComplexities = new Long2IntOpenHashMap();
    @Unique
    private final LongSet removedLightmaps = new LongOpenHashSet();

    @Unique
    private static ChunkNibbleArray createDirectSkyLightMap()
    {
        final ChunkNibbleArray lightmap = new ChunkNibbleArray();
        Arrays.fill(lightmap.asByteArray(), (byte) -1);

        return lightmap;
    }

    @Override
    public boolean hasSection(final long sectionPos)
    {
        return super.hasSection(sectionPos) && this.getLightSection(sectionPos, true) != null;
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

            final ChunkNibbleArray lightmap = this.getLightmap(sectionPos);

            if (lightmap != null)
                lightmapAbove = lightmap;
        }

        // Set up a lightmap and adjust the complexity for the section below

        final long sectionPosBelow = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), minY, ChunkSectionPos.unpackZ(chunkPos));

        if (this.hasSection(sectionPosBelow))
        {
            final ChunkNibbleArray lightmapBelow = this.getLightmap(sectionPosBelow);

            if (lightmapBelow == null)
            {
                int complexity = 15 * 16 * 16;

                if (lightmapAbove != null)
                    for (int z = 0; z < 16; ++z)
                        for (int x = 0; x < 16; ++x)
                            complexity -= lightmapAbove.get(x, 0, z);

                this.getOrAddLightmap(sectionPosBelow);
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

        // Add trivial lightmaps for vanilla compatibility

        for (int y = 16; y > minY; --y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), y, ChunkSectionPos.unpackZ(chunkPos));

            if (this.nonOptimizableSections.contains(sectionPos))
            {
                this.storage.put(sectionPos, this.createTrivialVanillaLightmap(DIRECT_SKYLIGHT_MAP));
                this.dirtySections.add(sectionPos);
            }
        }

        this.storage.clearCache();

        return minY;
    }

    @Override
    protected void beforeLightChange(final long blockPos, final int oldVal, final int newVal, final ChunkNibbleArray lightmap)
    {
        final long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);

        if (ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos)) == 0)
        {
            this.vanillaLightmapComplexities.put(sectionPos, this.vanillaLightmapComplexities.get(sectionPos) + newVal - oldVal);

            final long sectionPosBelow = this.getSectionBelow(sectionPos);

            if (sectionPosBelow != Long.MAX_VALUE)
            {
                final ChunkNibbleArray lightmapBelow = this.getOrAddLightmap(sectionPosBelow);

                final int x = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos));
                final int z = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos));

                this.changeLightmapComplexity(sectionPosBelow, getComplexityChange(lightmapBelow.get(x, 15, z), oldVal, newVal));
            }
        }

        // Vanilla lightmaps need to be re-parented as they otherwise leak a reference to the old lightmap

        if (this.dirtySections.add(sectionPos))
        {
            this.storage.replaceWithCopy(sectionPos);
            this.updateVanillaLightmapsBelow(sectionPos, this.getLightSection(sectionPos, true), false);
        }
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
        final long sectionPosAbove = this.getSectionAbove(sectionPos);

        return sectionPosAbove == Long.MAX_VALUE ? null : this.getLightSection(sectionPosAbove, true);
    }

    /**
     * Returns the first section above the provided <code>sectionPos</code> that {@link #hasLightmap(long)}  has a lightmap} or {@link Long#MAX_VALUE} if none exists.
     */
    @Unique
    private long getSectionAbove(long sectionPos)
    {
        sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP);

        if (this.isAtOrAboveTopmostSection(sectionPos))
            return Long.MAX_VALUE;

        for (; !this.hasLightmap(sectionPos); sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP));

        return sectionPos;
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

        if (sectionPosBelow != Long.MAX_VALUE)
        {
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
                    this.getOrAddLightmap(sectionPosBelow);
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

        // Vanilla lightmaps need to be re-parented as they otherwise leak a reference to the old lightmap

        this.updateVanillaLightmapsOnLightmapCreation(sectionPos, newLightmap);
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
            ((SkyLightStorageDataAccessor) this.storage).updateMinHeight(ChunkSectionPos.unpackY(id));

        super.setLevel(id, level);
    }

    @Override
    protected ChunkNibbleArray createInitialVanillaLightmap(final long sectionPos)
    {
        // Attempt to restore data stripped from vanilla saves. See MC-198987

        if (!this.readySections.contains(sectionPos) && !this.readySections.contains(ChunkSectionPos.offset(sectionPos, Direction.UP)))
            return this.createTrivialVanillaLightmap(sectionPos);

        // A lightmap should have been present in this case unless it was stripped from the vanilla save or the chunk is loaded for the first time.
        // In both cases the lightmap should be initialized with zero.

        final long sectionPosAbove = this.getSectionAbove(sectionPos);
        final int complexity;

        if (sectionPosAbove == Long.MAX_VALUE)
            complexity = this.isSectionEnabled(sectionPos) ? 15 * 16 * 16 : 0;
        else
            complexity = this.vanillaLightmapComplexities.get(sectionPosAbove);

        if (complexity == 0)
            return this.createTrivialVanillaLightmap(null);

        // Need to create an actual lightmap in this case as it is non-trivial

        final ChunkNibbleArray lightmap = new ChunkNibbleArray(new byte[2048]);
        this.storage.put(sectionPos, lightmap);
        this.storage.clearCache();

        this.onLoadSection(sectionPos);
        this.setLightmapComplexity(sectionPos, complexity);

        return lightmap;
    }

    @Override
    protected ChunkNibbleArray createTrivialVanillaLightmap(final long sectionPos)
    {
       final long sectionPosAbove = this.getSectionAbove(sectionPos);

       if (sectionPosAbove == Long.MAX_VALUE)
           return this.createTrivialVanillaLightmap(this.isSectionEnabled(sectionPos) ? DIRECT_SKYLIGHT_MAP : null);

       return this.createTrivialVanillaLightmap(this.vanillaLightmapComplexities.get(sectionPosAbove) == 0 ? null : this.getLightSection(sectionPosAbove, true));
    }

    @Unique
    private ChunkNibbleArray createTrivialVanillaLightmap(final ChunkNibbleArray lightmapAbove)
    {
        return lightmapAbove == null ? new EmptyChunkNibbleArray() : new SkyLightChunkNibbleArray(lightmapAbove);
    }

    @Inject(
        method = "onLoadSection(J)V",
        at = @At("HEAD")
    )
    private void updateVanillaLightmapsOnLightmapCreation(final long sectionPos, final CallbackInfo ci)
    {
        this.updateVanillaLightmapsOnLightmapCreation(sectionPos, this.getLightSection(sectionPos, true));
    }

    @Unique
    private void updateVanillaLightmapsOnLightmapCreation(final long sectionPos, final ChunkNibbleArray lightmap)
    {
        int complexity = 0;

        for (int z = 0; z < 16; ++z)
            for (int x = 0; x < 16; ++x)
                complexity += lightmap.get(x, 0, z);

        this.vanillaLightmapComplexities.put(sectionPos, complexity);
        this.removedLightmaps.remove(sectionPos);

        // Enabling the chunk already creates all relevant vanilla lightmaps

        if (!this.enabledChunks.contains(ChunkSectionPos.withZeroY(sectionPos)))
            return;

        // Vanilla lightmaps need to be re-parented immediately as the old parent can now be modified without informing them

        this.updateVanillaLightmapsBelow(sectionPos, complexity == 0 ? null : lightmap, false);
    }

    @Inject(
        method = "onUnloadSection(J)V",
        at = @At("HEAD")
    )
    private void updateVanillaLightmapsOnLightmapRemoval(final long sectionPos, final CallbackInfo ci)
    {
        this.vanillaLightmapComplexities.remove(sectionPos);

        if (!this.enabledChunks.contains(ChunkSectionPos.withZeroY(sectionPos)))
            return;

        // Re-parenting can be deferred as the removed parent is now unmodifiable

        this.removedLightmaps.add(sectionPos);
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
    private void updateVanillaLightmaps(final CallbackInfo ci)
    {
        if (this.removedLightmaps.isEmpty())
            return;

        final LongSet removedLightmaps = new LongOpenHashSet(this.removedLightmaps);

        for (final LongIterator it = removedLightmaps.iterator(); it.hasNext(); )
        {
            final long sectionPos = it.nextLong();

            if (!this.enabledChunks.contains(ChunkSectionPos.withZeroY(sectionPos)))
                continue;

            if (!this.removedLightmaps.contains(sectionPos))
                continue;

            final long sectionPosAbove = this.getSectionAbove(sectionPos);

            if (sectionPosAbove == Long.MAX_VALUE)
                this.updateVanillaLightmapsBelow(sectionPos, this.isSectionEnabled(sectionPos) ? DIRECT_SKYLIGHT_MAP : null, true);
            else
            {
                long removedLightmapPosAbove = sectionPos;

                for (long pos = sectionPos; pos != sectionPosAbove; pos = ChunkSectionPos.offset(pos, Direction.UP))
                    if (this.removedLightmaps.remove(pos))
                        removedLightmapPosAbove = pos;

                this.updateVanillaLightmapsBelow(removedLightmapPosAbove, this.vanillaLightmapComplexities.get(sectionPosAbove) == 0 ? null : this.getLightSection(sectionPosAbove, true), false);
            }
        }

        this.removedLightmaps.clear();
    }

    @Unique
    private void updateVanillaLightmapsBelow(final long sectionPos, final ChunkNibbleArray lightmapAbove, final boolean stopOnRemovedLightmap)
    {
        for (int y = ChunkSectionPos.unpackY(sectionPos) - 1; this.isAboveMinHeight(y); --y)
        {
            final long sectionPosBelow = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(sectionPos), y, ChunkSectionPos.unpackZ(sectionPos));

            if (stopOnRemovedLightmap)
            {
                if (this.removedLightmaps.contains(sectionPosBelow))
                    break;
            }
            else
                this.removedLightmaps.remove(sectionPosBelow);

            final ChunkNibbleArray lightmapBelow = this.getLightSection(sectionPosBelow, true);

            if (lightmapBelow == null)
                continue;

            if (!((IReadonly) lightmapBelow).isReadonly())
                break;

            this.storage.put(sectionPosBelow, this.createTrivialVanillaLightmap(lightmapAbove));
            this.dirtySections.add(sectionPosBelow);
        }

        this.storage.clearCache();
    }
}
