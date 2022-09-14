package net.caffeinemc.mixin;

import java.time.Duration;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.profiler.ProfilerSystem;

@Mixin(ProfilerSystem.class)
class MixinProfilerSystem {
    private MixinProfilerSystem() {}

    private static final long[] THRESHOLDS = {Duration.ofMillis(100L).toNanos(), Duration.ofMillis(50L).toNanos(), Duration.ofMillis(20L).toNanos(), Duration.ofMillis(15L).toNanos()};
    private int depth;

    @Inject(
        method = "startTick",
        at = @At(
            value = "TAIL"
        )
    )
    private void initDepth(final CallbackInfo ci) {
        this.depth = 0;
    }

    @Inject(
        method = "push(Ljava/lang/String;)V",
        at = @At(
            value = "TAIL"
        )
    )
    private void pushDepth(final CallbackInfo ci) {
        ++this.depth;
    }

    @Inject(
        method = "pop",
        at = @At(
            value = "TAIL"
        )
    )
    private void popDepth(final CallbackInfo ci) {
        --this.depth;
    }

    @Redirect(
        method = "pop",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/util/profiler/ProfilerSystem;TIMEOUT_NANOSECONDS:J"
        )
    )
    private long getThreshold() {
        return THRESHOLDS[Math.min(this.depth, THRESHOLDS.length - 1)];
    }
}
