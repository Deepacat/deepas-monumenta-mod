package net.deepacat.deepamonu.mixin.clothconfig;

import me.shedaniel.clothconfig2.gui.entries.ColorEntry;
import me.shedaniel.clothconfig2.gui.widget.ColorDisplayWidget;
import net.deepacat.deepamonu.compat.clothconfig.DMMColorDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ColorEntry.class)
public abstract class ColorEntryMixin {
    @Shadow(remap = false)
    private boolean alpha;

    @Shadow(remap = false)
    @Final
    private ColorDisplayWidget colorDisplayWidget;

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void dmm$registerColorWidget(CallbackInfo ci) {
        List<GuiEventListener> children = (List<GuiEventListener>) ((ColorEntry) (Object) this).children();
        if (!children.contains(colorDisplayWidget)) {
            children.add(colorDisplayWidget);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dmm$syncAlphaSetting(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                      int mouseX, int mouseY, boolean isHovered, float delta, CallbackInfo ci) {
        if (colorDisplayWidget instanceof DMMColorDisplay display) {
            display.dmm$setAlphaAllowed(alpha);
        }
    }
}
