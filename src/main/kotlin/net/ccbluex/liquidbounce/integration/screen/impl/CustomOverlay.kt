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
import net.minecraft.client.gui.screens.Screen

/**
 * In-world overlay that paints the integration browser on top of the
 * game. The "overlay" path is used for things like the HUD editor, where
 * we want the browser visible while the player can still see and interact
 * with the world.
 */
class CustomOverlay(
    screenType: CustomScreenType,
) : CustomSharedMinecraftScreen(screenType) {

    override val customScreen = super.customScreen
    override fun isPauseScreen(): Boolean = false
}

/** Convenience: any virtual screen the integration can show. */
sealed interface VirtualScreen {
    fun open(parent: Screen?)
}
