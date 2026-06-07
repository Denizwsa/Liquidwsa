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
package net.ccbluex.liquidbounce.integration.backend

import net.ccbluex.liquidbounce.integration.browser.Browser
import net.ccbluex.liquidbounce.integration.browser.BrowserSettings

/**
 * Abstraction over the underlying browser engine (currently only MCEF/Chromium
 * is supported, but the interface makes it easy to add an external-system
 * fallback in the future).
 *
 * Implementations are responsible for spawning the engine subprocess, creating
 * individual [Browser] instances, and for shutting the engine down when no
 * browsers are alive any more.
 */
interface BrowserBackend {

    val accelerationFlags: BrowserAccelerationFlags

    /**
     * The browser engine's human-readable name. Surfaced in logs and in the
     * integration menu so users can verify which backend is actually loaded.
     */
    val name: String

    /**
     * @return true if the backend has finished initializing and is ready to
     * create browsers. The check is non-blocking; the backend is expected to
     * start lazily on the first call to [createBrowser].
     */
    val isReady: Boolean

    /**
     * Create a new browser instance pointing at [url]. Implementations are
     * expected to be cheap to call multiple times — the underlying engine
     * process is shared.
     */
    fun createBrowser(url: String, settings: BrowserSettings): Browser
}
