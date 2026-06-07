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
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton that owns the active [Theme]. We support exactly one theme
 * ("liquidbounce") which is bundled in the JAR at
 * `assets/liquidbounce/themes/liquidbounce/`. If the user drops a custom
 * theme into the themes directory it can be selected by setting the
 * `liquidbounce.theme.name` system property, but for the core scope the
 * default theme is always good enough.
 *
 * The [fallback] theme is a tiny stub that ships URLs pointing at
 * `about:blank` so the browser still has something to load if the bundle
 * is missing for any reason (e.g. the player extracted the JAR by hand
 * and forgot the assets folder).
 */
object ThemeManager {

    private val activeRef = AtomicReference<Theme?>(null)

    val fallback: Theme by lazy {
        Theme(
            ThemeMetadata(
                name = "fallback",
                author = "system",
                version = "0.0.0",
                entryPoint = mapOf(
                    CustomScreenType.CLICK_GUI.routeName to "/clickgui/index.html",
                    CustomScreenType.HUD.routeName to "/hud/index.html",
                ),
            )
        )
    }

    /** Currently active theme, or the fallback if none has been loaded. */
    val theme: Theme? get() = activeRef.get() ?: loadDefault()

    fun setActive(theme: Theme) { activeRef.set(theme) }

    private fun loadDefault(): Theme {
        val t = Theme(
            ThemeMetadata(
                name = "liquidbounce",
                author = "CCBlueX",
                version = "1.0.0",
            )
        )
        activeRef.set(t)
        return t
    }
}
