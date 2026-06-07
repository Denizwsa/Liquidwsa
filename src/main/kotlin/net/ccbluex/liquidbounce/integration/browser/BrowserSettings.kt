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
package net.ccbluex.liquidbounce.integration.browser

/**
 * Whether the user wants to share the GPU with Chromium (true) or force
 * software rendering (false). Exposed as a [Value] in the ClickGui module
 * and toggled at runtime via F12.
 */
class BrowserSettings(
    /**
     * Pixel ratio passed to the browser. 0 means "use the OS default".
     */
    var deviceScaleFactor: Float = 0f,
    /**
     * Whether the on-screen browser is currently visible. Hidden browsers
     * stop receiving render ticks so they don't burn CPU when the player
     * is in-game.
     */
    var visible: Boolean = true,
    /**
     * When true the browser is throttled to the Minecraft FPS cap. Useful
     * for low-end machines where running both render loops at uncapped FPS
     * tanks the framerate.
     */
    var syncGameFps: Boolean = false,
)

/**
 * Global settings shared across all browser instances. Held in
 * [BrowserSettingsStore].
 */
object GlobalBrowserSettings {
    @Volatile var accelerated: Boolean? = null
}

/**
 * Holder for runtime browser settings of an individual [Browser] instance.
 * Mirrored from the Svelte side through the interop server.
 */
class IntegrationBrowserSettings(
    var fps: Int = 60,
    val onChange: () -> Unit,
) {
    var currentFps: Int = fps
        set(value) {
            if (field == value) return
            field = value
            onChange()
        }

    var syncGameFps: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            onChange()
        }
}
