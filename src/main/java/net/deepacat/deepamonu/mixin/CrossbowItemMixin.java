package net.deepacat.deepamonu.mixin;

import net.deepacat.deepamonu.hud.MultiloadDisplay;
import net.deepacat.deepamonu.utils.SafeExceptionLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Unique
    private static final SafeExceptionLogger dmm$EH = new SafeExceptionLogger("MultiloadCrossbowHook");

    @Unique
    private static final ThreadLocal<Boolean> dmm$wasCharged = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<ListTag> dmm$chargedProjectiles = new ThreadLocal<>();

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void dmm$captureCharged(Level level, Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        dmm$wasCharged.set(false);
        dmm$chargedProjectiles.remove();

        if (!level.isClientSide() || player != Minecraft.getInstance().player) return;
        if (hand != InteractionHand.MAIN_HAND) return;

        ItemStack stack = player.getItemInHand(hand);

        // After a predicted shot, the crossbow is intentionally kept charged so
        // it doesn't reload while waiting for the server. Ignore the continuous
        // "use" while the button stays held so this doesn't turn into auto-fire.
        if (MultiloadDisplay.shouldSuppressRepeatUse()) {
            cir.setReturnValue(InteractionResultHolder.consume(stack));
            return;
        }

        if (!CrossbowItem.isCharged(stack)) return;

        dmm$wasCharged.set(true);
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            ListTag projectiles = tag.getList("ChargedProjectiles", Tag.TAG_COMPOUND);
            // onCrossbowShot clears this list during the shot, so keep a copy.
            if (!projectiles.isEmpty()) {
                dmm$chargedProjectiles.set(projectiles.copy());
            }
        }
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void dmm$afterUse(Level level, Player player, InteractionHand hand,
                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ListTag chargedProjectiles = dmm$chargedProjectiles.get();
        boolean wasCharged = dmm$wasCharged.get();
        dmm$wasCharged.remove();
        dmm$chargedProjectiles.remove();

        if (!wasCharged || chargedProjectiles == null) return;
        if (!level.isClientSide() || player != Minecraft.getInstance().player) return;
        if (hand != InteractionHand.MAIN_HAND) return;

        ItemStack stack = player.getItemInHand(hand);
        // The vanilla use() path sets Charged=false only after an actual shot.
        if (CrossbowItem.isCharged(stack)) return;

        dmm$EH.runSafely(() -> MultiloadDisplay.onLocalShot(stack, chargedProjectiles));
    }
}
