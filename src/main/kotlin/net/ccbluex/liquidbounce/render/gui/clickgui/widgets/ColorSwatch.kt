/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 * A small color preview swatch that, when clicked, advances to the next
 * preset or triggers the parent screen to open a color picker overlay.
 * For simplicity, click cycles the swatch's alpha (full <-> half <-> off)
 * and shows the hex code on the right.
 */
class ColorSwatch(
    var x: Float,
    var y: Float,
    val size: Float = 18f,
    var color: Color4b,
) {
    fun draw(context: GuiGraphicsExtractor) {
        with(context) {
            drawRoundedRect(
                x, y, x + size, y + size, 3f,
                fillColor = ClickGuiTheme.rowIdle,
                outlineColor = ClickGuiTheme.border,
                outlineWidth = 1.0f,
            )
            val pad = 2f
            drawRoundedRect(
                x + pad, y + pad, x + size - pad, y + size - pad, 2f,
                fillColor = color,
            )
        }
    }

    fun drawHex(context: GuiGraphicsExtractor, rightX: Float, yCenter: Float) {
        val hex = String.format("#%02X%02X%02X", color.r, color.g, color.b)
        val tw = mc.font.width(hex)
        val tx = (rightX - tw).toInt()
        val ty = (yCenter - 4f).toInt()
        context.text(mc.font, hex, tx, ty, ClickGuiTheme.textAccent.argb, false)
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        return mx in x..(x + size) && my in y..(y + size)
    }

    fun cycleAlpha() {
        color = when {
            color.a >= 250 -> Color4b(color.r, color.g, color.b, 130)
            color.a >= 130 -> Color4b(color.r, color.g, color.b, 60)
            else -> Color4b(color.r, color.g, color.b, 255)
        }
    }
}
