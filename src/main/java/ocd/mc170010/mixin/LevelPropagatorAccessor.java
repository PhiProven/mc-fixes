package ocd.mc170010.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.chunk.light.LevelPropagator;

@Mixin(LevelPropagator.class)
public interface LevelPropagatorAccessor
{
    @Invoker("updateLevel")
    void invokeUpdateLevel(long sourceId, long id, int level, boolean decrease);

    @Invoker("getPropagatedLevel")
    int callGetPropagatedLevel(long sourceId, long targetId, int level);
}
