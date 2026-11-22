package net.deepacat.deepamonu.mixin;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.events.LocalShieldHitEvent;
import net.deepacat.deepamonu.utils.Util;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin {
    @Unique

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void fireShieldBlockEvent(byte status, CallbackInfo ci) {
        DMMClient.GLOBAL_SAFE_EH.runSafely(() -> {
            if (status == 29) {
                LocalShieldHitEvent.EVENT.invoker().onShieldHit(Util.c(this));
            }
        });
    }
}