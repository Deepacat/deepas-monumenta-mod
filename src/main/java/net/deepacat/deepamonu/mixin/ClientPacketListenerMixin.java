package net.deepacat.deepamonu.mixin;

import com.dayssky.mma.util.SafeExceptionLogger;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.events.ClientPlayerHurtEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Unique
    private static final SafeExceptionLogger dmm$EH = new SafeExceptionLogger("ClientPacketEvents");

    @Unique
    private float dmm$lastHealth = -1f;

    @Unique
    private float dmm$lastAbsorption = 0f;

    @Inject(
            method = {"handleSetHealth"},
            at = {@At("TAIL")}
    )
    private void onSetHealth(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        dmm$EH.runSafely(() -> {
            if (DMMClient.player() == null) return;

            float newHealth = packet.getHealth();
            float newAbsorption = DMMClient.player().getAbsorptionAmount();
            float newMaxHealth = DMMClient.player().getMaxHealth();
            float newMaxAbsorption = DMMClient.player().getMaxAbsorption();

            // First packet after joining
            if (dmm$lastHealth < 0) {
                dmm$lastHealth = newHealth;
                dmm$lastAbsorption = newAbsorption;
                return;
            }

            // What these values would be if only max-stat changes occurred
            float expectedHealth = Math.min(dmm$lastHealth, newMaxHealth);
            float expectedAbsorption = Math.min(dmm$lastAbsorption, newMaxAbsorption);

            // Calculated damage numbers
            float healthDamage = expectedHealth - newHealth;
            float absorptionDamage = expectedAbsorption - newAbsorption;
            float totalDamage = healthDamage + absorptionDamage;

            if (totalDamage > 0.01f) {
                ClientPlayerHurtEvent.EVENT.invoker().onHurt(totalDamage);
            }

            dmm$lastHealth = newHealth;
            dmm$lastAbsorption = newAbsorption;
        }, () -> "packet=" + packet.toString());
    }
}