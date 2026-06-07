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
 * Callback the owning screen wires into the browser. Called from
 * [BrowserInputHandler] whenever a meaningful input event arrives. Screens
 * implement this to forward the event to Minecraft's input queue (e.g.
 * `Minecraft.getInstance().screen?.keyPressed(...)` for typing-into-search).
 *
 * Default no-op implementation is convenient for overlays that only want
 * to forward input into the browser.
 */
fun interface InputListener {
    fun onInput(event: BrowserInputEvent)
}
