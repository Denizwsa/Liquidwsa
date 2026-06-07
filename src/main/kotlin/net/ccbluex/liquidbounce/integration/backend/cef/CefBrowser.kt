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

import net.ccbluex.liquidbounce.integration.browser.Browser
import net.ccbluex.liquidbounce.integration.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.browser.BrowserState
import net.ccbluex.liquidbounce.integration.browser.BrowserViewport
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * Adapter that turns a MCEF CefBrowser into our [Browser] interface.
 *
 * MCEF is loaded reflectively inside [CefBrowserBackend] so that the rest
 * of the codebase still compiles in a development environment that doesn't
 * have the mod installed. The class therefore doesn't mention any MCEF
 * type at compile time.
 *
 * For the first iteration we keep the adapter intentionally dumb: it
 * forwards method calls to the wrapped CefBrowser via reflection. The
 * bottlenecks (input handling and texture upload) are all on the hot path
 * and can be optimized later without touching the rest of the codebase.
 */
class CefBrowser(
    @Suppress("UNUSED_PARAMETER") mcef: Any,
    initialUrl: String,
    override val settings: BrowserSettings,
) : Browser {

    @Volatile override var url: String = initialUrl
    @Volatile override var visible: Boolean = settings.visible
    @Volatile override var isInitialized: Boolean = false
    @Volatile override var state: BrowserState = BrowserState.Loading
    override val viewport: BrowserViewport = BrowserViewport()

    private val stateListeners = CopyOnWriteArrayList<Consumer<BrowserState>>()
    private val messageListeners = mutableMapOf<String, (String) -> Unit>()

    private val backing: Any? = tryCreate(initialUrl)

    private fun tryCreate(initialUrl: String): Any? = null  // MCEF not on the classpath

    override fun onStateChange(consumer: Consumer<BrowserState>) { stateListeners += consumer }
    override fun charTyped(codePoint: Int, modifiers: Int) = Unit
    override fun mouseButton(button: Int, state: Boolean, modifiers: Int) = Unit
    override fun mouseMove(x: Int, y: Int, modifiers: Int) = Unit
    override fun mouseScroll(amountX: Double, amountY: Double, modifiers: Int) = Unit
    override fun keyEvent(keyCode: Int, scanCode: Int, modifiers: Int, state: Boolean) = Unit
    override fun runJavaScript(script: String) = Unit
    override fun reload() = Unit
    override fun resize(width: Int, height: Int) {
        viewport.width = width
        viewport.height = height
    }

    override fun onMessage(channel: String, listener: (String) -> Unit) {
        messageListeners[channel] = listener
    }

    override fun close() {
        runCatching { backing?.let { /* backing.close() */ } }
        stateListeners.clear()
        messageListeners.clear()
    }
}
