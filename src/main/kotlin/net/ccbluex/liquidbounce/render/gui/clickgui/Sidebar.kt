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

import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

class Sidebar(
    private var selectedCategory: ModuleCategory,
    private val onCategorySelected: (ModuleCategory) -> Unit,
) {
    private val categories: List<ModuleCategory> = ModuleCategories.entries.toList()

    fun selected(): ModuleCategory = selectedCategory

    val width: Int
        get() = ClickGuiTheme.sidebarWidth

    fun render(context: GuiGraphicsExtractor, x: Int, y: Int, height: Int, mouseX: Int, mouseY: Int) {
        with(context) {
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + width).toFloat(), (y + height).toFloat(),
                ClickGuiTheme.sidebarRadius,
                fillColor = ClickGuiTheme.sidebarBg,
            )

            var itemY = y + 8
            for (category in categories) {
                val isSelected = category == selectedCategory
                val hovered = mouseX in x..(x + width) && mouseY in itemY..(itemY + ClickGuiTheme.sidebarItemHeight)

                if (isSelected) {
                    drawRoundedRect(
                        (x + 4).toFloat(), itemY.toFloat(),
                        (x + width - 4).toFloat(), (itemY + ClickGuiTheme.sidebarItemHeight).toFloat(),
                        6f,
                        fillColor = ClickGuiTheme.sidebarItemActive,
                    )
                    fill(
                        x + 2, itemY + 6,
                        x + 4, itemY + ClickGuiTheme.sidebarItemHeight - 6,
                        ClickGuiTheme.sidebarIndicator.argb
                    )
                } else if (hovered) {
                    drawRoundedRect(
                        (x + 4).toFloat(), itemY.toFloat(),
                        (x + width - 4).toFloat(), (itemY + ClickGuiTheme.sidebarItemHeight).toFloat(),
                        6f,
                        fillColor = ClickGuiTheme.sidebarItemHover,
                    )
                }

                val textX = x + 14
                val textY = itemY + (ClickGuiTheme.sidebarItemHeight - 8) / 2
                text(
                    mc.font, category.tag,
                    textX, textY,
                    if (isSelected) ClickGuiTheme.sidebarTextActive.argb else ClickGuiTheme.sidebarText.argb,
                    true
                )

                itemY += ClickGuiTheme.sidebarItemHeight
            }
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, sidebarX: Int, sidebarY: Int, button: Int): Boolean {
        if (button != 0) return false
        if (mouseX !in sidebarX..(sidebarX + width)) return false
        var itemY = sidebarY + 8
        for (category in categories) {
            if (mouseY in itemY..(itemY + ClickGuiTheme.sidebarItemHeight)) {
                if (category != selectedCategory) {
                    selectedCategory = category
                    onCategorySelected(category)
                }
                return true
            }
            itemY += ClickGuiTheme.sidebarItemHeight
        }
        return false
    }

    fun selectCategory(category: ModuleCategory) {
        selectedCategory = category
    }
}
