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
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Modern pill switch. Renders a track + thumb that slides between off/on
 * positions with a smooth lerp animation. The caller owns the
 * [LerpState] instance so it can persist across frames.
 */
class PillSwitch(
    var x: Float,
    var y: Float,
    var width: Float = 36f,
    var height: Float = 18f,
    private val animMs: Long = ClickGuiTheme.toggleAnimMs,
) {
    private val state = LerpState(current = 0f, target = 0f, animMs)
    private val hoverState = LerpState(0f, 0f, ClickGuiTheme.hoverAnimMs)
    private var isOn: Boolean = false

    fun setOn(on: Boolean, nowMs: Long) {
        isOn = on
        state.setTarget(if (on) 1f else 0f, nowMs)
    }

    fun isOn(): Boolean = isOn

    fun update(nowMs: Long) {
        state.update(nowMs)
        hoverState.update(nowMs)
    }

    fun draw(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        nowMs: Long,
    ) {
        val t = state.update(nowMs)
        val hover = isHovered(mouseX, mouseY)
        hoverState.setTarget(if (hover) 1f else 0f, nowMs)
        val hov = hoverState.update(nowMs)

        val trackColor = lerpColor(ClickGuiTheme.switchTrackOff, ClickGuiTheme.switchTrackOn, t)
        val padding = 2f
        val thumbSize = height - padding * 2f
        val thumbX = padding + (width - padding * 2f - thumbSize) * t

        with(context) {
            drawRoundedRect(
                x, y, x + width, y + height, height / 2f,
                fillColor = trackColor,
            )
            val thumbColor = ClickGuiTheme.switchThumb
            val thumbXCenter = x + thumbX + thumbSize / 2f
            val thumbYCenter = y + padding + thumbSize / 2f
            val thumbAlpha = (thumbColor.a * (1f - 0.18f * hov)).toInt().coerceIn(0, 255)
            drawRoundedRect(
                x + thumbX, y + padding,
                x + thumbX + thumbSize, y + padding + thumbSize,
                thumbSize / 2f,
                fillColor = Color4b(thumbColor.r, thumbColor.g, thumbColor.b, thumbAlpha),
            )
        }
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        return mx in x..(x + width) && my in y..(y + height)
    }

    fun handleClick(mouseX: Int, mouseY: Int, nowMs: Long): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        isOn = !isOn
        state.setTarget(if (isOn) 1f else 0f, nowMs)
        return true
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
