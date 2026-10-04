package net.deepacat.deepamonu.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    public Camera camera;

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private <E extends BlockEntity> void renderMixin(E blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        ModConfig config = DMMClient.config();
        ModConfig.Features.BlockEntityHider hider = config.features.blockEntityHider;

        if (!shouldHide(hider, blockEntity)) return;

        int distSq = distSquared(blockEntity.getBlockPos(), this.camera.getBlockPosition());
        if (distSq > hider.distance * hider.distance) {
            ci.cancel();
        }
    }

    @Unique
    private boolean shouldHide(ModConfig.Features.BlockEntityHider hider, BlockEntity blockEntity) {
        // Hide-in-all-dimensions overrides the dimension list.
        boolean inDimension;
        if (hider.hideInAllDimensions) {
            inDimension = true;
        } else if (hider.hideInDimensionList) {
            var level = Minecraft.getInstance().level;
            if (level == null) return false;
            inDimension = matchesAny(hider.dimensionIds, level.dimension().location().toString());
        } else {
            inDimension = false;
        }
        if (!inDimension) return false;

        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (id == null) return false;
        if (hider.hideAllBlocks) return true;
        return matchesAny(hider.blockEntityIds, id.toString());
    }

    @Unique
    private static boolean matchesAny(List<String> patterns, String value) {
        for (String pattern : patterns) {
            if (matches(pattern, value)) return true;
        }
        return false;
    }

    @Unique
    private static boolean matches(String pattern, String value) {
        // /regex/ syntax
        if (pattern.length() >= 2 && pattern.startsWith("/") && pattern.endsWith("/")) {
            try {
                return value.matches(pattern.substring(1, pattern.length() - 1));
            } catch (Exception e) {
                return false;
            }
        }
        return pattern.equals(value);
    }

    @Unique
    private int distSquared(BlockPos b1, BlockPos b2) {
        // block positions are integer only but the usual distSqr still calculates distance between two by converting to doubles first
        int x = b1.getX() - b2.getX();
        int y = b1.getY() - b2.getY();
        int z = b1.getZ() - b2.getZ();
        return x * x + y * y + z * z;
    }
}