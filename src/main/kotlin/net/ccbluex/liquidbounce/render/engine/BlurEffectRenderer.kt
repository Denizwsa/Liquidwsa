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
package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.gui.screens.ChatScreen

object BlurEffectRenderer : MinecraftShortcuts, EventListener {

    var isDrawingHudFramebuffer = false

    val overlayRenderTargetHolder = LazyRenderTargetHolder(
        "${LiquidBounce.CLIENT_NAME} BlurOverlay",
        useDepth = true,
    )

    private val overlaySampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)

    private val GUI_BLUR_UNIFORM_BUFFER = ClientUniformDefine.GUI_BLUR.createSingleBuffer()

    private fun hasNoFullScreen(): Boolean =
        mc.screen == null || mc.screen is ChatScreen || FeatureSilentScreen.shouldHide

    fun shouldDrawBlur(): Boolean = false

    fun blitBlurOverlay() {
        // No-op: blur effect has been removed.
    }
}
