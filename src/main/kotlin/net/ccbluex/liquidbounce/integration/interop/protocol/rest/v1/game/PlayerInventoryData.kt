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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player

/**
 * Minimal inventory snapshot kept as a stub after the web integration was
 * removed. The data is no longer transmitted, but trackers still create
 * instances to satisfy listeners.
 */
data class PlayerInventoryData(
    val slots: List<net.minecraft.world.item.ItemStack> = emptyList(),
) {
    companion object {
        @JvmStatic
        fun fromInventory(inventory: Inventory): PlayerInventoryData = PlayerInventoryData(
            slots = (0 until inventory.containerSize).map { inventory.getItem(it) },
        )

        @JvmStatic
        fun fromPlayer(player: Player): PlayerInventoryData =
            fromInventory(player.inventory)
    }
}
