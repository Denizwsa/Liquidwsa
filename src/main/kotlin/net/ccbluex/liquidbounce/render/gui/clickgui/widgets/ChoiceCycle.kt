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
 * Two-button cycle selector of the form:  [<] ChoiceName [>].
 * The caller provides the list of options; clicking left/right changes
 * the selection. Used for single-choice values.
 */
class ChoiceCycle(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float = ClickGuiTheme.rowHeight.toFloat() - 4f,
    val options: List<String>,
    initialIndex: Int = 0,
) {
    var index: Int = initialIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))

    fun next() {
        if (options.isEmpty()) return
        index = (index + 1) % options.size
    }

    fun prev() {
        if (options.isEmpty()) return
        index = (index - 1 + options.size) % options.size
    }

    fun current(): String = options.getOrNull(index) ?: ""

    fun draw(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, textColor: Color4b) {
        val arrowW = 18f
        val bodyX = x + arrowW
        val bodyW = width - arrowW * 2f

        val isHovered = isHovered(mouseX, mouseY)
        val bgColor = if (isHovered) ClickGuiTheme.rowHover else ClickGuiTheme.rowIdle

        with(context) {
            drawRoundedRect(
                x, y, x + width, y + height, ClickGuiTheme.buttonRadius,
                fillColor = bgColor,
                outlineColor = ClickGuiTheme.border,
                outlineWidth = 1.0f,
            )
            drawRoundedRect(
                x, y, x + arrowW, y + height, ClickGuiTheme.buttonRadius,
                fillColor = Color4b.TRANSPARENT,
            )
            drawRoundedRect(
                x + width - arrowW, y, x + width, y + height, ClickGuiTheme.buttonRadius,
                fillColor = Color4b.TRANSPARENT,
            )
        }

        val label = current()
        if (label.isNotEmpty()) {
            val tw = mc.font.width(label)
            val tx = (bodyX + (bodyW - tw) / 2f).toInt()
            val ty = (y + (height - 8f) / 2f).toInt()
            context.text(mc.font, label, tx, ty, textColor.argb, false)
        }

        // Arrow markers
        val arrowColor = ClickGuiTheme.textSecondary
        context.text(
            mc.font, "<",
            (x + 5f).toInt(), (y + (height - 8f) / 2f).toInt(),
            arrowColor.argb, false,
        )
        context.text(
            mc.font, ">",
            (x + width - 14f).toInt(), (y + (height - 8f) / 2f).toInt(),
            arrowColor.argb, false,
        )
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        return mx in x..(x + width) && my in y..(y + height)
    }

    fun handleClick(mouseX: Int, mouseY: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        val arrowW = 18f
        when {
            mouseX.toFloat() < x + arrowW -> prev()
            mouseX.toFloat() > x + width - arrowW -> next()
            else -> next()
        }
        return true
    }
}
