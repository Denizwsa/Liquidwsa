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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

class ContentArea(
    private var currentCategory: ModuleCategory,
) {
    var searchQuery: String = ""
    var searchFocused: Boolean = false

    private val moduleCards: MutableMap<ClientModule, ModuleCard> = mutableMapOf()

    var scrollOffset: Int = 0
    private var maxScroll: Int = 0

    private fun getCardsForCategory(): List<ModuleCard> {
        val modules = ModuleManager.filter { it.category == currentCategory }
        return modules.map { module ->
            moduleCards.getOrPut(module) { ModuleCard(module) }
        }
    }

    private fun getFilteredCards(): List<ModuleCard> {
        val cards = getCardsForCategory()
        if (searchQuery.isBlank()) return cards
        val q = searchQuery.lowercase().replace(" ", "")
        return cards.filter { card ->
            q in card.module.name.lowercase() || card.module.aliases.any { q in it.lowercase() }
        }
    }

    fun selectedCategory(category: ModuleCategory) {
        currentCategory = category
        searchQuery = ""
        scrollOffset = 0
    }

    fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int, height: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
    ) {
        with(context) {
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + width).toFloat(), (y + height).toFloat(),
                ClickGuiTheme.cardRadius,
                fillColor = ClickGuiTheme.bgContent,
                outlineColor = ClickGuiTheme.border,
                outlineWidth = 1f,
            )

            val searchY = y + ClickGuiTheme.contentPadding
            val searchX = x + ClickGuiTheme.contentPadding
            val searchW = width - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4
            drawSearchBar(this@with, searchX, searchY, searchW, mouseX, mouseY)

            val listY = searchY + ClickGuiTheme.searchHeight + 8
            val listH = height - (listY - y) - ClickGuiTheme.contentPadding
            val listX = x + ClickGuiTheme.contentPadding
            val listW = width - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4

            enableScissor(listX, listY, listX + listW, listY + listH)

            val cards = getFilteredCards()
            var cardY = listY - scrollOffset

            val totalContent = cards.sumOf { it.fullHeight } + (cards.size - 1) * ClickGuiTheme.cardGap
            maxScroll = (totalContent - listH).coerceAtLeast(0)
            scrollOffset = scrollOffset.coerceIn(0, maxScroll.coerceAtLeast(0))

            for (card in cards) {
                if (cardY + card.fullHeight > listY && cardY < listY + listH) {
                    card.render(this@with, listX, cardY, listW, mouseX, mouseY, partialTick)
                }
                cardY += card.fullHeight + ClickGuiTheme.cardGap
            }

            if (cards.isEmpty()) {
                val emptyText = if (searchQuery.isNotBlank()) "No modules found" else "No modules"
                val tw = mc.font.width(emptyText)
                text(
                    mc.font, emptyText,
                    listX + (listW - tw) / 2, listY + listH / 2 - 4,
                    ClickGuiTheme.textDimmed.argb, true
                )
            }

            disableScissor()

            if (maxScroll > 0) {
                val sbX = x + width - ClickGuiTheme.scrollbarWidth - 4
                val sbTrackH = listH
                val ratio = (listH.toFloat() / totalContent.coerceAtLeast(1)).coerceIn(0.1f, 1f)
                val sbH = (sbTrackH * ratio).toInt().coerceAtLeast(16)
                val sbY = listY + ((sbTrackH - sbH).toFloat() * (scrollOffset.toFloat() / maxScroll.coerceAtLeast(1))).toInt()
                drawRoundedRect(
                    sbX.toFloat(), sbY.toFloat(),
                    (sbX + ClickGuiTheme.scrollbarWidth).toFloat(), (sbY + sbH).toFloat(),
                    (ClickGuiTheme.scrollbarWidth / 2f),
                    fillColor = ClickGuiTheme.scrollbarThumb,
                )
            }
        }
    }

    private fun drawSearchBar(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        with(context) {
            val h = ClickGuiTheme.searchHeight
            val hovered = mouseX in x..(x + width) && mouseY in y..(y + h)

            val borderColor = if (searchFocused) ClickGuiTheme.borderAccent
                else if (hovered) ClickGuiTheme.borderLight
                else ClickGuiTheme.border

            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + width).toFloat(), (y + h).toFloat(),
                6f,
                fillColor = ClickGuiTheme.bgInput,
                outlineColor = borderColor,
                outlineWidth = 1f,
            )

            val textX = x + 8
            val textY = y + (h - 8) / 2

            if (searchQuery.isEmpty() && !searchFocused) {
                text(
                    mc.font, "Search modules...",
                    textX, textY,
                    ClickGuiTheme.textDimmed.argb, true
                )
            } else {
                text(
                    mc.font, searchQuery,
                    textX, textY,
                    ClickGuiTheme.textPrimary.argb, true
                )
            }
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, areaX: Int, areaY: Int, areaW: Int, areaH: Int, button: Int): Boolean {
        val searchY = areaY + ClickGuiTheme.contentPadding
        val searchX = areaX + ClickGuiTheme.contentPadding
        val searchW = areaW - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4
        val searchH = ClickGuiTheme.searchHeight

        if (mouseX in searchX..(searchX + searchW) && mouseY in searchY..(searchY + searchH)) {
            if (button == 0) {
                searchFocused = true
                return true
            }
        } else {
            searchFocused = false
        }

        val listY = searchY + ClickGuiTheme.searchHeight + 8
        val listX = areaX + ClickGuiTheme.contentPadding
        val listW = areaW - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4
        val listH = areaH - (listY - areaY) - ClickGuiTheme.contentPadding

        if (mouseY < listY || mouseY > listY + listH) return false

        val cards = getFilteredCards()
        var cardY = listY - scrollOffset
        for (card in cards) {
            if (mouseY in cardY..(cardY + card.fullHeight)) {
                if (card.handleMouseClick(mouseX, mouseY, listX, cardY, listW, button)) return true
            }
            cardY += card.fullHeight + ClickGuiTheme.cardGap
        }
        return false
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, areaX: Int, areaY: Int, areaW: Int, areaH: Int, button: Int): Boolean {
        val searchY = areaY + ClickGuiTheme.contentPadding
        val listY = searchY + ClickGuiTheme.searchHeight + 8
        val listX = areaX + ClickGuiTheme.contentPadding
        val listW = areaW - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4

        val cards = getFilteredCards()
        var cardY = listY - scrollOffset
        for (card in cards) {
            if (card.handleMouseRelease(mouseX, mouseY, listX, cardY, listW, button)) return true
            cardY += card.fullHeight + ClickGuiTheme.cardGap
        }
        return false
    }

    fun mouseDragged(mouseX: Int, mouseY: Int, areaX: Int, areaY: Int, areaW: Int, areaH: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        val searchY = areaY + ClickGuiTheme.contentPadding
        val listY = searchY + ClickGuiTheme.searchHeight + 8
        val listX = areaX + ClickGuiTheme.contentPadding
        val listW = areaW - ClickGuiTheme.contentPadding * 2 - ClickGuiTheme.scrollbarWidth - 4

        val cards = getFilteredCards()
        var cardY = listY - scrollOffset
        for (card in cards) {
            if (card.handleMouseDrag(mouseX, mouseY, listX, cardY, listW, button, dragX, dragY)) return true
            cardY += card.fullHeight + ClickGuiTheme.cardGap
        }
        return false
    }

    fun mouseScrolled(mouseX: Int, mouseY: Int, scrollY: Double, areaX: Int, areaY: Int, areaW: Int, areaH: Int): Boolean {
        if (mouseX !in areaX..(areaX + areaW)) return false
        if (mouseY !in areaY..(areaY + areaH)) return false
        scrollOffset = (scrollOffset - scrollY.toInt() * 12).coerceIn(0, maxScroll.coerceAtLeast(0))
        return true
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (searchFocused) {
            when (keyCode) {
                259 -> { if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1); return true }
                256 -> { searchFocused = false; searchQuery = ""; return true }
                else -> {}
            }
        }
        val cards = getFilteredCards()
        for (card in cards) {
            if (card.handleKeyPressed(keyCode, scanCode, modifiers)) return true
        }
        return false
    }

    fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (searchFocused) {
            searchQuery += codePoint
            scrollOffset = 0
            return true
        }
        val cards = getFilteredCards()
        for (card in cards) {
            if (card.handleCharTyped(codePoint, modifiers)) return true
        }
        return false
    }

    fun moduleAt(mouseX: Int, mouseY: Int, areaX: Int, areaY: Int): ModuleCard? {
        val searchY = areaY + ClickGuiTheme.contentPadding
        val listY = searchY + ClickGuiTheme.searchHeight + 8
        val listX = areaX + ClickGuiTheme.contentPadding

        val cards = getFilteredCards()
        var cardY = listY - scrollOffset
        for (card in cards) {
            if (mouseX in listX..(listX + areaX + areaX) && mouseY in cardY..(cardY + card.fullHeight)) {
                return card
            }
            cardY += card.fullHeight + ClickGuiTheme.cardGap
        }
        return null
    }
}
