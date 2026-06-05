/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.OverlayMessageEvent;
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.ModuleCrosshair;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Final
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE_LOCATION;

    @Shadow
    @Nullable
    protected abstract Player getCameraPlayer();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void hookRenderEventStart(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        EventManager.INSTANCE.callEvent(new OverlayRenderEvent(context, tickCounter.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderSpyglassOverlay(GuiGraphicsExtractor context, float scale, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.SPYGLASS_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(GuiGraphicsExtractor context, Identifier texture, float opacity, CallbackInfo callback) {
        if (!ModuleAntiBlind.INSTANCE.getRunning()) {
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.PUMPKIN_BLUR) && ModuleAntiBlind.TEXTURE_PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
            return;
        }

        if (!ModuleAntiBlind.canRender(DoRender.POWDER_SNOW_FOG) && POWDER_SNOW_OUTLINE_LOCATION.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamRenderCrosshairInThirdPerson(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if ((ModuleFreeCam.INSTANCE.getRunning() && ModuleFreeCam.INSTANCE.shouldDisableCameraInteract())
                || ModuleCrosshair.INSTANCE.getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderPortalOverlay(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.PORTAL_OVERLAY)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void hookRenderTitleAndSubtitle(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.TITLE)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNauseaOverlay(GuiGraphicsExtractor context, float distortionStrength, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.NAUSEA)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "extractItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean hookOffhandItem(boolean original) {
        return original || ModuleSwordBlock.INSTANCE.shouldHideOffhand() && ModuleSwordBlock.INSTANCE.getHideShieldSlot();
    }

    @ModifyExpressionValue(method = "extractCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnCrosshair(CameraType original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyExpressionValue(method = "extractCameraOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"
            )
    )
    private CameraType hookPerspectiveEventOnMiscOverlays(CameraType original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyReceiver(
        method = "extractCrosshair",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/AttackRange;isInRange(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Z"
        )
    )
    private AttackRange injectReachAttackRange(AttackRange instance, LivingEntity entity, Vec3 pos) {
        if (ModuleReach.INSTANCE.getRunning()) {
            return ModuleReach.INSTANCE.getEntity().adjustAttackRange(instance);
        }

        return instance;
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void hookSetOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new OverlayMessageEvent(message, tinted));
    }
}
