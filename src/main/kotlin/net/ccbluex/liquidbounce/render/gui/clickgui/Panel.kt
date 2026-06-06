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
package net.ccbluex.liquidbounce.render.gui.clickgui

import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth
import kotlin.math.roundToInt

/**
 * One draggable category panel in the Svelte-style ClickGUI. Owns its
 * position, expanded/collapsed state, scroll offset, and z-index. The
 * z-index increases on every drag so the most recently interacted panel
 * always paints on top.
 */
class Panel(val category: ModuleCategory) {

    var x: Int = 0
    var y: Int = 0
    var width: Int = 110
    var zIndex: Int = 0
    var expanded: Boolean = false
    var scrollOffset: Int = 0

    private val moduleElements: List<ModuleElement> = ModuleManager
        .filter { it.category == category }
        .map(::ModuleElement)

    fun moduleElements(): List<ModuleElement> = moduleElements

    fun moduleByName(name: String): ModuleElement? = moduleElements.firstOrNull { it.module.name == name }

    private var dragging: Boolean = false
    private var dragOffsetX: Int = 0
    private var dragOffsetY: Int = 0
    private var ignoreGrid: Boolean = false

    val totalContentHeight: Int
        get() = moduleElements.sumOf { it.height + 1 }

    val visibleContentHeight: Int
        get() = (mc.window.guiScaledHeight - 40).coerceAtLeast(0)

    val canScroll: Boolean
        get() = totalContentHeight > visibleContentHeight

    init {
        loadConfig()
    }

    fun bringToFront() {
        zIndex = ++currentMaxZIndex
    }

    fun render(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        highlightedModule: ModuleElement?,
    ) {
        clampToScreen()

        val headerH = ClickGuiTheme.panelHeaderHeight
        // Header background
        context.fill(x, y, x + width, y + headerH, ClickGuiTheme.panelHeaderBg.argb)
        // Title
        val title = category.tag
        val tw = mc.font.width(title)
        context.text(
            mc.font, title, x + (width - tw) / 2, y + (headerH - 8) / 2,
            ClickGuiTheme.panelHeaderText.argb, true
        )
        // Expand arrow
        val cx = x + width - 8
        val cy = y + headerH / 2
        if (expanded) {
            context.fill(cx - 3, cy - 1, cx, cy, ClickGuiTheme.textDimmed.argb)
        } else {
            context.fill(cx - 3, cy - 1, cx - 3, cy + 1, ClickGuiTheme.textDimmed.argb)
            context.fill(cx - 2, cy, cx, cy, ClickGuiTheme.textDimmed.argb)
        }

        if (!expanded) return

        val bodyX = x
        val bodyY = y + headerH
        val bodyW = width
        val bodyH = visibleContentHeight
        // Body background
        context.fill(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH, ClickGuiTheme.panelBg.argb)

        // Scroll clip
        context.enableScissor(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH)
        var rowY = bodyY - scrollOffset
        for (m in moduleElements) {
            if (rowY + m.height > bodyY && rowY < bodyY + bodyH) {
                m.render(
                    context, bodyX, rowY, bodyW,
                    mouseX, mouseY, partialTick,
                    m === highlightedModule
                )
            }
            rowY += m.height + 1
        }
        context.disableScissor()

        // Scrollbar
        if (canScroll) {
            val sbX = x + width - 3
            val sbTrackH = bodyH
            val ratio = (visibleContentHeight.toFloat() / totalContentHeight).coerceIn(0.1f, 1f)
            val sbH = (sbTrackH * ratio).toInt().coerceAtLeast(16)
            val maxScroll = totalContentHeight - visibleContentHeight
            val sbY = bodyY + ((sbTrackH - sbH).toFloat() * (scrollOffset.toFloat() / maxScroll.coerceAtLeast(1))).toInt()
            context.fill(sbX, sbY, sbX + 2, sbY + sbH, ClickGuiTheme.moduleEnabled.argb)
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        val headerH = ClickGuiTheme.panelHeaderHeight
        if (mouseX in x..(x + width) && mouseY in y..(y + headerH)) {
            if (button == 0) {
                dragging = true
                dragOffsetX = mouseX - x
                dragOffsetY = mouseY - y
                bringToFront()
                return true
            }
            if (button == 1) {
                expanded = !expanded
                saveConfig()
                return true
            }
            if (button == 2) {
                expanded = !expanded
                saveConfig()
                return true
            }
        }
        if (expanded) {
            // Forward to module elements
            val bodyX = x
            val bodyY = y + headerH
            val bodyW = width
            var rowY = bodyY - scrollOffset
            for (m in moduleElements) {
                if (m.handleMouseClick(mouseX, mouseY, bodyX, rowY, bodyW, button)) return true
                rowY += m.height + 1
            }
        }
        return false
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (dragging && button == 0) {
            dragging = false
            saveConfig()
            return true
        }
        if (expanded) {
            var rowY = y + ClickGuiTheme.panelHeaderHeight - scrollOffset
            for (m in moduleElements) {
                if (m.handleMouseRelease(mouseX, mouseY, x, rowY, width, button)) return true
                rowY += m.height + 1
            }
        }
        return false
    }

    fun mouseDragged(mouseX: Int, mouseY: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        if (dragging && button == 0) {
            val nx = mouseX - dragOffsetX
            val ny = mouseY - dragOffsetY
            x = snap(nx).toInt()
            y = snap(ny).toInt()
            clampToScreen()
            return true
        }
        if (expanded) {
            var rowY = y + ClickGuiTheme.panelHeaderHeight - scrollOffset
            for (m in moduleElements) {
                if (m.handleMouseDrag(mouseX, mouseY, x, rowY, width, button, dragX, dragY)) return true
                rowY += m.height + 1
            }
        }
        return false
    }

    fun mouseScrolled(mouseX: Int, mouseY: Int, scrollX: Double, scrollY: Double): Boolean {
        if (!expanded) return false
        if (mouseX !in x..(x + width)) return false
        if (mouseY !in y..(y + visibleContentHeight + ClickGuiTheme.panelHeaderHeight)) return false
        if (!canScroll) return false
        val maxScroll = (totalContentHeight - visibleContentHeight).coerceAtLeast(0)
        scrollOffset = (scrollOffset + scrollY.toInt() * 12).coerceIn(0, maxScroll)
        saveConfig()
        return true
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!expanded) return false
        var rowY = y + ClickGuiTheme.panelHeaderHeight - scrollOffset
        for (m in moduleElements) {
            if (m.handleKeyPressed(keyCode, scanCode, modifiers)) return true
            rowY += m.height + 1
        }
        return false
    }

    fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!expanded) return false
        var rowY = y + ClickGuiTheme.panelHeaderHeight - scrollOffset
        for (m in moduleElements) {
            if (m.handleCharTyped(codePoint, modifiers)) return true
            rowY += m.height + 1
        }
        return false
    }

    fun moduleAt(mouseX: Int, mouseY: Int): ModuleElement? {
        if (!expanded) return null
        var rowY = y + ClickGuiTheme.panelHeaderHeight - scrollOffset
        for (m in moduleElements) {
            if (mouseX in x..(x + width) && mouseY in rowY..(rowY + m.height)) return m
            rowY += m.height + 1
        }
        return null
    }

    fun onKeyShift(pressed: Boolean) {
        ignoreGrid = pressed
    }

    private fun snap(value: Int): Float {
        if (ignoreGrid) return value.toFloat()
        return (value / ClickGuiTheme.gridSize).roundToInt() * ClickGuiTheme.gridSize
    }

    private fun clampToScreen() {
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        x = x.coerceIn(0, (sw - width).coerceAtLeast(0))
        y = y.coerceIn(0, (sh - ClickGuiTheme.panelHeaderHeight).coerceAtLeast(0))
    }

    private fun configKey(suffix: String): String = "clickgui.panel.${category.tag}.$suffix"

    private fun loadConfig() {
        val px = ClickGuiConfig.getInt(configKey("x"), -1)
        val py = ClickGuiConfig.getInt(configKey("y"), -1)
        if (px >= 0) x = px
        if (py >= 0) y = py
        expanded = ClickGuiConfig.getBoolean(configKey("expanded"), false)
        scrollOffset = ClickGuiConfig.getInt(configKey("scroll"), 0)
        zIndex = ClickGuiConfig.getInt(configKey("zIndex"), 0)
        if (zIndex > currentMaxZIndex) currentMaxZIndex = zIndex
    }

    fun saveConfig() {
        ClickGuiConfig.put(configKey("x"), x)
        ClickGuiConfig.put(configKey("y"), y)
        ClickGuiConfig.put(configKey("expanded"), expanded)
        ClickGuiConfig.put(configKey("scroll"), scrollOffset)
        ClickGuiConfig.put(configKey("zIndex"), zIndex)
    }

    companion object {
        private var currentMaxZIndex: Int = 0
    }
}
