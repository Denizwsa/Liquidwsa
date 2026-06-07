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

import net.ccbluex.liquidbounce.integration.browser.Browser
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager

/**
 * Represents a single virtual screen (ClickGui, Hud, …) inside the
 * integration browser. The actual rendering and event handling happens
 * inside the browser, but this class keeps the metadata that the
 * surrounding `Screen` wrapper needs.
 */
class CustomScreen(
    val type: CustomScreenType,
    val theme: Theme = ThemeManager.theme ?: ThemeManager.fallback,
) {
    /** Convenience accessor; the owning screen grabs the browser through [BrowserHolder]. */
    val browser: Browser?
        get() = BrowserHolder.current

    /** Map a virtual screen type to a sub-route the Svelte side knows about. */
    fun route(): String = type.routeName
}

/**
 * Thread-local pointer to the browser the active screen is currently driving.
 * Set/cleared by `CustomSharedMinecraftScreen` so that other code can
 * invoke JS / send messages without having to walk the screen tree.
 */
object BrowserHolder {
    @Volatile var current: Browser? = null
}
