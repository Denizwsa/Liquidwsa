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
package net.ccbluex.liquidbounce.integration.screen

/**
 * Logical screen type the integration browser can render. Maps 1:1 to a
 * Svelte route (and thus to a file under `src-theme/src/routes/<name>/`).
 */
enum class CustomScreenType(val routeName: String) {
    CLICK_GUI("clickgui"),
    HUD("hud"),
    INVENTORY("inventory"),
    MENU("menu"),
    NONE("none"),
    BROWSER("browser");

    companion object {
        fun byName(name: String): CustomScreenType? =
            entries.firstOrNull { it.routeName.equals(name, ignoreCase = true) }
    }
}
