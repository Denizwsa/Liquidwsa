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

package net.ccbluex.liquidbounce.integration.theme.component.components.native

import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * Shared color palette for the native (non-browser) HUD components, mirroring the default web theme
 * (`src-theme/src/colors.scss`). Kept in one place so all native components stay visually consistent.
 */
internal object NativeHudPalette {
    /** Accent blue `#4677ff`. */
    val ACCENT = Color4b(0x46, 0x77, 0xFF)

    /** White text `#ffffff`. */
    val TEXT = Color4b.WHITE

    /** Dimmed text `#d3d3d3`. */
    val DIMMED = Color4b(0xD3, 0xD3, 0xD3)

    /** Error/level red `#fc4130`. */
    val ERROR = Color4b(0xFC, 0x41, 0x30)

    /** Tag gray `#aaaaaa`. */
    val TAG = Color4b(0xAA, 0xAA, 0xAA)

    /** `color-mix(#000 68%, transparent)` background. */
    val BG_68 = Color4b(0, 0, 0, 174)

    /** `color-mix(#000 50%, transparent)` background. */
    val BG_50 = Color4b(0, 0, 0, 128)

    /** `color-mix(#000 30%, transparent)` (inactive armor point). */
    val BG_30 = Color4b(0, 0, 0, 77)
}
