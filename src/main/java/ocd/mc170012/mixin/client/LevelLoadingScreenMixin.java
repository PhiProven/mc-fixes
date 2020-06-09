package ocd.mc170012.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.world.chunk.ChunkStatus;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin
{
    @Shadow
    @Final
    private static Object2IntMap<ChunkStatus> STATUS_TO_COLOR;

    static {
        final ChunkStatus PRE_LIGHT = ChunkStatus.LIGHT.getPrevious();
        STATUS_TO_COLOR.put(PRE_LIGHT, STATUS_TO_COLOR.getInt(PRE_LIGHT.getPrevious()));
    }
}
