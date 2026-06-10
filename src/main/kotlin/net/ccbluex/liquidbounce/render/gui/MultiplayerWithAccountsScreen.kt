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
package net.ccbluex.liquidbounce.render.gui

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.network.chat.Component

class MultiplayerWithAccountsScreen(
    previous: Screen?,
) : JoinMultiplayerScreen(previous ?: TitleScreen()) {

    override fun init() {
        super.init()

        addRenderableWidget(
            Button.builder(
                Component.literal("Account Manager"),
            ) {
                mc.setScreen(AccountManagerScreen(this@MultiplayerWithAccountsScreen))
            }.bounds(
                this.width - 154, 6, 148, 18,
            ).build()
        )
    }
}
