/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package net.ccbluex.liquidbounce.integration.theme

import net.ccbluex.liquidbounce.integration.screen.CustomScreenType

/**
 * Lightweight metadata block parsed from a theme's `theme.json` file. The
 * `entryPoint` map lets the theme override the URL each virtual screen
 * type navigates to — most themes keep the defaults but some (e.g. a
 * "no-clickgui" theme) might want the ClickGui to redirect to a placeholder.
 */
data class ThemeMetadata(
    val name: String,
    val author: String = "anonymous",
    val version: String = "0.0.0",
    val entryPoint: Map<String, String> = emptyMap(),
) {
    fun entryPoint(type: CustomScreenType): String =
        entryPoint[type.routeName] ?: "/#/${type.routeName}"
}
