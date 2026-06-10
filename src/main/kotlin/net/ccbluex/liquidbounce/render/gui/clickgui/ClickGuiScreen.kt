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
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class ClickGuiScreen : Screen(Component.literal("ClickGui")) {

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {}

    private val firstCategory: ModuleCategory = ModuleCategories.entries.first()
    private val sidebar = Sidebar(firstCategory) { category ->
        contentArea.selectedCategory(category)
    }
    private val contentArea = ContentArea(firstCategory)
    private val description = DescriptionOverlay()

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight

        context.fill(0, 0, sw, sh, 0x60080810.toInt())

        val margin = 12
        val topBarHeight = 0
        val sidebarX = margin
        val sidebarY = margin + topBarHeight
        val sidebarH = sh - margin * 2 - topBarHeight

        sidebar.render(context, sidebarX, sidebarY, sidebarH, mouseX, mouseY)

        val contentX = sidebarX + sidebar.width + 8
        val contentY = sidebarY
        val contentW = sw - contentX - margin
        val contentH = sidebarH

        contentArea.render(context, contentX, contentY, contentW, contentH, mouseX, mouseY, partialTick)

        val module = contentArea.moduleAt(mouseX, mouseY, contentX, contentY)
        if (module != null) {
            val cardX = contentX + ClickGuiTheme.contentPadding
            val cardW = contentW - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4
            description.show(
                module.description(),
                module.aliases(),
                (cardX + cardW / 2).toFloat(),
                mouseY.toFloat(),
                (cardX + cardW).toFloat(),
                sw.toFloat(),
            )
        } else {
            description.hide()
        }
        description.render(context, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        val margin = 12
        val sidebarX = margin
        val sidebarY = margin
        val sidebarH = sh - margin * 2

        if (sidebar.mouseClicked(mx, my, sidebarX, sidebarY, event.button())) return true

        val contentX = sidebarX + ClickGuiTheme.sidebarWidth + 8
        val contentY = sidebarY
        val contentW = sw - contentX - margin
        val contentH = sidebarH

        if (contentArea.mouseClicked(mx, my, contentX, contentY, contentW, contentH, event.button())) return true
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        val margin = 12
        val contentX = margin + ClickGuiTheme.sidebarWidth + 8
        val contentY = margin
        val contentW = sw - contentX - margin
        val contentH = sh - margin * 2

        if (contentArea.mouseReleased(mx, my, contentX, contentY, contentW, contentH, event.button())) return true
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        val margin = 12
        val contentX = margin + ClickGuiTheme.sidebarWidth + 8
        val contentY = margin
        val contentW = sw - contentX - margin
        val contentH = sh - margin * 2

        if (contentArea.mouseDragged(mx, my, contentX, contentY, contentW, contentH, event.button(), dragX, dragY)) return true
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        val margin = 12
        val contentX = margin + ClickGuiTheme.sidebarWidth + 8
        val contentY = margin
        val contentW = sw - contentX - margin
        val contentH = sh - margin * 2

        if (contentArea.mouseScrolled(mx, my, scrollY, contentX, contentY, contentW, contentH)) return true
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.key()
        val scanCode = event.scancode()
        val modifiers = event.modifiers()

        if (contentArea.keyPressed(keyCode, scanCode, modifiers)) return true

        when (keyCode) {
            GLFW.GLFW_KEY_ESCAPE -> {
                mc.setScreen(null)
                return true
            }
            InputConstants.KEY_TAB -> return true
        }
        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        return super.keyReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val codePoint = event.codepoint().toChar()
        val modifiers = 0

        if (contentArea.charTyped(codePoint, modifiers)) return true
        return super.charTyped(event)
    }

    override fun isPauseScreen(): Boolean = false
}
