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

package net.ccbluex.liquidbounce.integration.theme.component.components.native

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.TargetChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import kotlin.math.floor

/**
 * Native (non-browser) target HUD. Listens to [TargetChangeEvent] (emitted by CombatManager) and
 * shows the last combat target for a short window. Renders only while the native HUD theme is active.
 *
 * Mirrors `src-theme/src/routes/hud/elements/targethud/TargetHud.svelte` (avatar & armor durability
 * are omitted in this phase).
 */
object NativeTargetHudComponent : NativeHudComponent(
    "TargetHud", true, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.CENTER,
        horizontalOffset = 0,
        verticalAlignment = Alignment.ScreenAxisY.TOP,
        verticalOffset = 30,
    )
) {

    private val textShadow by boolean("Shadow", true)
    private val visibleDurationMs by int("VisibleDuration", 1000, 0..10000)

    private const val PAD_X = 12f
    private const val PAD_Y = 8f
    private const val BAR_HEIGHT = 6f
    private const val MIN_WIDTH = 110f
    private const val RADIUS = 5f

    @Volatile
    private var target: PlayerData? = null
    @Volatile
    private var lastUpdate = 0L

    init {
        registerComponentListen(this)
    }

    @Suppress("unused")
    val targetHandler = handler<TargetChangeEvent> { event ->
        val newTarget = event.target ?: return@handler
        target = newTarget
        lastUpdate = System.currentTimeMillis()
    }

    @Suppress("unused")
    val renderHandler = handler<OverlayRenderEvent> { event ->
        if (!enabled || !ModuleHud.running || HideAppearance.isHidingNow || !ThemeManager.isNativeActive) {
            return@handler
        }

        val currentTarget = target ?: return@handler
        if (System.currentTimeMillis() - lastUpdate > visibleDurationMs) {
            return@handler
        }

        val fontRenderer = FontManager.FONT_RENDERER
        val scale = fontRenderer.scaleToVanillaFont
        val lineHeight = fontRenderer.height * scale

        val nameComponent = currentTarget.username.asPlainText(Style.EMPTY + NativeHudPalette.TEXT)

        val health = floor(currentTarget.health).toInt()
        val maxHealth = floor(currentTarget.maxHealth).toInt()
        val absorption = floor(currentTarget.absorption).toInt()

        val statsParts = ArrayList<Component>()
        statsParts.add("$health/$maxHealth".asPlainText(Style.EMPTY + NativeHudPalette.ERROR))
        if (absorption > 0) {
            statsParts.add("  +$absorption".asPlainText(Style.EMPTY + Color4b(0xEF, 0xBF, 0x04)))
        }
        if (currentTarget.armor > 0) {
            statsParts.add("  Armor ${currentTarget.armor}".asPlainText(Style.EMPTY + NativeHudPalette.DIMMED))
        }
        val statsComponent = textOf(*statsParts.toTypedArray())

        val nameWidth = fontRenderer.getStringWidth(
            fontRenderer.process(nameComponent, NativeHudPalette.TEXT), textShadow
        ) * scale
        val statsWidth = fontRenderer.getStringWidth(
            fontRenderer.process(statsComponent, NativeHudPalette.TEXT), textShadow
        ) * scale

        val contentWidth = maxOf(nameWidth, statsWidth, MIN_WIDTH)
        val blockWidth = contentWidth + PAD_X * 2f
        val blockHeight = PAD_Y * 2f + lineHeight * 2f + 2f + BAR_HEIGHT + 2f
        val bb = alignment.getBounds(blockWidth, blockHeight)

        val healthCap = (currentTarget.maxHealth + currentTarget.absorption).coerceAtLeast(1f)
        val healthRatio = (currentTarget.health / healthCap).coerceIn(0f, 1f)

        with(event.context) {
            // Panel background
            drawRoundedRect(bb.xMin, bb.yMin, bb.xMax, bb.yMax, RADIUS, NativeHudPalette.BG_68)

            // Name
            fontRenderer.draw(nameComponent) {
                x = bb.xMin + PAD_X
                y = bb.yMin + PAD_Y
                this.scale = scale
                shadow = textShadow
                horizontalAnchor = HorizontalAnchor.START
                verticalAnchor = VerticalAnchor.TOP
            }

            // Stats
            fontRenderer.draw(statsComponent) {
                x = bb.xMin + PAD_X
                y = bb.yMin + PAD_Y + lineHeight + 2f
                this.scale = scale
                shadow = textShadow
                horizontalAnchor = HorizontalAnchor.START
                verticalAnchor = VerticalAnchor.TOP
            }

            // Health progress bar at the bottom (full width)
            val barTop = bb.yMax - BAR_HEIGHT
            drawQuad(bb.xMin, barTop, bb.xMax, bb.yMax, NativeHudPalette.BG_30)
            drawQuad(
                bb.xMin, barTop,
                bb.xMin + (bb.xMax - bb.xMin) * healthRatio, bb.yMax,
                NativeHudPalette.ERROR
            )
        }
    }
}
