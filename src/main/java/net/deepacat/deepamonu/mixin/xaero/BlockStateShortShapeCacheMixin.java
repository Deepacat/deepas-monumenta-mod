package net.deepacat.deepamonu.mixin.xaero;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.deepacat.deepamonu.DMMClient;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.cache.BlockStateShortShapeCache;

/**
 * AI backported fix
 *
 * Backport of the Xaero's World Map 1.46.0 fix for the Java 25 JVM
 * store-reordering crash that happens when leaving a world/server or
 * transferring between servers.
 *
 * <p>1.39.12 publishes {@code ioThreadWaitingFor} immediately after
 * {@code ioThreadWaitingForSupplier} with no memory barrier. Java 25 can make
 * the future visible while the supplier is still null, so
 * {@code supplyForIOThread()} throws:
 *
 * <pre>
 * NullPointerException: Cannot invoke "java.util.function.Supplier.get()"
 * because "this.ioThreadWaitingForSupplier" is null
 * </pre>
 *
 * <p>Upstream 1.46.0 fixed it by inserting {@code synchronized (this) {}}
 * between the two writes. Injecting after the supplier PUTFIELD reproduces
 * exactly that barrier on 1.39.12.
 */
@Mixin(value = BlockStateShortShapeCache.class, remap = false)
public abstract class BlockStateShortShapeCacheMixin {

    @Shadow
    private CompletableFuture<Boolean> ioThreadWaitingFor;

    @Shadow
    private Supplier<Boolean> ioThreadWaitingForSupplier;

    @Inject(
            method = "isShort",
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/cache/BlockStateShortShapeCache;ioThreadWaitingForSupplier:Ljava/util/function/Supplier;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            ),
            require = 0,
            remap = false
    )
    private void dmm$fenceSupplierPublication(CallbackInfoReturnable<Boolean> cir) {
        if (!dmm$isFixEnabled()) return;

        synchronized (this) {
            // Intentionally empty: monitorenter/monitorexit is the memory
            // barrier that stops the JVM reordering the two field writes.
        }
    }

    /**
     * Safety net. If the race still slips through, complete the pending future
     * with "not short" instead of crashing the game or leaving the map IO
     * thread blocked forever in {@code CompletableFuture.join()}.
     */
    @Inject(
            method = "supplyForIOThread",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void dmm$guardNullSupplier(CallbackInfo ci) {
        if (!dmm$isFixEnabled()) return;

        CompletableFuture<Boolean> future = this.ioThreadWaitingFor;
        if (future != null && !future.isDone() && this.ioThreadWaitingForSupplier == null) {
            future.complete(Boolean.FALSE);
            ci.cancel();
        }
    }

    @Unique
    private static boolean dmm$isFixEnabled() {
        return DMMClient.CONFIG != null
                && DMMClient.config().modtweaks.xaeromap.patches.enableWorldMapTransferCrashFix;
    }
}
