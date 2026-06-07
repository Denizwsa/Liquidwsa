/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClickGuiScaleChangeEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiScreen
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import org.lwjgl.glfw.GLFW

/**
 * Native Java ClickGUI module.
 *
 * Tapping Right Shift opens the Java-side [ClickGuiScreen] that paints a
 * Svelte-style panel grid directly via 1.21+'s [GuiGraphicsExtractor] —
 * no CEF, no WebSocket, no browser process.
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

    override fun onEnabled() {
        if (!inGame) return
        mc.setScreen(ClickGuiScreen())
        super.onEnabled()
    }

    override fun onDisabled() {
        if (mc.screen is ClickGuiScreen) {
            mc.setScreen(null)
        }
        super.onDisabled()
    }

    /**
     * No-op stub for callers that used to tell the WebGUI browser to reload
     * after configuration or bind changes. The native GUI reads values
     * directly, so explicit synchronisation is unnecessary.
     */
    @Suppress("unused")
    fun sync() = Unit
}
