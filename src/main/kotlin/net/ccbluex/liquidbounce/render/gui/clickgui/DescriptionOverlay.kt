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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Floating tooltip that shows a module's description and aliases when the
 * user hovers a module row. Anchors itself to whichever side of the row
 * has the most free space, mirroring the Svelte WebUI behavior.
 */
class DescriptionOverlay {

    private var pending: Entry? = null
    private var current: Entry? = null
    private var currentMs: Long = 0L
    private var alpha: Float = 0f

    data class Entry(
        val description: String,
        val aliases: List<String>,
        val anchorX: Float,
        val anchorY: Float,
        val anchorRight: Boolean,
    )

    fun show(
        description: String,
        aliases: List<String>,
        rowCenterX: Float,
        rowCenterY: Float,
        rowRight: Float,
        screenWidth: Float,
    ) {
        val anchorRight = (screenWidth - rowRight) > ClickGuiTheme.descriptionMaxWidth
        pending = Entry(
            description = description,
            aliases = aliases,
            anchorX = if (anchorRight) rowRight + 6f else rowCenterX - 6f,
            anchorY = rowCenterY,
            anchorRight = anchorRight,
        )
    }

    fun hide() {
        pending = null
    }

    fun render(context: GuiGraphicsExtractor, partialTick: Float) {
        if (pending != current) {
            current = pending
            currentMs = System.currentTimeMillis()
        }
        val entry = current ?: run { alpha = 0f; return }
        val now = System.currentTimeMillis()
        val elapsed = now - currentMs
        val target = if (pending == null) 0f else 1f
        val speed = 1f / ClickGuiTheme.descriptionFadeMs.coerceAtLeast(1)
        alpha = (alpha + (target - alpha) * speed * (now - lastRender).coerceAtLeast(1)).coerceIn(0f, 1f)
        lastRender = now
        if (alpha <= 0.01f) return

        val lines = buildList {
            add(entry.description)
            if (entry.aliases.isNotEmpty()) {
                add("aka " + entry.aliases.joinToString(", "))
            }
        }
        val maxW = lines.maxOf { mc.font.width(it) }
        val boxW = (maxW + 12).coerceAtMost(ClickGuiTheme.descriptionMaxWidth)
        val boxH = lines.size * (mc.font.lineHeight + 2) + 8
        val boxX = if (entry.anchorRight) entry.anchorX.toInt() else (entry.anchorX - boxW).toInt()
        val boxY = (entry.anchorY - boxH / 2f).toInt()

        val a = (alpha * 245).toInt()
        val bg = Color4b(ClickGuiTheme.descriptionBg.r, ClickGuiTheme.descriptionBg.g, ClickGuiTheme.descriptionBg.b, a)
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, bg.argb)
        var y = boxY + 4
        for ((i, line) in lines.withIndex()) {
            val color = if (i == 0)
                Color4b(ClickGuiTheme.descriptionText.r, ClickGuiTheme.descriptionText.g, ClickGuiTheme.descriptionText.b, a)
            else
                Color4b(ClickGuiTheme.descriptionAlias.r, ClickGuiTheme.descriptionAlias.g, ClickGuiTheme.descriptionAlias.b, a)
            context.text(mc.font, line, boxX + 6, y, color.argb, true)
            y += mc.font.lineHeight + 2
        }
    }

    private var lastRender: Long = 0L
}
