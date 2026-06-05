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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraTargetTracker
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity

/**
 * Lightweight 2D TargetHud that draws a small panel with the currently
 * tracked KillAura target's name, health, armor, distance, and hit count.
 *
 * The target is read from [KillAuraTargetTracker] which is the global
 * singleton used by [net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura].
 */
@Suppress("MagicNumber")
object ModuleTargetHud : ClientModule("TargetHud", ModuleCategories.RENDER) {

    private val scale by float("Scale", 1.0f, 0.5f..2.0f)
    private val offsetX by int("OffsetX", 0, -200..200)
    private val offsetY by int("OffsetY", 0, -200..200)
    private val showName by boolean("ShowName", true)
    private val showHealth by boolean("ShowHealth", true)
    private val showArmor by boolean("ShowArmor", true)
    private val showDistance by boolean("ShowDistance", true)
    private val showHitCount by boolean("ShowHitCount", true)
    private val position by enumChoice("Position", Position.TOP_CENTER)
    private val style by enumChoice("Style", Style.VANILLA)

    private enum class Position(override val tag: String) : Tagged {
        TOP_LEFT("TopLeft"),
        TOP_CENTER("TopCenter"),
        TOP_RIGHT("TopRight"),
        MIDDLE_LEFT("MiddleLeft"),
        MIDDLE_RIGHT("MiddleRight");

        override fun toString() = tag
    }

    private enum class Style(override val tag: String) : Tagged {
        VANILLA("Vanilla"),
        GRADIENT("Gradient");

        override fun toString() = tag
    }

    // Hit count tracking by target UUID. Reset whenever the target changes.
    private var trackedUuid: java.util.UUID? = null
    private var previousHealth: Float = -1f
    private var hitCount: Int = 0

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val target = KillAuraTargetTracker.target?.takeIf { it.isAlive && !it.isRemoved }
            ?: return@handler
        if (target !== player && target is LivingEntity) {
            drawHud(event, target)
        }
    }

    private fun drawHud(event: OverlayRenderEvent, target: LivingEntity) {
        val context = event.context
        val font = mc.font
        val screenW = context.guiWidth()
        val screenH = context.guiHeight()

        // Update hit count
        updateHitCount(target)

        // Build display info
        val name = if (showName) target.displayName?.string ?: target.name.string else null
        val healthPct = (target.health / target.maxHealth.toFloat()).coerceIn(0f, 1f)
        val armorPct = if (target is net.minecraft.world.entity.player.Player) {
            (target.armorValue.toFloat() / 20f).coerceIn(0f, 1f)
        } else null
        val distance = player.distanceTo(target)
        val hits = if (showHitCount) hitCount else null

        // Pre-compute lines
        val lines = mutableListOf<Pair<String, Color4b>>()
        if (name != null) {
            lines += name to Color4b.WHITE
        }
        if (showHealth) {
            val hp = "${"%.1f".format(target.health)}/${target.maxHealth.toFloat()}"
            lines += hp to healthColor(healthPct)
        }
        if (showArmor && armorPct != null) {
            val armor = "${target.armorValue}/20"
            lines += armor to Color4b(160, 200, 255, 255)
        }
        if (showDistance) {
            lines += "${"%.1f".format(distance)}m" to Color4b(200, 200, 200, 255)
        }
        if (hits != null) {
            lines += "Hits: $hits" to Color4b(255, 200, 140, 255)
        }
        if (lines.isEmpty()) return

        // Measure
        val padding = 4
        val nameW = font.width(lines.first().first)
        val valueW = lines.drop(1).maxOfOrNull { font.width(it.first) } ?: 0
        val nameRowH = font.lineHeight + 2
        val valueRowH = font.lineHeight + 1
        val w = (padding * 2 + nameW).coerceAtLeast(padding * 2 + valueW)
        val h = padding * 2 + nameRowH + valueRowH * (lines.size - 1).coerceAtLeast(0)

        // Base dimensions then scale
        val s = scale
        val scaledW = (w * s).toInt()
        val scaledH = (h * s).toInt()

        // Position
        val (x, y) = computePosition(screenW, screenH, scaledW, scaledH)

        val bgColor = Color4b(0, 0, 0, 150)
        val outlineColor = Color4b(0, 0, 0, 220)

        with(context) {
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + scaledW).toFloat(), (y + scaledH).toFloat(), 3f,
                fillColor = bgColor,
            )
        }

        var curY = y + padding
        for ((i, line) in lines.withIndex()) {
            val (text, color) = line
            val rowH = if (i == 0) nameRowH else valueRowH
            // text is drawn at native font scale; we don't apply scale to text (keep readable)
            val textY = curY
            val textX = if (i == 0) {
                x + padding
            } else {
                x + padding
            }
            context.text(font, text, textX, textY, color.argb, true)
            curY += rowH
        }
    }

    private fun updateHitCount(target: LivingEntity) {
        val uuid = target.uuid
        if (uuid != trackedUuid) {
            trackedUuid = uuid
            previousHealth = target.maxHealth.toFloat()
            hitCount = 0
            return
        }
        val current = target.health
        if (previousHealth > 0f && current < previousHealth - 0.05f) {
            hitCount++
        }
        previousHealth = current
    }

    private fun healthColor(pct: Float): Color4b {
        // green -> yellow -> red
        val r = (pct > 0.5f).let { if (it) (1f - (pct - 0.5f) * 2f) else 1f }
        val g = (pct > 0.5f).let { if (it) 1f else pct * 2f }
        val rr = (r * 255).toInt().coerceIn(0, 255)
        val gg = (g * 255).toInt().coerceIn(0, 255)
        return Color4b(rr, gg, 80, 255)
    }

    private fun computePosition(
        screenW: Int,
        screenH: Int,
        w: Int,
        h: Int,
    ): Pair<Int, Int> {
        val mx = offsetX
        val my = offsetY
        return when (position) {
            Position.TOP_LEFT -> mx to (4 + my)
            Position.TOP_CENTER -> (screenW / 2 - w / 2 + mx) to (4 + my)
            Position.TOP_RIGHT -> (screenW - w - 4 + mx) to (4 + my)
            Position.MIDDLE_LEFT -> mx to (screenH / 2 - h / 2 + my)
            Position.MIDDLE_RIGHT -> (screenW - w - 4 + mx) to (screenH / 2 - h / 2 + my)
        }
    }
}
