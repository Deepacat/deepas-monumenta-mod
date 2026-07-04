package net.deepacat.deepamonu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.tslat.tes.core.state.EntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityState.class)
public class TESStateMixin {

    @Inject(
            method = "handleHealthChange",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/tslat/tes/core/particle/TESParticleManager;addParticle(Lnet/tslat/tes/api/TESParticle;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            remap = false
    )
    private void applyThresholds(CallbackInfo ci, @Local(ordinal = 0) float healthDelta) {
        ModConfig config = DMMClient.config();

        float damageThreshold = config.modtweaks.tslatentitystatus.particles.damageThreshold;
        float healThreshold = config.modtweaks.tslatentitystatus.particles.healThreshold;
        boolean enabled = config.modtweaks.tslatentitystatus.particles.enableThresholds;

        if (enabled) {
            if (healthDelta < 0) {
                if (-healthDelta < damageThreshold) {
                    ci.cancel();
                }
            } else if (healthDelta > 0) {
                if (healthDelta < healThreshold) {
                    ci.cancel();
                }
            }
        }
    }
}