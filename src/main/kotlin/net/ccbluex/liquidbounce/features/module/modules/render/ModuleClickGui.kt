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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiScaleChangeEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitSeconds
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.integration.screen.impl.CustomStandaloneMinecraftScreen
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import org.lwjgl.glfw.GLFW

/**
 * WebGUI-backed ClickGui module.
 *
 * Tapping Right Shift opens a CEF-backed `CustomSharedMinecraftScreen`
 * that paints a Svelte theme (`#clickgui`) on top of the Minecraft world.
 * The screen, browser and interop server are all created lazily so a
 * player who never opens the ClickGui pays zero overhead.
 */
object ModuleClickGui :
    ClientModule("ClickGUI", ModuleCategories.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    override val running: Boolean get() = true

    @Suppress("UnusedPrivateProperty")
    private val scale by float("Scale", 1f, 0.5f..2f).onChanged {
        EventManager.callEvent(ClickGuiScaleChangeEvent(it))
        EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }

    @Suppress("UnusedPrivateProperty", "unused")
    private val searchBarAutoFocus by boolean("SearchBarAutoFocus", true).onChanged {
        EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }

    /** Whether the search bar of the current ClickGui has focus. */
    val isInSearchBar: Boolean
        get() {
            val screen = mc.screen ?: return false
            return (screen is CustomSharedMinecraftScreen && screen.screenType == CustomScreenType.CLICK_GUI) ||
                (screen is CustomStandaloneMinecraftScreen && screen.screenType == CustomScreenType.CLICK_GUI)
        }

    object Snapping : ToggleableValueGroup(this, "Snapping", true) {
        @Suppress("UnusedPrivateProperty", "unused")
        private val gridSize by int("GridSize", 10, 1..100, "px").onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(ModuleClickGui))
        }

        init {
            inner.find { it.name == "Enabled" }?.onChanged {
                EventManager.callEvent(ClickGuiValueChangeEvent(ModuleClickGui))
            }
        }
    }

    init {
        tree(Snapping)
    }

    @Suppress("UnusedPrivateProperty")
    private val useStandaloneScreen by boolean("Cache", true).onChanged {
        mc.execute(::onEnabled)
    }

    private var standaloneScreen: CustomStandaloneMinecraftScreen? = null

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent>(priority = READ_FINAL_STATE) {
        // The browser backend is alive. The interop server is started the first
        // time the browser is created, so there is nothing to do here for now.
    }

    override fun onEnabled() {
        if (!inGame) return

        // Make sure the interop server is up before the browser tries to talk
        // to it. Both calls are no-ops if they are already running.
        ClientInteropServer.start()
        if (BrowserBackendManager.isSkipping) {
            // MCEF missing — open a fallback screen that just shows a message.
            mc.setScreen(CustomSharedMinecraftScreen(CustomScreenType.CLICK_GUI))
            super.onEnabled()
            return
        }

        updateStandaloneScreen()
        mc.execute {
            mc.setScreen(standaloneScreen ?: CustomSharedMinecraftScreen(CustomScreenType.CLICK_GUI))
        }
        super.onEnabled()
    }

    @Suppress("unused")
    private val worldChangeHandler = sequenceHandler<WorldChangeEvent>(
        priority = OBJECTION_AGAINST_EVERYTHING
    ) { event ->
        if (event.world == null || !useStandaloneScreen) {
            return@sequenceHandler
        }
        waitSeconds(1)
        if (updateStandaloneScreen()) {
            standaloneScreen?.sync()
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        standaloneScreen?.close()
        standaloneScreen = null
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        // Keep the browser visible state in sync with whether the player is
        // actually looking at the screen. Saves a fair amount of CPU on big
        // Svelte pages.
        standaloneScreen?.browser?.visible = mc.screen == standaloneScreen
    }

    fun updateStandaloneScreen(): Boolean {
        if (useStandaloneScreen) {
            if (standaloneScreen == null) {
                standaloneScreen = CustomStandaloneMinecraftScreen(CustomScreenType.CLICK_GUI)
            } else {
                return true
            }
        } else if (standaloneScreen != null) {
            standaloneScreen?.close()
            standaloneScreen = null
        }
        return false
    }

    fun sync() {
        standaloneScreen?.sync()
    }

    fun invalidate() {
        val screen = standaloneScreen ?: return
        val wasOpen = mc.screen == screen
        if (wasOpen) {
            mc.setScreen(null)
        }
        screen.close()
        standaloneScreen = null
        if (wasOpen) {
            updateStandaloneScreen()
            mc.setScreen(standaloneScreen ?: CustomSharedMinecraftScreen(CustomScreenType.CLICK_GUI))
        }
    }
}
