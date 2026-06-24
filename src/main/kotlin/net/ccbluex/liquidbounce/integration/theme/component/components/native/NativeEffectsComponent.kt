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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * Native (non-browser) potion effects list. Renders only while the native HUD theme is active.
 *
 * Mirrors `src-theme/src/routes/hud/elements/Effects.svelte` (icons omitted in this phase; text-first).
 */
object NativeEffectsComponent : NativeHudComponent(
    "Effects", true, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.RIGHT,
        horizontalOffset = 0,
        verticalAlignment = Alignment.ScreenAxisY.CENTER_TRANSLATED,
        verticalOffset = 0,
    )
) {

    private val textShadow by boolean("Shadow", true)

    private const val PAD_X = 6f
    private const val PAD_Y = 4f
    private const val GAP = 10f
    private const val RADIUS = 5f

    init {
        registerComponentListen(this)
    }

    private class EffectRow(val name: Component, val duration: Component, val width: Float)

    private fun formatDuration(ticks: Int): String {
        if (ticks < 0) {
            return "**:**" // infinite
        }
        val totalSeconds = ticks / 20
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    @Suppress("unused")
    val renderHandler = handler<OverlayRenderEvent> { event ->
        if (!enabled || !ModuleHud.running || HideAppearance.isHidingNow || !ThemeManager.isNativeActive) {
            return@handler
        }

        val activeEffects = player.activeEffects
        if (activeEffects.isEmpty()) {
            return@handler
        }

        val fontRenderer = FontManager.FONT_RENDERER
        val scale = fontRenderer.scaleToVanillaFont
        val lineHeight = fontRenderer.height * scale
        val rowHeight = lineHeight + 2f

        val rows = ArrayList<EffectRow>()
        var maxWidth = 0f
        for (effect in activeEffects) {
            val displayName = effect.effect.value().displayName
            val name: Component = if (effect.amplifier > 0) {
                textOf(
                    displayName,
                    " ".asPlainText(),
                    (effect.amplifier + 1).toString().asPlainText(Style.EMPTY + NativeHudPalette.ERROR),
                )
            } else {
                displayName
            }
            val duration = formatDuration(if (effect.isInfiniteDuration) -1 else effect.duration)
                .asPlainText(Style.EMPTY + NativeHudPalette.TEXT)

            val nameWidth = fontRenderer.getStringWidth(
                fontRenderer.process(name, NativeHudPalette.TEXT), textShadow
            ) * scale
            val durWidth = fontRenderer.getStringWidth(
                fontRenderer.process(duration, NativeHudPalette.TEXT), textShadow
            ) * scale

            val rowWidth = nameWidth + GAP + durWidth
            maxWidth = maxOf(maxWidth, rowWidth)
            rows.add(EffectRow(name, duration, rowWidth))
        }

        val contentWidth = maxWidth
        val blockWidth = contentWidth + PAD_X * 2f
        val blockHeight = rows.size * rowHeight + PAD_Y * 2f
        val bb = alignment.getBounds(blockWidth, blockHeight)

        with(event.context) {
            drawRoundedRect(bb.xMin, bb.yMin, bb.xMax, bb.yMax, RADIUS, NativeHudPalette.BG_50)

            rows.forEachIndexed { index, row ->
                val rowTop = bb.yMin + PAD_Y + index * rowHeight

                fontRenderer.draw(row.name) {
                    x = bb.xMin + PAD_X
                    y = rowTop
                    this.scale = scale
                    shadow = textShadow
                    horizontalAnchor = HorizontalAnchor.START
                    verticalAnchor = VerticalAnchor.TOP
                }

                fontRenderer.draw(row.duration) {
                    x = bb.xMax - PAD_X
                    y = rowTop
                    this.scale = scale
                    shadow = textShadow
                    horizontalAnchor = HorizontalAnchor.END
                    verticalAnchor = VerticalAnchor.TOP
                }
            }
        }
    }
}
