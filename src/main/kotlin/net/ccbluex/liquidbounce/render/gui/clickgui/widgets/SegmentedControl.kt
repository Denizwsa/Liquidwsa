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
package net.ccbluex.liquidbounce.render.gui.clickgui.widgets

import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.theme.ClickGuiTheme
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Segmented control: a row of N pills where exactly one is selected at a
 * time. Used for [ModeValueGroup] pickers.
 */
class SegmentedControl(
    var x: Float,
    var y: Float,
    var width: Float,
    val options: List<String>,
    initialIndex: Int = 0,
) {
    private val height: Float = ClickGuiTheme.rowHeight.toFloat() - 4f
    private val padding: Float = 2f
    var index: Int = initialIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))

    private fun segmentWidth(): Float {
        val n = options.size.coerceAtLeast(1)
        return (width - padding * 2f) / n
    }

    fun draw(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val segW = segmentWidth()
        with(context) {
            drawRoundedRect(
                x, y, x + width, y + height, height / 2f,
                fillColor = ClickGuiTheme.rowIdle,
                outlineColor = ClickGuiTheme.border,
                outlineWidth = 1.0f,
            )
        }
        if (options.isNotEmpty()) {
            val selectedX = x + padding + segW * index
            with(context) {
                drawRoundedRect(
                    selectedX, y + padding,
                    selectedX + segW, y + height - padding,
                    (height - padding * 2f) / 2f,
                    fillColor = ClickGuiTheme.accent,
                )
            }
        }
        for ((i, opt) in options.withIndex()) {
            val sx = x + padding + segW * i
            val tw = mc.font.width(opt)
            val tx = (sx + (segW - tw) / 2f).toInt()
            val ty = (y + (height - 8f) / 2f).toInt()
            val color = if (i == index) ClickGuiTheme.textOnAccent else ClickGuiTheme.textPrimary
            context.text(mc.font, opt, tx, ty, color.argb, false)
        }
    }

    fun handleClick(mouseX: Int, mouseY: Int): Boolean {
        if (mouseX.toFloat() !in x..(x + width)) return false
        if (mouseY.toFloat() !in y..(y + height)) return false
        if (options.isEmpty()) return false
        val segW = segmentWidth()
        val localX = mouseX.toFloat() - x - padding
        val newIndex = (localX / segW).toInt().coerceIn(0, options.size - 1)
        if (newIndex != index) {
            index = newIndex
            return true
        }
        return false
    }
}
