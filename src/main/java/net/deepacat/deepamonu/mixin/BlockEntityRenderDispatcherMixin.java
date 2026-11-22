package net.deepacat.deepamonu.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import net.deepacat.deepamonu.DMMClient;
import net.deepacat.deepamonu.ModConfig;
import net.deepacat.deepamonu.features.BlockEntityHiderCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private <E extends BlockEntity> void renderMixin(E blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        ModConfig config = DMMClient.config();
        ModConfig.Features.BlockEntityHider hider = config.features.blockEntityHider;

        // Compiles patterns only when the configured lists changed; otherwise a cheap compare.
        BlockEntityHiderCache.setDebug(DMMClient.features().enableDebug);
        BlockEntityHiderCache.sync(hider.blockEntityIds, hider.dimensionIds);

        // Cheap state compare; logs whenever a switch changes so silent no-ops are visible.
        BlockEntityHiderCache.logGateState(hider.enabled, hider.hideInAllDimensions,
                hider.hideInDimensionList, hider.hideAllBlocks, hider.distance);

        if (!hider.enabled) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Block entities farther than the configured distance are hidden; nearby
        // ones stay visible. This test is a handful of integer ops, so do it before
        // touching any of the (cached) pattern matching.
        int distSq = distSquared(blockEntity.getBlockPos(), player);
        if (distSq <= hider.distance * hider.distance) return;

        if (!shouldHide(hider, blockEntity)) return;

        BlockEntityHiderCache.logHide(blockEntity.getType(), blockEntity.getBlockPos(), distSq);
        ci.cancel();
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
            inDimension = BlockEntityHiderCache.matchesDimension(level.dimension().location());
        } else {
            inDimension = false;
        }
        if (!inDimension) return false;

        // The id lookup and pattern match are memoized per block entity type,
        // so this no longer scans the registry or compiles regexes per frame.
        if (!BlockEntityHiderCache.isRegistered(blockEntity.getType())) {
            BlockEntityHiderCache.logUnregistered(blockEntity.getType());
            return false;
        }
        if (hider.hideAllBlocks) return true;
        return BlockEntityHiderCache.matches(blockEntity.getType());
    }

    @Unique
    private static int distSquared(BlockPos pos, Player player) {
        // block positions are integer only but the usual distSqr still calculates distance between two by converting to doubles first
        int x = pos.getX() - Mth.floor(player.getX());
        int y = pos.getY() - Mth.floor(player.getY());
        int z = pos.getZ() - Mth.floor(player.getZ());
        return x * x + y * y + z * z;
    }
}
