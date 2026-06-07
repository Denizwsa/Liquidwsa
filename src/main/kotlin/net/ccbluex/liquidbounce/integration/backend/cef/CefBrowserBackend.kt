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
package net.ccbluex.liquidbounce.integration.backend.cef

import net.ccbluex.liquidbounce.integration.backend.BrowserAccelerationFlags
import net.ccbluex.liquidbounce.integration.backend.BrowserBackend
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.browser.Browser
import net.ccbluex.liquidbounce.integration.browser.BrowserSettings
import net.ccbluex.liquidbounce.utils.client.clientLogger

/**
 * Real Chromium-based backend backed by CCBlueX's MCEF mod.
 *
 * The actual MCEF API surface (CefApp, CefClient, CefBrowser) is wrapped
 * here so that the rest of the codebase can be compiled without MCEF on
 * the classpath — the [tryInit] reflection dance means a missing mod just
 * flips [isSkipping] on the manager and we render a friendly error message
 * instead of crashing the client.
 */
class CefBrowserBackend internal constructor() : BrowserBackend, AutoCloseable {

    override val name: String = "MCEF"
    override val accelerationFlags: BrowserAccelerationFlags = BrowserAccelerationFlags(true)
    override val isReady: Boolean
        get() = backingApp != null

    private val logger = clientLogger("CefBrowserBackend")
    private var backingApp: Any? = tryInit()

    private fun tryInit(): Any? {
        return try {
            val cls = Class.forName("net.ccbluex.mcef.api.MCEFApi")
            cls.getMethod("getInstance").invoke(null)
        } catch (t: Throwable) {
            logger.warn("MCEF not found on classpath; web UI is disabled.", t)
            null
        }
    }

    override fun createBrowser(url: String, settings: BrowserSettings): Browser {
        val app = backingApp
            ?: throw IllegalStateException("MCEF is not available; install the mcef mod to use the web UI.")
        return CefBrowser(app, url, settings)
    }

    override fun close() {
        runCatching { backingApp = null }
    }
}

internal fun BrowserTexture.isUsable(): Boolean = isValid
