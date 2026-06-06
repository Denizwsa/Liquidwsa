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
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth

/**
 * Horizontal range slider with a track, filled portion, and circular thumb.
 * The caller passes the value range and a callback that receives the new
 * value (already clamped to the range). Drag is performed by the screen.
 */
class RangeSlider(
    var x: Float,
    var y: Float,
    var width: Float,
    val minValue: Double,
    val maxValue: Double,
    val isInteger: Boolean,
    initialValue: Double,
) {
    var value: Double = initialValue.coerceIn(minValue, maxValue)
    private val trackHeight = 4f
    private val thumbRadius = 5f

    fun ratio(): Float {
        if (maxValue <= minValue) return 0f
        return ((value - minValue) / (maxValue - minValue)).toFloat().coerceIn(0f, 1f)
    }

    fun draw(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        textColor: Color4b = ClickGuiTheme.textPrimary,
    ): String {
        val trackY = y + (ClickGuiTheme.rowHeight - trackHeight) / 2f
        val ratio = ratio()

        with(context) {
            drawRoundedRect(
                x, trackY, x + width, trackY + trackHeight, trackHeight / 2f,
                fillColor = ClickGuiTheme.sliderTrack,
            )
            if (ratio > 0f) {
                val filledX = x + width * ratio
                drawRoundedRect(
                    x, trackY, filledX, trackY + trackHeight, trackHeight / 2f,
                    fillColor = ClickGuiTheme.sliderFill,
                )
            }
            val thumbX = x + width * ratio
            val thumbY = trackY + trackHeight / 2f
            drawRoundedRect(
                thumbX - thumbRadius, thumbY - thumbRadius,
                thumbX + thumbRadius, thumbY + thumbRadius,
                thumbRadius,
                fillColor = ClickGuiTheme.sliderThumb,
                outlineColor = Color4b(60, 100, 200, 200),
                outlineWidth = 1.0f,
            )
        }

        return formatValue()
    }

    fun isOnTrack(mouseX: Int, mouseY: Int): Boolean {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        val trackY = y + (ClickGuiTheme.rowHeight - trackHeight) / 2f
        return mx in (x - 4f)..(x + width + 4f) && my in (trackY - 6f)..(trackY + trackHeight + 6f)
    }

    fun updateFromMouse(mouseX: Int): Boolean {
        if (maxValue <= minValue) return false
        val mx = mouseX.toFloat().coerceIn(x, x + width)
        val ratio = ((mx - x) / width).coerceIn(0f, 1f)
        val raw = minValue + (maxValue - minValue) * ratio
        val newVal = if (isInteger) raw.toInt().toDouble() else raw
        val clamped = newVal.coerceIn(minValue, maxValue)
        if (Mth.equal(clamped, value)) return false
        value = clamped
        return true
    }

    private fun formatValue(): String {
        return if (isInteger) {
            value.toInt().toString()
        } else {
            "%.2f".format(value)
        }
    }
}
