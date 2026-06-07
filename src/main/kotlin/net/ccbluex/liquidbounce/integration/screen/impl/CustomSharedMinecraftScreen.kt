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
package net.ccbluex.liquidbounce.integration.screen.impl

import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.browser.Browser
import net.ccbluex.liquidbounce.integration.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.input.BrowserInputEvent
import net.ccbluex.liquidbounce.integration.input.BrowserInputHandler
import net.ccbluex.liquidbounce.integration.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.screen.BrowserHolder
import net.ccbluex.liquidbounce.integration.screen.CustomScreen
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * A Minecraft `Screen` whose only job is to host the integration browser.
 * The Svelte side paints the entire UI; we just draw the browser texture
 * and forward input events to it.
 */
open class CustomSharedMinecraftScreen(
    val screenType: CustomScreenType,
    val originalScreen: Screen? = null,
    val parentScreen: Screen? = mc.screen,
) : Screen(Component.literal("VS-${screenType.routeName.uppercase()}")) {

    protected open val customScreen: CustomScreen = CustomScreen(screenType)
    private var inputHandler: BrowserInputHandler? = null
    private var inputAcceptor: InputAcceptor? = null

    var browser: Browser? = null

    /** Re-sync the browser contents (called after a world change). */
    open fun sync() {
        browser?.reload()
    }

    override fun init() {
        // Lazy backend init — first time the player opens the ClickGui.
        val b = BrowserBackendManager.acquireBrowser(
            url = (ThemeManager.theme ?: ThemeManager.fallback).entryPoint(screenType),
            settings = BrowserSettings(),
        )
        browser = b
        if (b != null) {
            inputHandler = BrowserInputHandler(b)
            inputAcceptor = inputHandler
            BrowserHolder.current = b
        }
    }

    override fun onClose() {
        BrowserHolder.current = null
        if (parentScreen is CustomSharedMinecraftScreen) {
            mc.setScreen(parentScreen)
        } else {
            super.onClose()
        }
    }

    override fun isPauseScreen(): Boolean = false

    /** Forward a typed character to the browser if it has focus. */
    fun forwardChar(codePoint: Int, modifiers: Int) =
        inputAcceptor?.accept(BrowserInputEvent.CharTyped(codePoint, modifiers))

    fun forwardKey(keyCode: Int, scanCode: Int, modifiers: Int, pressed: Boolean) =
        inputAcceptor?.accept(BrowserInputEvent.KeyEvent(keyCode, scanCode, modifiers, pressed))

    fun forwardMouse(button: Int, pressed: Boolean, modifiers: Int) =
        inputAcceptor?.accept(BrowserInputEvent.MouseButton(button, pressed, modifiers))

    fun forwardScroll(dx: Double, dy: Double, modifiers: Int) =
        inputAcceptor?.accept(BrowserInputEvent.MouseScroll(dx, dy, modifiers))

    fun forwardMove(x: Int, y: Int, modifiers: Int) =
        inputAcceptor?.accept(BrowserInputEvent.MouseMove(x, y, modifiers))

    fun setFocus(focused: Boolean) =
        inputAcceptor?.accept(BrowserInputEvent.FocusChanged(focused))
}
