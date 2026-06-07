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
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Java-side port of the Svelte WebUI ClickGUI that ships with the original
 * LiquidBounce-nextgen theme. Composes the top-center [SearchBar], a
 * draggable [Panel] per [ModuleCategory], and the floating
 * [DescriptionOverlay] into a single screen, owns the input dispatch, and
 * persists panel layout to [ClickGuiConfig].
 */
class ClickGuiScreen : Screen(Component.literal("ClickGui")) {
    // Override extractBackground to prevent the default 1.21+ blur + dim overlay
    // (which causes VulkanMod shader conflicts and FPS drops). Solid gradient fill
    // is done in extractRenderState instead.
    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        // no-op
    }

    private val panels: MutableList<Panel> = ModuleManager
        .map { it.category }
        .toSortedSet(compareBy { it.tag })
        .map(::Panel)
        .toMutableList()
    private val searchBar = SearchBar { module -> highlightAndExpand(module) }
    private val description = DescriptionOverlay()
    private var highlighted: ModuleElement? = null

    init {
        // Default panel grid: 3 columns x N rows
        val cols = 3
        val startX = 24
        val startY = 56
        val gapX = 124
        val gapY = 18
        for ((idx, panel) in panels.withIndex()) {
            if (panel.x == 0 && panel.y == 0) {
                val col = idx % cols
                val row = idx / cols
                panel.x = startX + col * gapX
                panel.y = startY + row * gapY
            }
        }
    }

    override fun init() {
        for (p in panels) {
            if (p.x < 0 || p.y < 0) {
                p.x = 24
                p.y = 56
            }
        }
    }

    override fun onClose() {
        ClickGuiConfig.flush()
        for (p in panels) p.saveConfig()
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Solid semi-transparent background (Vulkan-safe single fill, no shader)
        context.fill(0, 0, mc.window.guiScaledWidth, mc.window.guiScaledHeight, 0x40080808.toInt())

        val sortedPanels = panels.sortedBy { it.zIndex }
        for (p in sortedPanels) {
            p.render(context, mouseX, mouseY, partialTick, highlighted)
        }
        searchBar.render(context, mc.window.guiScaledWidth, mouseX, mouseY, partialTick)
        description.render(context, partialTick)

        val module = panels.firstNotNullOfOrNull { it.moduleAt(mouseX, mouseY) }
        if (module != null) {
            val panel = panels.firstOrNull { it.moduleAt(mouseX, mouseY) != null }
            val rowRight = (panel?.let { (it.x + it.width).toFloat() }) ?: mouseX.toFloat()
            description.show(
                module.description(),
                module.aliases(),
                mouseX.toFloat(),
                (ClickGuiTheme.moduleRowHeight).toFloat(),
                rowRight,
                mc.window.guiScaledWidth.toFloat()
            )
        } else {
            description.hide()
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        if (searchBar.mouseClicked(mx, my, mc.window.guiScaledWidth, event.button())) return true
        for (p in panels.sortedByDescending { it.zIndex }) {
            if (p.mouseClicked(mx, my, event.button())) return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        for (p in panels.sortedByDescending { it.zIndex }) {
            if (p.mouseReleased(mx, my, event.button())) return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        for (p in panels.sortedByDescending { it.zIndex }) {
            if (p.mouseDragged(mx, my, event.button(), dragX, dragY)) return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        for (p in panels.sortedByDescending { it.zIndex }) {
            if (p.mouseScrolled(mx, my, scrollX, scrollY)) return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.key()
        val scanCode = event.scancode()
        val modifiers = event.modifiers()
        if (searchBar.keyPressed(keyCode, scanCode, modifiers)) return true
        when (keyCode) {
            GLFW.GLFW_KEY_ESCAPE -> {
                mc.setScreen(null)
                return true
            }
            InputConstants.KEY_TAB -> {
                return true
            }
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> {
                for (p in panels) p.onKeyShift(true)
            }
        }
        for (p in panels.sortedByDescending { it.zIndex }) {
            if (p.keyPressed(keyCode, scanCode, modifiers)) return true
        }
        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        val keyCode = event.key()
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            for (p in panels) p.onKeyShift(false)
        }
        return super.keyReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val codePoint = event.codepoint().toChar()
        val modifiers = 0
        if (searchBar.charTyped(codePoint, modifiers)) return true
        for (p in panels.sortedByDescending { it.zIndex }) {
            if (p.charTyped(codePoint, modifiers)) return true
        }
        return super.charTyped(event)
    }

    override fun isPauseScreen(): Boolean = false

    private fun highlightAndExpand(module: net.ccbluex.liquidbounce.features.module.ClientModule) {
        val panel = panels.firstOrNull { p -> p.category == module.category } ?: return
        panel.expanded = true
        val element = panel.moduleElements().firstOrNull { it.module == module } ?: return
        element.expanded = true
        highlighted = element
        panel.bringToFront()
    }
}
