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
package net.ccbluex.liquidbounce.render.gui.clickgui.theme

import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * Centralized color and layout tokens for the modern ClickGUI.
 *
 * Inspired by the legacy LiquidBounce look, but with a darker palette,
 * soft accent blue, and tighter spacing.
 */
object ClickGuiTheme {

    // --- Colors: backgrounds ---
    val panelBg = Color4b(18, 18, 24, 240)
    val sidebarBg = Color4b(14, 14, 20, 245)
    val headerBg = Color4b(22, 22, 30, 250)
    val tabBarBg = Color4b(18, 18, 24, 230)
    val rowIdle = Color4b(28, 28, 38, 180)
    val rowHover = Color4b(40, 40, 56, 220)
    val rowSelected = Color4b(48, 78, 156, 220)
    val groupHeader = Color4b(34, 34, 44, 220)
    val groupHeaderHover = Color4b(48, 48, 64, 240)

    // --- Colors: text ---
    val textPrimary = Color4b(238, 238, 245)
    val textSecondary = Color4b(160, 160, 178)
    val textAccent = Color4b(120, 175, 255)
    val textMuted = Color4b(110, 110, 128)
    val textOnAccent = Color4b(255, 255, 255)
    val textEnabled = Color4b(140, 240, 170)
    val textDisabled = Color4b(150, 150, 160)

    // --- Colors: accent + accent variants ---
    val accent = Color4b(74, 143, 255, 255)
    val accentHover = Color4b(96, 160, 255, 255)
    val accentMuted = Color4b(74, 143, 255, 140)
    val accentGlow = Color4b(74, 143, 255, 90)
    val accentDisabled = Color4b(60, 60, 75, 255)

    // --- Colors: widget parts ---
    val switchTrackOff = Color4b(50, 50, 62, 255)
    val switchTrackOn = Color4b(74, 143, 255, 255)
    val switchThumb = Color4b(255, 255, 255, 255)
    val sliderTrack = Color4b(50, 50, 62, 255)
    val sliderFill = Color4b(74, 143, 255, 255)
    val sliderThumb = Color4b(255, 255, 255, 255)
    val separator = Color4b(60, 60, 75, 140)
    val border = Color4b(50, 50, 64, 180)
    val shadow = Color4b(0, 0, 0, 100)

    // --- Colors: search bar ---
    val searchBg = Color4b(28, 28, 38, 200)
    val searchBgFocused = Color4b(40, 40, 56, 240)
    val searchPlaceholder = Color4b(120, 120, 138)

    // --- Spacing ---
    const val paddingSmall = 4
    const val paddingMedium = 8
    const val paddingLarge = 12

    // --- Layout ---
    const val headerHeight = 32
    const val tabBarHeight = 26
    const val statusBarHeight = 18
    const val rowHeight = 22
    const val groupHeaderHeight = 24
    const val indentWidth = 10
    const val panelRadius = 6f
    const val buttonRadius = 4f

    // --- Animation timing (ms) ---
    const val hoverAnimMs = 150L
    const val toggleAnimMs = 200L
    const val expandAnimMs = 250L
    const val underlineAnimMs = 200L
    const val searchPulseMs = 1500L
    const val notificationSlideMs = 300L

    // --- Misc ---
    const val tabMinWidth = 60
    const val tabPaddingX = 14
}
