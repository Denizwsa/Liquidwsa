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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

/**
 * Stub for the upstream data class that the Svelte side used to
 * serialise the local player. The minimal WebGUI core doesn't need a
 * real implementation because the interop server is also stubbed.
 *
 * Keep the field set as small as possible — only the bits referenced
 * from [net.ccbluex.liquidbounce.event.events.ClientEvents] /
 * `UserInterfaceEvents` / `CombatManager` are populated.
 */
data class PlayerData(
    val username: String = "",
    val uuid: String? = null,
    val ping: Int = 0,
    val gameMode: String = "survival",
) {
    companion object {
        /** Empty data set, used when we don't have a real player. */
        fun fromPlayer(@Suppress("UNUSED_PARAMETER") player: Any?): PlayerData = PlayerData()
    }
}

/** Stub for the player's inventory state. */
data class PlayerInventoryData(
    val items: List<String> = emptyList(),
) {
    companion object {
        fun fromPlayer(@Suppress("UNUSED_PARAMETER") player: Any?): PlayerInventoryData =
            PlayerInventoryData()
    }
}
