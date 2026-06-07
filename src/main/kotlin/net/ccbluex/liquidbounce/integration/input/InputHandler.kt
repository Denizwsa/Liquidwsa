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
package net.ccbluex.liquidbounce.integration.input

import net.ccbluex.liquidbounce.integration.browser.Browser

/**
 * Glues a Minecraft `Screen` (which captures raw input events) to a
 * [Browser] (which expects typed [BrowserInputEvent]s). Handles the focus
 * gating so that keyboard input is forwarded to the page only when the
 * browser has actually grabbed focus — otherwise the events would still
 * hit the underlying Minecraft screen and the player would suddenly start
 * sprinting.
 */
class BrowserInputHandler(private val browser: Browser) : InputAcceptor {

    @Volatile private var hasFocus: Boolean = false

    override fun accept(event: BrowserInputEvent) {
        if (event is BrowserInputEvent.FocusChanged) {
            hasFocus = event.focused
            return
        }
        if (!hasFocus) return

        when (event) {
            is BrowserInputEvent.CharTyped ->
                browser.charTyped(event.codePoint, event.modifiers)
            is BrowserInputEvent.MouseButton ->
                browser.mouseButton(event.button, event.state, event.modifiers)
            is BrowserInputEvent.MouseMove ->
                browser.mouseMove(event.x, event.y, event.modifiers)
            is BrowserInputEvent.MouseScroll ->
                browser.mouseScroll(event.dx, event.dy, event.modifiers)
            is BrowserInputEvent.KeyEvent ->
                browser.keyEvent(event.keyCode, event.scanCode, event.modifiers, event.pressed)
            is BrowserInputEvent.Resize ->
                browser.resize(event.width, event.height)
            is BrowserInputEvent.FocusChanged -> Unit
        }
    }
}
