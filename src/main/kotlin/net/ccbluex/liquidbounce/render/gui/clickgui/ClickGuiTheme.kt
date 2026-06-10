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

object ClickGuiTheme {
    val accent = Color4b(74, 143, 255, 255)
    val accentDim = Color4b(74, 143, 255, 80)
    val accentGlow = Color4b(74, 143, 255, 30)

    val bgPrimary = Color4b(12, 12, 16, 240)
    val bgSecondary = Color4b(18, 18, 24, 230)
    val bgContent = Color4b(14, 14, 20, 220)
    val bgCard = Color4b(24, 24, 32, 200)
    val bgCardHover = Color4b(34, 34, 44, 200)
    val bgCardEnabled = Color4b(74, 143, 255, 25)
    val bgInput = Color4b(30, 30, 40, 200)

    val textPrimary = Color4b(235, 235, 245, 255)
    val textSecondary = Color4b(160, 160, 175, 255)
    val textDimmed = Color4b(110, 110, 125, 255)

    val border = Color4b(40, 40, 50, 200)
    val borderLight = Color4b(55, 55, 65, 200)
    val borderAccent = Color4b(74, 143, 255, 180)

    val toggleBg = Color4b(50, 50, 60, 200)
    val toggleEnabled = Color4b(74, 143, 255, 255)
    val toggleKnob = Color4b(255, 255, 255, 255)
    val toggleKnobOff = Color4b(180, 180, 190, 255)

    val sliderBg = Color4b(45, 45, 55, 200)
    val sliderFill = Color4b(74, 143, 255, 255)
    val sliderKnob = Color4b(255, 255, 255, 255)

    val sidebarBg = Color4b(16, 16, 22, 240)
    val sidebarItemBg = Color4b(255, 255, 255, 6)
    val sidebarItemHover = Color4b(255, 255, 255, 12)
    val sidebarItemActive = Color4b(74, 143, 255, 25)
    val sidebarIndicator = Color4b(74, 143, 255, 255)
    val sidebarText = Color4b(160, 160, 175, 255)
    val sidebarTextActive = Color4b(235, 235, 245, 255)

    val descriptionBg = Color4b(20, 20, 28, 245)
    val descriptionText = Color4b(210, 210, 220, 255)
    val descriptionAlias = Color4b(140, 140, 155, 255)

    val scrollbarBg = Color4b(255, 255, 255, 10)
    val scrollbarThumb = Color4b(74, 143, 255, 150)

    val notificationBg = Color4b(20, 20, 28, 220)
    val notificationBorder = Color4b(50, 50, 64, 200)

    const val sidebarWidth = 100
    const val sidebarItemHeight = 38
    const val sidebarRadius = 8f
    const val contentPadding = 12
    const val cardRadius = 8f
    const val cardGap = 6
    const val cardHeight = 36
    const val searchHeight = 32
    const val toggleWidth = 36
    const val toggleHeight = 16
    const val sliderHeight = 4
    const val sliderKnobSize = 8
    const val scrollbarWidth = 3

    const val animSlideMs = 300
    const val animFadeMs = 150
}
