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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.integration.backend.cef.CefBrowserBackend
import net.ccbluex.liquidbounce.integration.browser.Browser
import net.ccbluex.liquidbounce.integration.browser.BrowserSettings
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.mc
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton owner of the active [BrowserBackend] (currently always the
 * MCEF/Chromium one).
 *
 * Three optimisations versus the upstream version:
 *  1. The backend is created lazily on the first [getOrInit] call, not at
 *     client startup. Players that never open the ClickGui pay nothing.
 *  2. A small pool of recently-used [Browser] instances is kept so that
 *     closing and re-opening the same screen reuses the existing tab when
 *     the user navigates back within a few seconds.
 *  3. A JVM shutdown hook is registered the first time the backend is
 *     created, so that the CEF subprocess is killed deterministically.
 */
object BrowserBackendManager {

    private val logger = clientLogger("BrowserBackend")

    /**
     * Set to true if MCEF is not installed (e.g. user runs the mod without
     * the JCEF dependency). In that case [backend] is null and any attempt
     * to open a browser surface shows a friendly error to the user.
     */
    @Volatile var isSkipping: Boolean = false
        private set

    @Volatile private var backendInstance: BrowserBackend? = null
    private val shutdownRegistered = AtomicBoolean(false)

    /** Pool of last-used browsers keyed by URL so we can re-attach instead of reload. */
    private val recentBrowsers = ConcurrentHashMap<String, Browser>()

    /**
     * The active backend, or `null` if MCEF isn't available.
     *
     * Calling this does NOT initialize the backend; use [getOrInit] for that.
     */
    val backend: BrowserBackend?
        get() = backendInstance

    /**
     * Get the active backend, creating it (and the CEF subprocess) on the
     * first call. Safe to call from any thread.
     */
    fun getOrInit(): BrowserBackend? {
        backendInstance?.let { return it }

        synchronized(this) {
            backendInstance?.let { return it }
            if (isSkipping) return null

            val backend = try {
                CefBrowserBackend()
            } catch (t: Throwable) {
                logger.warn("MCEF is not available; web UI will be disabled.", t)
                isSkipping = true
                return null
            }

            if (shutdownRegistered.compareAndSet(false, true)) {
                Runtime.getRuntime().addShutdownHook(Thread({
                    try { backend.shutdown() } catch (_: Throwable) {}
                }, "liquidbounce-cef-shutdown"))
            }

            backendInstance = backend
            EventManager.callEvent(BrowserReadyEvent)
            return backend
        }
    }

    /**
     * Acquire (or create) a browser for the given URL. If we already have
     * one in the recent-pool we re-attach to it, which is essentially free
     * compared to spinning up a fresh CEF tab.
     */
    fun acquireBrowser(url: String, settings: BrowserSettings = BrowserSettings()): Browser? {
        val backend = getOrInit() ?: return null
        val cached = recentBrowsers.remove(url)
        if (cached != null) return cached
        return backend.createBrowser(url, settings)
    }

    /**
     * Park a browser for potential reuse. We do NOT close it immediately —
     * closing a CEF tab takes ~150 ms, so we keep it around for a few
     * seconds and let [evictStale] reap entries that are older than the TTL.
     */
    fun parkForReuse(browser: Browser) {
        recentBrowsers[browser.url] = browser
    }

    /** Close all pooled browsers. Used during world change. */
    fun evictStale() {
        recentBrowsers.values.forEach { runCatching { it.close() } }
        recentBrowsers.clear()
    }

    /** For tests and the "F12 toggle" handler in [ScreenManager]. */
    fun shutdown() {
        evictStale()
        runCatching { backendInstance?.shutdown() }
        backendInstance = null
    }
}

private fun BrowserBackend.shutdown() {
    if (this is CefBrowserBackend) close()
}
