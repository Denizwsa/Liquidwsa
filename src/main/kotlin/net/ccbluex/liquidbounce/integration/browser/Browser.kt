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

import net.ccbluex.liquidbounce.integration.input.InputListener
import java.util.function.Consumer

/**
 * Logical handle to a single browser tab owned by a [BrowserBackend].
 *
 * The actual rendering is delegated to a [BrowserTexture] (which wraps a
 * Minecraft `RenderTarget`), but everything that does not need to know
 * about Minecraft lives behind this interface.
 */
interface Browser : AutoCloseable {

    /** Currently navigated URL. Mutating it triggers a reload. */
    var url: String

    /** Whether the page is allowed to update its visual texture. */
    var visible: Boolean

    /** Whether the browser has finished loading the initial page. */
    val isInitialized: Boolean

    /** Last known lifecycle state (Loading / Success / Failure). */
    val state: BrowserState

    /** Logical viewport. Updated by the owning screen every frame. */
    val viewport: BrowserViewport

    /** Settings (scale, sync-fps, visibility) for this instance. */
    val settings: BrowserSettings

    /** Listener that gets called every time [state] changes. */
    fun onStateChange(consumer: Consumer<BrowserState>)

    /** Forward a typed character to the page. */
    fun charTyped(codePoint: Int, modifiers: Int)

    /** Forward a mouse-button event. */
    fun mouseButton(button: Int, state: Boolean, modifiers: Int)

    /** Forward a mouse-move event (in Minecraft GUI pixels). */
    fun mouseMove(x: Int, y: Int, modifiers: Int)

    /** Forward a vertical scroll event. */
    fun mouseScroll(amountX: Double, amountY: Double, modifiers: Int)

    /** Forward a keyboard key. */
    fun keyEvent(keyCode: Int, scanCode: Int, modifiers: Int, state: Boolean)

    /** Inject raw JS into the page. */
    fun runJavaScript(script: String)

    /** Reload the current page. */
    fun reload()

    /** Resize the browser to [width]x[height] pixels. */
    fun resize(width: Int, height: Int)

    /**
     * Register a JS ↔ Kotlin bridge. The callback is invoked when the page
     * sends a message of the given [channel] name. Implementations are
     * expected to call [InputListener] on the browser thread to keep the
     * order of incoming messages deterministic.
     */
    fun onMessage(channel: String, listener: (String) -> Unit)

    /** Stop the browser. Idempotent. */
    override fun close()
}
