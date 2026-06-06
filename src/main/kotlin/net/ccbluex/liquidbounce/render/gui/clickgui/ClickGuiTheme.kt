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

/**
 * Design tokens for the Java-side ClickGUI. Colors, spacing, and animation
 * timings mirror the Svelte WebUI CSS variables that ship with the original
 * LiquidBounce-nextgen theme so the Java port feels visually identical.
 */
object ClickGuiTheme {
    val textDimmed = Color4b(170, 170, 170, 255)
    val textNormal = Color4b(255, 255, 255, 255)
    val moduleHoverBg = Color4b(255, 255, 255, 13)
    val moduleEnabled = Color4b(74, 143, 255, 255)
    val moduleHighlight = Color4b(255, 200, 80, 255)
    val settingsBg = Color4b(0, 0, 0, 77)
    val settingsBorder = Color4b(74, 143, 255, 255)
    val panelBg = Color4b(20, 20, 20, 230)
    val panelHeaderBg = Color4b(40, 40, 40, 240)
    val panelHeaderText = Color4b(255, 255, 255, 255)
    val searchBarBg = Color4b(20, 20, 20, 220)
    val searchBarFocus = Color4b(74, 143, 255, 255)
    val searchResultSelected = Color4b(74, 143, 255, 60)
    val descriptionBg = Color4b(20, 20, 20, 245)
    val descriptionText = Color4b(220, 220, 220, 255)
    val descriptionAlias = Color4b(140, 140, 140, 255)
    val separatorLine = Color4b(255, 255, 255, 18)
    val checkboxOff = Color4b(80, 80, 80, 200)
    val checkboxOn = Color4b(74, 143, 255, 255)
    val sliderTrack = Color4b(80, 80, 80, 200)
    val sliderFill = Color4b(74, 143, 255, 255)
    val sliderKnob = Color4b(255, 255, 255, 255)
    val valueTextDimmed = Color4b(170, 170, 170, 255)

    val gridSize: Float = 8f
    val snapEnabled: Boolean = true

    const val panelHeaderHeight: Int = 18
    const val panelMinWidth: Int = 90
    const val panelBorderRadius: Float = 0f
    const val moduleRowHeight: Int = 22
    const val settingsRowHeight: Int = 20
    const val searchBarHeight: Int = 22
    const val searchBarWidth: Int = 240
    const val searchResultHeight: Int = 18
    const val descriptionMaxWidth: Int = 220
    const val expandArrowSize: Float = 6f

    const val fadeMs: Int = 200
    const val slideMs: Int = 500
    const val descriptionFadeMs: Int = 120

    val zIndexBase: Int = 0
}
