package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.chunk.light.SkyLightStorage;
import ocd.mc196725.SkyLightStorageDataAccessor;

@Mixin(SkyLightStorage.Data.class)
public abstract class SkyLightStorageDataMixin implements SkyLightStorageDataAccessor
{
    @Shadow
    private int minSectionY;

    @Shadow
    @Final
    private Long2IntOpenHashMap columnToTopSection;

    @Override
    public void updateMinHeight(final int y)
    {
        if (this.minSectionY > y)
        {
            this.minSectionY = y;
            this.columnToTopSection.defaultReturnValue(this.minSectionY);
        }
    }
}
