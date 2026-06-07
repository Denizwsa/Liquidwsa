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
 * Represents a single Svelte theme (a directory of `*.svelte` files
 * compiled into a single-page app). The theme is loaded from a resource
 * zip on the classpath at startup; the actual [Browser] instance is what
 * ends up loading the entry-point HTML.
 *
 * The class is intentionally minimal — the upstream theme system had a
 * background-fading / cosmetic system that we don't need for the
 * "ClickGui-only" scope. We just need enough metadata to know which URL
 * each screen should navigate to.
 */
class Theme(val metadata: ThemeMetadata) {

    /**
     * Whether the theme is willing to fully replace the Minecraft screen
     * for [route]. Themes that only support overlays return false here.
     */
    fun isScreenSupported(route: String): Boolean = true

    /** Whether the theme can paint on top of the existing screen. */
    fun isOverlaySupported(route: String): Boolean = true

    /**
     * Where to send the browser for the given virtual screen type. Most
     * themes just hash-route to a Svelte page; some prefer a literal URL.
     */
    fun entryPoint(type: CustomScreenType): String = metadata.entryPoint(type)
}
