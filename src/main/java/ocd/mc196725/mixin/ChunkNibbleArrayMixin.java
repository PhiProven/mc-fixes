package ocd.mc196725.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.chunk.ChunkNibbleArray;
import ocd.mc196725.IReadonly;

@Mixin(ChunkNibbleArray.class)
public abstract class ChunkNibbleArrayMixin implements IReadonly
{
    @Inject(
        method = "set(IIII)V",
        at = @At("HEAD")
    )
    private void cancelReadonlySet(final CallbackInfo ci)
    {
        if (this.isReadonly())
            throw new UnsupportedOperationException("Cannot modify readonly ChunkNibbleArray");
    }

    @Override
    public boolean isReadonly()
    {
        return false;
    }
}
