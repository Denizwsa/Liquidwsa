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
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * Native (non-browser) ArrayList: lists enabled modules. Renders only while the native HUD theme
 * is active, so it never overlaps with the web ArrayList.
 *
 * Mirrors `src-theme/src/routes/hud/elements/ArrayList.svelte`.
 */
object NativeArrayListComponent : NativeHudComponent(
    "ArrayList", true, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.RIGHT,
        horizontalOffset = 0,
        verticalAlignment = Alignment.ScreenAxisY.TOP,
        verticalOffset = 0,
    )
) {

    private val showTags by boolean("ShowTags", true)
    private val textShadow by boolean("Shadow", true)

    private const val PAD_X = 5f
    private const val PAD_Y = 2f
    private const val BAR_WIDTH = 2f

    init {
        registerComponentListen(this)
    }

    private class Row(val text: Component, val width: Float)

    @Suppress("unused")
    val renderHandler = handler<OverlayRenderEvent> { event ->
        if (!enabled || !ModuleHud.running || HideAppearance.isHidingNow || !ThemeManager.isNativeActive) {
            return@handler
        }

        val fontRenderer = FontManager.FONT_RENDERER
        val scale = fontRenderer.scaleToVanillaFont
        val lineHeight = fontRenderer.height * scale
        val rowHeight = lineHeight + PAD_Y * 2f

        // Build the rows (one per enabled, non-hidden module), measuring each.
        val rows = ArrayList<Row>()
        var maxWidth = 0f
        for (module in ModuleManager) {
            if (!module.enabled || module.hidden) {
                continue
            }

            val tag = module.tag
            val component: Component = if (showTags && tag != null) {
                textOf(
                    module.name.asPlainText(),
                    " ".asPlainText(),
                    tag.asPlainText(Style.EMPTY + NativeHudPalette.TAG),
                )
            } else {
                module.name.asPlainText()
            }

            // process() may recycle its result, so only derive a width here and re-process on draw.
            val width = fontRenderer.getStringWidth(
                fontRenderer.process(component, Color4b.WHITE), textShadow
            ) * scale
            maxWidth = maxOf(maxWidth, width)
            rows.add(Row(component, width))
        }

        if (rows.isEmpty()) {
            return@handler
        }

        // Longest first -> classic descending staircase
        rows.sortByDescending { it.width }

        val blockWidth = maxWidth + PAD_X * 2f + BAR_WIDTH
        val blockHeight = rowHeight * rows.size
        val bb = alignment.getBounds(blockWidth, blockHeight)

        with(event.context) {
            rows.forEachIndexed { index, row ->
                val rowTop = bb.yMin + index * rowHeight
                val rowBottom = rowTop + rowHeight
                val rowWidth = row.width + PAD_X * 2f + BAR_WIDTH
                val rowLeft = bb.xMax - rowWidth

                // Row background
                drawQuad(rowLeft, rowTop, bb.xMax, rowBottom, NativeHudPalette.BG_68)
                // Accent left bar (matches CSS border-left)
                drawQuad(rowLeft, rowTop, rowLeft + BAR_WIDTH, rowBottom, NativeHudPalette.ACCENT)

                fontRenderer.draw(row.text) {
                    x = bb.xMax - PAD_X
                    y = rowTop + PAD_Y
                    this.scale = scale
                    shadow = textShadow
                    horizontalAnchor = HorizontalAnchor.END
                    verticalAnchor = VerticalAnchor.TOP
                }
            }
        }
    }
}
