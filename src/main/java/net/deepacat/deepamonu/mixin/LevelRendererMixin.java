package net.deepacat.deepamonu.mixin;

import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.deepacat.deepamonu.utils.SafeExceptionLogger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
    @Unique
    private static final SafeExceptionLogger ExceptionHandler = new SafeExceptionLogger("PlayerGlowing");
    @Unique
    ModConfig config = DMMClient.config();

    @ModifyExpressionValue(
            method = {"renderLevel"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"
            )}
    )
    private int modifyEntityGlowingColor(int original, @Local Entity entity) {
        return ExceptionHandler.runSafely(() -> {
            var map = config.features.mobGlowColorOverrides.mobColorsDropdown.mobColorMap;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (entity.getName().getString().contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return original;
        }).orElse(original);
    }
}