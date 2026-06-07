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
package net.ccbluex.liquidbounce.render.gui.clickgui.setting

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.render.gui.clickgui.spacedName
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Base class for all ClickGUI value widgets.
 *
 * Each subclass owns a [Value] and renders a row of fixed [height] inside
 * the parent module's settings panel. The widget is responsible for handling
 * its own mouse / keyboard input and persisting state back to the underlying
 * value via `Value.set(...)`.
 */
abstract class GenericSetting {
    abstract val value: Value<*>
    open val height: Int = 20

    /** Width of the text-label column on the left side of the row. */
    open val labelWidth: Int = 90

    /**
     * Draws the widget row at `(x, y)` with the given [width]. Returns the
     * pixel height actually consumed (usually [height], but some widgets
     * like color pickers may use more).
     */
    abstract fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int

    open fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean = false
    open fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean = false
    open fun mouseDragged(mouseX: Int, mouseY: Int, button: Int, dragX: Double, dragY: Double): Boolean = false
    open fun mouseScrolled(mouseX: Int, mouseY: Int, scrollX: Double, scrollY: Double): Boolean = false
    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    open fun charTyped(codePoint: Char, modifiers: Int): Boolean = false

    open fun onFocusLost() {}

    /**
     * Display name derived from the value's internal name by splitting
     * camelCase into words. Never shows raw translation keys.
     */
    open val displayName: String
        get() = spacedName(value.name)
}
