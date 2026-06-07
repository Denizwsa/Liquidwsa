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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

/**
 * Top-center search box for the ClickGUI. Filters the global module list by
 * name/aliases and exposes keyboard navigation (arrow up/down, enter to
 * toggle, escape to clear) so users can quickly jump to a module without
 * opening the matching category panel.
 */
class SearchBar(private val onSelect: (ClientModule) -> Unit) {

    var query: String = ""
        private set

    var selectedIndex: Int = 0
        private set

    var focused: Boolean = false
        private set

    private var caretBlink: Long = 0L
    private var caretVisible: Boolean = true

    val results: List<ClientModule>
        get() {
            if (query.isBlank()) return emptyList()
            val q = query.lowercase().replace(" ", "")
            return ModuleManager.filter {
                q in it.name.lowercase() || it.aliases.any { alias -> q in alias.lowercase() }
            }
        }

    fun moveSelection(delta: Int) {
        val r = results
        if (r.isEmpty()) {
            selectedIndex = 0
            return
        }
        var next = selectedIndex + delta
        if (next < 0) next = r.size - 1
        if (next >= r.size) next = 0
        selectedIndex = next
    }

    fun toggleSelected() {
        val r = results
        if (selectedIndex !in r.indices) return
        val m = r[selectedIndex]
        m.enabled = !m.enabled
    }

    fun focus() {
        focused = true
    }

    fun clear() {
        query = ""
        selectedIndex = 0
        focused = false
    }

    fun render(context: GuiGraphicsExtractor, screenWidth: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        val w = ClickGuiTheme.searchBarWidth
        val h = ClickGuiTheme.searchBarHeight
        val x = (screenWidth - w) / 2
        val y = 8

        val bg = ClickGuiTheme.searchBarBg
        context.fill(x, y, x + w, y + h, bg.argb)
        if (focused) {
            context.fill(x, y, x + 1, y + h, ClickGuiTheme.searchBarFocus.argb)
            context.fill(x + w - 1, y, x + w, y + h, ClickGuiTheme.searchBarFocus.argb)
        }
        if (query.isEmpty()) {
            val placeholder = "Search modules..."
            context.text(mc.font, placeholder, x + 6, y + (h - 8) / 2, ClickGuiTheme.textDimmed.argb, true)
        } else {
            context.text(mc.font, query, x + 6, y + (h - 8) / 2, ClickGuiTheme.textNormal.argb, true)
            if (focused && caretVisible) {
                val qx = x + 6 + mc.font.width(query)
                context.fill(qx + 1, y + 4, qx + 2, y + h - 4, ClickGuiTheme.textNormal.argb)
            }
        }

        // Caret blink
        if (System.currentTimeMillis() - caretBlink > 500) {
            caretVisible = !caretVisible
            caretBlink = System.currentTimeMillis()
        }

        if (query.isBlank()) return

        val r = results
        if (r.isEmpty()) {
            val noResY = y + h + 2
            val noResW = w
            context.fill(x, noResY, x + noResW, noResY + ClickGuiTheme.searchResultHeight, ClickGuiTheme.searchBarBg.argb)
            context.text(mc.font, "No matches", x + 6, noResY + 3, ClickGuiTheme.textDimmed.argb, true)
            return
        }

        var ry = y + h + 2
        for ((idx, module) in r.withIndex()) {
            if (ry + ClickGuiTheme.searchResultHeight > mc.window.guiScaledHeight - 8) break
            val rowBg = if (idx == selectedIndex) ClickGuiTheme.searchResultSelected.argb else ClickGuiTheme.searchBarBg.argb
            context.fill(x, ry, x + w, ry + ClickGuiTheme.searchResultHeight, rowBg)
            val label = if (idx == selectedIndex && focused) "▸ ${module.name}" else module.name
            context.text(
                mc.font, label, x + 6, ry + 3,
                if (module.enabled) ClickGuiTheme.moduleEnabled.argb else ClickGuiTheme.textNormal.argb,
                true
            )
            ry += ClickGuiTheme.searchResultHeight
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, screenWidth: Int, button: Int): Boolean {
        if (button != 0) return false
        val w = ClickGuiTheme.searchBarWidth
        val h = ClickGuiTheme.searchBarHeight
        val x = (screenWidth - w) / 2
        val y = 8
        if (mouseX in x..(x + w) && mouseY in y..(y + h)) {
            focused = true
            return true
        }
        val r = results
        var ry = y + h + 2
        for ((idx, module) in r.withIndex()) {
            if (mouseX in x..(x + w) && mouseY in ry..(ry + ClickGuiTheme.searchResultHeight)) {
                selectedIndex = idx
                toggleSelected()
                return true
            }
            ry += ClickGuiTheme.searchResultHeight
        }
        focused = false
        return false
    }

    fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!focused) return false
        query += codePoint
        selectedIndex = 0
        return true
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!focused) return false
        return when (keyCode) {
            InputConstants.KEY_ESCAPE -> {
                clear()
                true
            }
            InputConstants.KEY_BACKSPACE -> {
                if (query.isNotEmpty()) query = query.dropLast(1)
                selectedIndex = 0
                true
            }
            InputConstants.KEY_RETURN -> {
                toggleSelected()
                true
            }
            InputConstants.KEY_UP -> {
                moveSelection(-1)
                true
            }
            InputConstants.KEY_DOWN -> {
                moveSelection(1)
                true
            }
            InputConstants.KEY_TAB -> {
                val r = results
                if (selectedIndex in r.indices) onSelect(r[selectedIndex])
                true
            }
            else -> false
        }
    }
}
