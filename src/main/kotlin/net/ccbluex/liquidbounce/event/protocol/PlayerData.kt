/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU ClickGui-style Apache 2.0-or-later stub that
 * exists so legacy event call sites compile after the CEF/WebGUI removal.
 */
package net.ccbluex.liquidbounce.event.protocol

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

data class PlayerData(
    val id: Int,
    val name: String,
    val health: Float,
    val maxHealth: Float,
    val armor: Int,
    val distance: Float,
) {
    companion object {
        @Suppress("unused")
        fun fromPlayer(entity: LivingEntity?): PlayerData? {
            if (entity !is Player) return null
            val mc = net.ccbluex.liquidbounce.utils.client.mc
            val dist = mc.player?.let { it.distanceTo(entity) } ?: 0f
            return PlayerData(
                id = entity.id,
                name = entity.gameProfile.name,
                health = entity.health,
                maxHealth = entity.maxHealth,
                armor = entity.armorValue,
                distance = dist,
            )
        }
    }
}

data class PlayerInventoryData(
    val slots: List<ItemStack>,
) {
    companion object {
        @Suppress("unused")
        fun fromPlayer(player: Player?): PlayerInventoryData {
            // Stub: return empty list for native ClickGUI; the old WebGUI
            // used this to sync inventory to the browser, which we no longer need.
            return PlayerInventoryData(
                slots = emptyList(),
            )
        }
    }
}
