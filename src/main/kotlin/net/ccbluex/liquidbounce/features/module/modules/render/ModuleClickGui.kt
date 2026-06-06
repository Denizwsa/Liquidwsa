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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiScreen
import net.ccbluex.liquidbounce.render.gui.clickgui.NotificationRenderer
import net.ccbluex.liquidbounce.utils.client.mc
import org.lwjgl.glfw.GLFW

/**
 * Java-side ClickGui module.
 *
 * Tapping Right Shift toggles the [ClickGuiScreen] in-game. The module
 * is a normal toggle: when enabled, the GUI is opened and the module is
 * immediately disabled so the next press opens it again.
 */
object ModuleClickGui : ClientModule(
    name = "ClickGui",
    category = ModuleCategories.RENDER,
    bind = GLFW.GLFW_KEY_RIGHT_SHIFT,
    state = false,
) {

    /**
     * Force-initializes the [NotificationRenderer] singleton so its event
     * hooks are registered with the [net.ccbluex.liquidbounce.event.EventManager].
     * Without this reference the Kotlin object would never be class-loaded
     * and no notifications would fire.
     */
    @Suppress("unused")
    private val notificationRendererRef: NotificationRenderer = NotificationRenderer
    /**
     * Re-sync the ClickGui after the module list or value registry changes.
     * No-op in the Java-side implementation; kept for source compatibility
     * with code that used to notify the web theme.
     */
    @JvmStatic
    fun sync() {
        // No-op. The Java-side ClickGui reads from the module registry on
        // each render, so there is nothing to invalidate here.
    }

    override suspend fun enabledEffect() {
        if (mc.player == null) {
            return
        }
        mc.setScreen(ClickGuiScreen())
        // Re-enabling is handled by the next bind press, so reset the toggle
        // immediately to keep the clickgui purely bind-driven.
        this.enabled = false
    }
}
