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
 * Horizontal tab bar. The active tab is indicated by an accent underline
 * that smoothly slides between tabs with a [LerpState].
 */
class TabBar(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float = ClickGuiTheme.tabBarHeight.toFloat(),
    val labels: List<String>,
    initialIndex: Int = 0,
) {
    private val padX: Float = 14f
    private val gap: Float = 4f
    var index: Int = initialIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
        private set
    private val underlineX = LerpState(0f, 0f, ClickGuiTheme.underlineAnimMs)
    private val underlineW = LerpState(0f, 0f, ClickGuiTheme.underlineAnimMs)
    private val hoverState = LerpState(0f, 0f, ClickGuiTheme.hoverAnimMs)
    private var lastHoverIndex: Int = -1

    fun tabWidths(): List<Pair<Float, Float>> {
        // Distribute width evenly to keep the layout stable.
        val n = labels.size.coerceAtLeast(1)
        val totalTextW = labels.sumOf { mc.font.width(it) }
        val totalGap = gap * (n - 1).coerceAtLeast(0)
        val available = (width - totalGap).coerceAtLeast(0f)
        if (totalTextW <= 0) {
            val per = available / n
            return labels.indices.map { i ->
                val start = i * (per + gap)
                start to per
            }
        }
        val scale = (available / totalTextW).coerceAtMost(1f)
        return labels.mapIndexed { i, label ->
            val w = mc.font.width(label) * scale + padX * 2f
            val start = labels.take(i).sumOf { (mc.font.width(it) * scale + padX * 2f + gap).toDouble() }.toFloat()
            start to w
        }
    }

    fun update(nowMs: Long) {
        underlineX.update(nowMs)
        underlineW.update(nowMs)
        hoverState.update(nowMs)
    }

    fun draw(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, nowMs: Long) {
        // Bar background
        with(context) {
            drawRoundedRect(
                x, y, x + width, y + height, 0f,
                fillColor = ClickGuiTheme.tabBarBg,
            )
            drawRoundedRect(
                x, y, x + width, y + height, ClickGuiTheme.panelRadius,
                fillColor = Color4b.TRANSPARENT,
                outlineColor = ClickGuiTheme.border,
                outlineWidth = 1.0f,
            )
        }

        val widths = tabWidths()
        var curX = x
        var hoveredIdx = -1
        for ((i, label) in labels.withIndex()) {
            val (start, w) = widths[i]
            val tx = x + start
            val ty = y + (height - 8f) / 2f
            val hovered = mouseX.toFloat() in tx..(tx + w) &&
                mouseY.toFloat() in y..(y + height)
            if (hovered) hoveredIdx = i
            val isActive = i == index
            val color = when {
                isActive -> ClickGuiTheme.textPrimary
                hovered -> ClickGuiTheme.textAccent
                else -> ClickGuiTheme.textSecondary
            }
            val tw = mc.font.width(label)
            context.text(mc.font, label, (tx + (w - tw) / 2f).toInt(), ty.toInt(), color.argb, false)
            curX += w + gap
        }
        lastHoverIndex = hoveredIdx
        hoverState.setTarget(if (hoveredIdx >= 0 && hoveredIdx != index) 1f else 0f, nowMs)

        // Underline
        if (widths.isNotEmpty() && index in widths.indices) {
            val (targetStart, targetW) = widths[index]
            val targetX = x + targetStart + 6f
            val targetWAdj = targetW - 12f
            underlineX.setTarget(targetX, nowMs)
            underlineW.setTarget(targetWAdj, nowMs)
            val cx = underlineX.update(nowMs)
            val cw = underlineW.update(nowMs)
            with(context) {
                drawRoundedRect(
                    cx, y + height - 2f,
                    cx + cw, y + height,
                    1f,
                    fillColor = ClickGuiTheme.accent,
                )
            }
        }
    }

    fun handleClick(mouseX: Int, mouseY: Int): Boolean {
        if (mouseY.toFloat() !in y..(y + height)) return false
        val widths = tabWidths()
        for ((i, pair) in widths.withIndex()) {
            val (start, w) = pair
            val tx = x + start
            if (mouseX.toFloat() in tx..(tx + w)) {
                if (index != i) {
                    index = i
                }
                return true
            }
        }
        return false
    }
}
