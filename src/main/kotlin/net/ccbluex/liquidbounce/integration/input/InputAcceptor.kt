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

/**
 * Single input event from Minecraft that should be forwarded to the browser.
 * Captured by the [BrowserInputHandler] when the on-screen browser has focus.
 */
sealed class BrowserInputEvent {
    data class CharTyped(val codePoint: Int, val modifiers: Int) : BrowserInputEvent()
    data class MouseButton(val button: Int, val state: Boolean, val modifiers: Int) : BrowserInputEvent()
    data class MouseMove(val x: Int, val y: Int, val modifiers: Int) : BrowserInputEvent()
    data class MouseScroll(val dx: Double, val dy: Double, val modifiers: Int) : BrowserInputEvent()
    data class KeyEvent(val keyCode: Int, val scanCode: Int, val modifiers: Int, val pressed: Boolean) : BrowserInputEvent()
    data class Resize(val width: Int, val height: Int) : BrowserInputEvent()
    data class FocusChanged(val focused: Boolean) : BrowserInputEvent()
}

/**
 * Sink of input events. Implemented by [Browser].
 */
interface InputAcceptor {
    fun accept(event: BrowserInputEvent)
}
