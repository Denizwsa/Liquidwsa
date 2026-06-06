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
import net.ccbluex.liquidbounce.render.gui.clickgui.theme.LerpState
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Real-time search input. The current [text] can be read by the screen to
 * filter modules. The bar shows a soft placeholder when empty and a focus
 * pulse when active.
 */
class SearchBar(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float = 22f,
) {
    var text: String = ""
    var focused: Boolean = false
    private val focusState = LerpState(0f, 0f, ClickGuiTheme.hoverAnimMs)
    private val pulseState = LerpState(0f, 0f, ClickGuiTheme.searchPulseMs)

    fun setFocused(value: Boolean, nowMs: Long) {
        focused = value
        focusState.setTarget(if (value) 1f else 0f, nowMs)
    }

    fun update(nowMs: Long) {
        focusState.update(nowMs)
        pulseState.update(nowMs)
    }

    fun draw(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, nowMs: Long) {
        focusState.update(nowMs)
        val focusT = focusState.update(nowMs)

        val baseColor = lerpColor(ClickGuiTheme.searchBg, ClickGuiTheme.searchBgFocused, focusT)
        val pulse = (0.5 + 0.5 * Math.sin(nowMs / 800.0)).toFloat()
        val glowAlpha = (ClickGuiTheme.accentGlow.a * focusT * (0.6f + 0.4f * pulse)).toInt().coerceIn(0, 255)
        val accent = Color4b(ClickGuiTheme.accent.r, ClickGuiTheme.accent.g, ClickGuiTheme.accent.b, glowAlpha)

        with(context) {
            // Soft glow
            if (focusT > 0.01f) {
                drawRoundedRect(
                    x - 2f, y - 2f, x + width + 2f, y + height + 2f, height / 2f + 2f,
                    fillColor = accent,
                )
            }
            drawRoundedRect(
                x, y, x + width, y + height, height / 2f,
                fillColor = baseColor,
                outlineColor = Color4b(
                    ClickGuiTheme.border.r, ClickGuiTheme.border.g, ClickGuiTheme.border.b,
                    (180 + 60 * focusT).toInt().coerceIn(0, 255)
                ),
                outlineWidth = 1.0f,
            )
        }

        val tx = (x + 10f).toInt()
        val ty = (y + (height - 8f) / 2f).toInt()
        val drawText: String
        val drawColor: Color4b
        if (text.isEmpty()) {
            drawText = "Search modules..."
            drawColor = ClickGuiTheme.searchPlaceholder
        } else {
            drawText = text
            drawColor = ClickGuiTheme.textPrimary
        }
        context.text(mc.font, drawText, tx, ty, drawColor.argb, false)

        // Caret
        if (focused) {
            val caretX = (x + 12f + mc.font.width(text).toFloat()).toInt()
            val caretOn = ((nowMs / 500L) % 2L) == 0L
            if (caretOn) {
                with(context) {
                    drawRoundedRect(
                        caretX.toFloat(), y + 4f, (caretX + 1).toFloat(), y + height - 4f,
                        0.5f,
                        fillColor = ClickGuiTheme.accent,
                    )
                }
            }
        }
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        return mx in x..(x + width) && my in y..(y + height)
    }

    fun appendChar(c: Char) {
        if (text.length < 32) {
            text += c
        }
    }

    fun backspace() {
        if (text.isNotEmpty()) text = text.dropLast(1)
    }

    private fun lerpColor(a: Color4b, b: Color4b, t: Float): Color4b {
        val tt = t.coerceIn(0f, 1f)
        return Color4b(
            (a.r + (b.r - a.r) * tt).toInt().coerceIn(0, 255),
            (a.g + (b.g - a.g) * tt).toInt().coerceIn(0, 255),
            (a.b + (b.b - a.b) * tt).toInt().coerceIn(0, 255),
            (a.a + (b.a - a.a) * tt).toInt().coerceIn(0, 255),
        )
    }
}
