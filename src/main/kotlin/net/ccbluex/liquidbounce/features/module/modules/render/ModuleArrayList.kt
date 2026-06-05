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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc

/**
 * Lightweight Java-side ArrayList HUD. Renders the names of all enabled,
 * in-game modules on the right side of the screen, ordered by display width
 * (longest at the bottom by default).
 */
object ModuleArrayList : ClientModule(
    name = "ArrayList",
    category = ModuleCategories.RENDER,
    state = true,
) {

    private val background by boolean("Background", true)
    private val side by enumChoice("Side", Side.RIGHT)
    private val upperCase by boolean("UpperCase", false)
    private val textColor by color("Color", Color4b(255, 255, 255, 255))
    private val backgroundColor by color("BackgroundColor", Color4b(0, 0, 0, 110))
    private val outlineColor by color("OutlineColor", Color4b(0, 0, 0, 200))

    init {
        tree(Colors)
    }

    private object Colors : ValueGroup("Colors") {
        val enableColor by color("EnableColor", Color4b(140, 200, 255, 255))
        val visibleColor by color("VisibleColor", Color4b(255, 255, 255, 255))
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val context = event.context
        val fontRenderer = mc.font

        val modules = ModuleManager
            .filter { it.running && it !== this }
            .sortedByDescending { fontRenderer.width(it.displayName()) }
        if (modules.isEmpty()) return@handler

        val screenWidth = context.guiWidth()
        val screenHeight = context.guiHeight()
        val lineHeight = fontRenderer.lineHeight
        var y = 4
        val margin = 4

        for (module in modules) {
            val name = module.displayName()
            val textWidth = fontRenderer.width(name)
            val lineY = y
            val xRight = screenWidth - textWidth - margin
            val xLeft = margin

            if (background) {
                val bgX1 = if (side == Side.RIGHT) xRight - 3 else xLeft - 3
                val bgX2 = if (side == Side.RIGHT) screenWidth else xLeft + textWidth + 3
                context.fill(bgX1, lineY - 1, bgX2, lineY + lineHeight - 1, backgroundColor.argb)
            }

            context.text(
                fontRenderer, name,
                if (side == Side.RIGHT) xRight else xLeft,
                lineY,
                textColor.argb,
                true,
            )

            y += lineHeight + 1
            if (y > screenHeight - lineHeight) break
        }
    }

    private fun ClientModule.displayName(): String {
        val base = this.name
        return if (upperCase) base.uppercase() else base
    }

    private enum class Side : Tagged {
        LEFT,
        RIGHT;

        override val tag: String get() = name.lowercase()
    }
}
