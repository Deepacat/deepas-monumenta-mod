package net.deepacat.deepamonu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.deepacat.deepamonu.config.ModConfig;
import net.tslat.tes.core.state.EntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityState.class)
public class EntityStatusStateMixin {

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
        float damageThreshold = ModConfig.ModTweaks.TSlatEntityStatus.Particles.damageThreshold;
        float healThreshold = ModConfig.ModTweaks.TSlatEntityStatus.Particles.healThreshold;

        if (ModConfig.ModTweaks.TSlatEntityStatus.Particles.enableThresholds) {
            if (healthDelta < 0) {          // damage
                if (-healthDelta < damageThreshold) {
                    ci.cancel();            // skip the addParticle call
                }
            } else if (healthDelta > 0) {   // healing
                if (healthDelta < healThreshold) {
                    ci.cancel();
                }
            }
        }
    }
}
