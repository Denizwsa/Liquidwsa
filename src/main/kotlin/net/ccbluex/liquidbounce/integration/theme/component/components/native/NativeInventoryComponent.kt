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

package net.ccbluex.liquidbounce.integration.theme.component.components.native

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.render.gui.ItemStackListRenderer.drawItemStackList
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.render.Alignment
import kotlin.math.ceil

/**
 * Native (non-browser) inventory viewer: draws the player's main inventory grid using the existing
 * [drawItemStackList] renderer. Renders only while the native HUD theme is active.
 *
 * Mirrors `src-theme/src/routes/hud/elements/inventory/GenericPlayerInventory.svelte`.
 */
object NativeInventoryComponent : NativeHudComponent(
    "Inventory", false, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.LEFT,
        horizontalOffset = 4,
        verticalAlignment = Alignment.ScreenAxisY.CENTER_TRANSLATED,
        verticalOffset = 60,
    )
) {

    private val scaleSetting by float("Scale", 1.0f, 0.25f..3.0f)

    private const val ROW_LENGTH = 9
    private const val CELL_SIZE = 16f

    init {
        registerComponentListen(this)
    }

    @Suppress("unused")
    val renderHandler = handler<OverlayRenderEvent> { event ->
        if (!enabled || !ModuleHud.running || HideAppearance.isHidingNow || !ThemeManager.isNativeActive) {
            return@handler
        }

        val stacks = player.inventory.nonEquipmentItems.toList()
        if (stacks.isEmpty()) {
            return@handler
        }

        // Estimate the rendered size so the alignment anchor places the centered grid correctly.
        val rows = ceil(stacks.size / ROW_LENGTH.toFloat()).toInt().coerceAtLeast(1)
        val estWidth = ROW_LENGTH * CELL_SIZE * scaleSetting
        val estHeight = rows * CELL_SIZE * scaleSetting
        val bb = alignment.getBounds(estWidth, estHeight)

        with(event.context) {
            drawItemStackList(stacks)
                .rowLength(ROW_LENGTH)
                .scale(scaleSetting)
                .centerX((bb.xMin + bb.xMax) * 0.5f)
                .centerY((bb.yMin + bb.yMax) * 0.5f)
                .rectBackground(NativeHudPalette.BG_50, margin = 4f)
                .draw()
        }
    }
}
