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

import net.ccbluex.liquidbounce.integration.screen.CustomScreenType

/**
 * A "standalone" screen wraps the integration browser and adds the typical
 * ClickGui controls: it can be opened, moved to a different position by
 * dragging, and survives world changes. The standalone cache lives in
 * [ModuleClickGui] which reuses the same instance until the world changes
 * or the user explicitly closes the screen.
 */
class CustomStandaloneMinecraftScreen(
    screenType: CustomScreenType,
) : CustomSharedMinecraftScreen(screenType) {

    /** Where on the screen the user dropped the window last time. */
    var dragOffsetX: Int = 24
    var dragOffsetY: Int = 56

    fun close() {
        onClose()
    }
}
