package ocd.mc196725.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
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
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import ocd.mc196725.ILightUpdatesHandler;
import ocd.mc196725.LightStorageAccessor;

@Mixin(LightStorage.class)
public abstract class LightStorageMixin implements LightStorageAccessor, ILightUpdatesHandler
{
    @Unique
    private final LongSet enabledChunks = new LongOpenHashSet();
    @Unique
    protected final Long2IntMap lightmapComplexities = setDefaultReturnValue(new Long2IntOpenHashMap(), -1);

    @Unique
    private final LongSet markedEnabledChunks = new LongOpenHashSet();
    @Unique
    private final LongSet markedDisabledChunks = new LongOpenHashSet();
    @Unique
    private final LongSet trivialLightmaps = new LongOpenHashSet();

    // This is put here since the relevant methods to overwrite are located in LightStorage
    @Unique
    protected LongSet nonOptimizableSections = new LongOpenHashSet();

    @Unique
    private static Long2IntMap setDefaultReturnValue(final Long2IntMap map, final int rv)
    {
        map.defaultReturnValue(rv);
        return map;
    }


    @Shadow
    protected abstract ChunkNibbleArray createSection(final long sectionPos);

    @Shadow
    @Final
    protected ChunkToNibbleArrayMap<?> storage;

    @Shadow
    @Final
    protected LongSet dirtySections;

    @Shadow
    protected abstract void onLoadSection(final long sectionPos);

    @Shadow
    protected abstract void onUnloadSection(final long sectionPos);

    @Unique
    protected ChunkNibbleArray addLightmap(final long sectionPos)
    {
        final ChunkNibbleArray lightmap = this.createSection(sectionPos);

        this.storage.put(sectionPos, lightmap);
        this.storage.clearCache();
        this.dirtySections.add(sectionPos);

        this.onLoadSection(sectionPos);
        this.setLightmapComplexity(sectionPos, 0);

        return lightmap;
    }

    @Unique
    protected ChunkNibbleArray getOrAddLightmap(final long sectionPos)
    {
        final ChunkNibbleArray lightmap = this.getLightSection(sectionPos, true);

        return lightmap == null ? this.addLightmap(sectionPos) : lightmap;
    }

    @Unique
    protected void setLightmapComplexity(final long sectionPos, final int complexity)
    {
        int oldComplexity = this.lightmapComplexities.put(sectionPos, complexity);

        if (oldComplexity == 0)
        {
            this.trivialLightmaps.remove(sectionPos);
            this.checkForLightUpdates();
        }

        if (complexity == 0)
        {
            this.trivialLightmaps.add(sectionPos);
            this.checkForLightUpdates();
        }
    }

    @Shadow
    protected volatile boolean hasLightUpdates;

    @Unique
    private void checkForLightUpdates()
    {
        this.hasLightUpdates = !this.markedEnabledChunks.isEmpty() || !this.markedDisabledChunks.isEmpty() || !this.trivialLightmaps.isEmpty() || !this.queuedSections.isEmpty();
    }

    @Unique
    protected void changeLightmapComplexity(final long sectionPos, final int amount)
    {
        int complexity = this.lightmapComplexities.get(sectionPos);

        if (complexity == 0)
        {
            this.trivialLightmaps.remove(sectionPos);
            this.checkForLightUpdates();
        }

        complexity += amount;
        this.lightmapComplexities.put(sectionPos, complexity);

        if (complexity == 0)
        {
            this.trivialLightmaps.add(sectionPos);
            this.checkForLightUpdates();
        }
    }

    @Shadow
    protected abstract ChunkNibbleArray getLightSection(final long sectionPos, final boolean cached);

    @Unique
    protected boolean hasLightmap(final long sectionPos)
    {
        return this.getLightSection(sectionPos, true) != null;
    }

    @Inject(
        method = "set(JI)V",
        at = @At("HEAD")
    )
    private void setupLightmapsForLightChange(final long blockPos, final int value, final CallbackInfo ci)
    {
        final ChunkNibbleArray lightmap = this.getOrAddLightmap(ChunkSectionPos.fromBlockPos(blockPos));

        final int x = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos));
        final int y = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos));
        final int z = ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos));

        final int oldVal = lightmap.get(x, y, z);

        this.beforeLightChange(blockPos, oldVal, value, lightmap);
        this.changeLightmapComplexity(ChunkSectionPos.fromBlockPos(blockPos), this.getLightmapComplexityChange(blockPos, oldVal, value, lightmap));
    }

    /**
     * Set up lightmaps and adjust complexities as needed for the given light change.
     * Actions are only required for other affected positions, not for the given <code>blockPos</code> directly.
     */
    @Unique
    protected void beforeLightChange(final long blockPos, final int oldVal, final int newVal, final ChunkNibbleArray lightmap)
    {
    }

    @Unique
    protected int getLightmapComplexityChange(final long blockPos, final int oldVal, final int newVal, final ChunkNibbleArray lightmap)
    {
        return 0;
    }

    /**
     * Set up lightmaps and adjust complexities as needed for the given lightmap change.
     * Actions are only required for other affected sections, not for the given <code>sectionPos</code> directly.
     */
    @Unique
    protected void beforeLightmapChange(final long sectionPos, final ChunkNibbleArray oldLightmap, final ChunkNibbleArray newLightmap)
    {
    }

    @Unique
    protected int getInitialLightmapComplexity(final long sectionPos, final ChunkNibbleArray lightmap)
    {
        return 0;
    }

    /**
     * Determines whether light updates should be propagated into the given section.
     * @author PhiPro
     * @reason Method completely changed. Allow child mixins to properly extend this.
     */
    @Overwrite
    public boolean hasSection(final long sectionPos)
    {
        return this.enabledChunks.contains(ChunkSectionPos.withZeroY(sectionPos));
    }

    @Override
    public void enableLightUpdates(final long chunkPos)
    {
        if (this.markedDisabledChunks.remove(chunkPos) || this.enabledChunks.contains(chunkPos))
            return;

        this.markedEnabledChunks.add(chunkPos);
        this.checkForLightUpdates();
    }

    @Shadow
    protected abstract void setColumnEnabled(final long columnPos, final boolean enabled);

    @Override
    public void disableLightUpdates(final long chunkPos)
    {
        if (this.markedEnabledChunks.remove(chunkPos) || !this.enabledChunks.contains(chunkPos))
        {
            for (int i = -1; i < 17; ++i)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), i, ChunkSectionPos.unpackZ(chunkPos));

                if (this.storage.removeChunk(sectionPos) != null)
                    this.dirtySections.add(sectionPos);
            }

            this.setColumnEnabled(chunkPos, false);
        }
        else
        {
            this.markedDisabledChunks.add(chunkPos);
            this.checkForLightUpdates();
        }
    }

    @Override
    @Invoker("setColumnEnabled")
    public abstract void invokeSetColumnEnabled(final long chunkPos, final boolean enabled);

    @Shadow
    protected abstract boolean hasLightUpdates();

    @Inject(
        method = "updateLight(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V",
        at = @At("HEAD")
    )
    private void updateLightmaps(final ChunkLightProvider<?, ?> lightProvider, final boolean doSkylight, final boolean skipEdgeLightPropagation, final CallbackInfo ci)
    {
        if (!this.hasLightUpdates())
            return;

        this.initializeChunks();
        this.removeChunks(lightProvider);
        this.removeTrivialLightmaps(lightProvider);
    }

    @Unique
    private void initializeChunks()
    {
        this.storage.clearCache();

        for (final LongIterator it = this.markedEnabledChunks.iterator(); it.hasNext(); )
        {
            final long chunkPos = it.nextLong();

            // First need to register all lightmaps via onLoadSection() as this data is needed for calculating the initial complexity

            for (int i = -1; i < 17; ++i)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), i, ChunkSectionPos.unpackZ(chunkPos));

                if (this.hasLightmap(sectionPos))
                    this.onLoadSection(sectionPos);
            }

            // Now the initial complexities can be computed

            for (int i = -1; i < 17; ++i)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), i, ChunkSectionPos.unpackZ(chunkPos));

                if (this.hasLightmap(sectionPos))
                    this.setLightmapComplexity(sectionPos, this.getInitialLightmapComplexity(sectionPos, this.getLightSection(sectionPos, true)));
            }

            this.enabledChunks.add(chunkPos);
        }

        this.markedEnabledChunks.clear();
    }

    @Shadow
    protected abstract void removeSection(final ChunkLightProvider<?, ?> storage, final long sectionPos);

    @Unique
    private void removeChunks(final ChunkLightProvider<?, ?> lightProvider)
    {
        for (final LongIterator it = this.markedDisabledChunks.iterator(); it.hasNext(); )
        {
            final long chunkPos = it.nextLong();

            // First need to remove all pending light updates before changing any light value

            for (int i = -1; i < 17; ++i)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), i, ChunkSectionPos.unpackZ(chunkPos));

                if (this.hasSection(sectionPos))
                    this.removeSection(lightProvider, sectionPos);
            }

            // Now the chunk can be disabled

            this.enabledChunks.remove(chunkPos);

            // Now lightmaps can be removed

            int sections = 0;

            for (int i = -1; i < 17; ++i)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), i, ChunkSectionPos.unpackZ(chunkPos));

                this.queuedSections.remove(sectionPos);

                if (this.removeLightmap(sectionPos))
                    sections |= 1 << (i + 1);
            }

            // Calling onUnloadSection() after removing all the lightmaps is slightly more efficient

            this.storage.clearCache();

            for (int i = -1; i < 17; ++i)
                if ((sections & (1 << (i + 1))) != 0)
                    this.onUnloadSection(ChunkSectionPos.asLong(ChunkSectionPos.unpackX(chunkPos), i, ChunkSectionPos.unpackZ(chunkPos)));

            this.setColumnEnabled(chunkPos, false);
        }

        this.markedDisabledChunks.clear();
    }

    /**
     * Removes the lightmap associated to the provided <code>sectionPos</code>, but does not call {@link #onUnloadSection(long)} or {@link ChunkToNibbleArrayMap#clearCache()}
     * @return Whether a lightmap was removed
     */
    @Unique
    protected boolean removeLightmap(final long sectionPos)
    {
        if (this.storage.removeChunk(sectionPos) == null)
            return false;

        this.lightmapComplexities.remove(sectionPos);
        this.trivialLightmaps.remove(sectionPos);
        this.dirtySections.add(sectionPos);

        return true;
    }

    @Unique
    private void removeTrivialLightmaps(final ChunkLightProvider<?, ?> lightProvider)
    {
        for (final LongIterator it = this.trivialLightmaps.iterator(); it.hasNext(); )
        {
            final long sectionPos = it.nextLong();

            this.storage.removeChunk(sectionPos);
            this.lightmapComplexities.remove(sectionPos);
            this.dirtySections.add(sectionPos);
        }

        this.storage.clearCache();

        // Calling onUnloadSection() after removing all the lightmaps is slightly more efficient

        for (final LongIterator it = this.trivialLightmaps.iterator(); it.hasNext(); )
            this.onUnloadSection(it.nextLong());

        // Remove pending light updates for sections that no longer support light propagations

        for (final LongIterator it = this.trivialLightmaps.iterator(); it.hasNext(); )
        {
            final long sectionPos = it.nextLong();

            if (!this.hasSection(sectionPos))
                this.removeSection(lightProvider, sectionPos);
        }

        this.trivialLightmaps.clear();
    }

    @Redirect(
        method = "updateLight(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/chunk/light/LightStorage;hasSection(J)Z",
                ordinal = 0
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ChunkToNibbleArrayMap;put(JLnet/minecraft/world/chunk/ChunkNibbleArray;)V",
            ordinal = 0
        )
    )
    private void initializeLightmap(final ChunkToNibbleArrayMap<?> lightmapArray, final long sectionPos, final ChunkNibbleArray lightmap)
    {
        final ChunkNibbleArray oldLightmap = this.getLightSection(sectionPos, true);

        this.beforeLightmapChange(sectionPos, oldLightmap, lightmap);
        this.storage.put(sectionPos, lightmap);
        this.storage.clearCache();

        if (oldLightmap == null)
            this.onLoadSection(sectionPos);

        this.setLightmapComplexity(sectionPos, this.getInitialLightmapComplexity(sectionPos, lightmap));
    }

    @Shadow
    @Final
    protected Long2ObjectMap<ChunkNibbleArray> queuedSections;

    @Redirect(
        method = {
            "method_15532(JLnet/minecraft/class_2804;)V", // 1.14 - 1.15
            "enqueueSectionData(JLnet/minecraft/world/chunk/ChunkNibbleArray;Z)V" // 1.16
        },
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/chunk/light/LightStorage;queuedSections:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",
                opcode = Opcodes.GETFIELD,
                ordinal = 0
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0,
            remap = false
        )
    )
    private Object addLightmapForDisabledChunkDirectly(final Long2ObjectMap<?> queuedSections, final long l, final Object obj, final long sectionPos, final ChunkNibbleArray lightmap)
    {
        if (this.enabledChunks.contains(ChunkSectionPos.withZeroY(sectionPos)))
            return this.queuedSections.put(sectionPos, lightmap);

        this.storage.put(sectionPos, lightmap);
        this.dirtySections.add(sectionPos);
        return null;
    }

    @Redirect(
        method = {
            "method_15532(JLnet/minecraft/class_2804;)V", // 1.14 - 1.15
            "enqueueSectionData(JLnet/minecraft/world/chunk/ChunkNibbleArray;Z)V" // 1.16
        },
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/chunk/light/LightStorage;queuedSections:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",
                opcode = Opcodes.GETFIELD,
                ordinal = 1
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;remove(J)Ljava/lang/Object;",
            ordinal = 0,
            remap = false
        )
    )
    private Object removeLightmapForDisabledChunkDirectly(final Long2ObjectMap<?> queuedSections, final long sectionPos)
    {
        if (this.enabledChunks.contains(ChunkSectionPos.withZeroY(sectionPos)))
            return this.queuedSections.remove(sectionPos);

        this.dirtySections.add(sectionPos);
        return this.storage.removeChunk(sectionPos);
    }

    // Queued lightmaps are only added to the world via updateLightmaps()
    @Redirect(
        method = "createSection(J)Lnet/minecraft/world/chunk/ChunkNibbleArray;",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/chunk/light/LightStorage;queuedSections:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",
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

    @Redirect(
        method = "getLevel(J)I",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/world/chunk/light/LightStorage;storage:Lnet/minecraft/world/chunk/ChunkToNibbleArrayMap;",
                opcode = Opcodes.GETFIELD
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/ChunkToNibbleArrayMap;containsKey(J)Z",
            ordinal = 0
        )
    )
    private boolean isNonOptimizable(final ChunkToNibbleArrayMap<?> lightmapArray, final long sectionPos)
    {
        return this.nonOptimizableSections.contains(sectionPos);
    }

    @Shadow
    protected abstract int getLevel(final long id);

    @Shadow
    @Final
    protected LongSet readySections;

    @Shadow
    @Final
    protected LongSet markedReadySections;

    @Shadow
    @Final
    protected LongSet markedNotReadySections;

    /**
     * @author PhiPro
     * @reason Move large parts of the logic to other methods
     */
    @Overwrite
    public void setLevel(long id, int level)
    {
        int oldLevel = this.getLevel(id);

        if (oldLevel != 0 && level == 0)
        {
            this.readySections.add(id);
            this.markedReadySections.remove(id);
        }

        if (oldLevel == 0 && level != 0)
        {
            this.readySections.remove(id);
            this.markedNotReadySections.remove(id);
        }

        if (oldLevel >= 2 && level < 2)
            this.nonOptimizableSections.add(id);

        if (oldLevel < 2 && level >= 2)
            this.nonOptimizableSections.remove(id);
    }
}
