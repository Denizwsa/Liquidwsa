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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraTargetTracker
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity

/**
 * TargetHud module with two rendering modes.
 *
 * - [Mode.SCREEN_2D]: Classic 2D overlay panel anchored to a screen position.
 * - [Mode.WORLD_3D]: 3D nametag projected onto the entity's head with smooth
 *   position lerp, 3-layer pulse glow, and animated health bar that smoothly
 *   fills in both directions.
 */
@Suppress("MagicNumber")
object ModuleTargetHud : ClientModule("TargetHud", ModuleCategories.RENDER) {

    private val mode by enumChoice("Mode", Mode.SCREEN_2D)

    private val scale by float("Scale", 1.0f, 0.5f..2.0f)
    private val showName by boolean("ShowName", true)
    private val showHealth by boolean("ShowHealth", true)
    private val showArmor by boolean("ShowArmor", true)
    private val showDistance by boolean("ShowDistance", true)
    private val showHitCount by boolean("ShowHitCount", true)

    // Screen2D-specific
    private val screenOffsetX by int("ScreenOffsetX", 0, -400..400)
    private val screenOffsetY by int("ScreenOffsetY", 0, -400..400)
    private val screenPosition by enumChoice("ScreenPosition", ScreenPosition.TOP_CENTER)
    private val screenStyle by enumChoice("ScreenStyle", ScreenStyle.VANILLA)

    // World3D-specific
    private val world3DOffsetY by float("World3DOffsetY", 0.6f, 0.0f..2.0f)
    private val world3DGlowColor by color("World3DGlowColor", Color4b(74, 143, 255, 200))
    private val world3DPulseSpeed by float("World3DPulseSpeed", 2.0f, 0.0f..6.0f)
    private val world3DPulseAmount by float("World3DPulseAmount", 0.4f, 0.0f..1.0f)
    private val world3DLerpSpeed by float("World3DLerpSpeed", 0.3f, 0.05f..1.0f)
    private val world3DHealthLerpSpeed by float("World3DHealthLerpSpeed", 0.25f, 0.05f..1.0f)

    private enum class Mode(override val tag: String) : Tagged {
        SCREEN_2D("Screen2D"),
        WORLD_3D("World3D");

        override fun toString() = tag
    }

    private enum class ScreenPosition(override val tag: String) : Tagged {
        TOP_LEFT("TopLeft"),
        TOP_CENTER("TopCenter"),
        TOP_RIGHT("TopRight"),
        MIDDLE_LEFT("MiddleLeft"),
        MIDDLE_RIGHT("MiddleRight");

        override fun toString() = tag
    }

    private enum class ScreenStyle(override val tag: String) : Tagged {
        VANILLA("Vanilla"),
        GRADIENT("Gradient");

        override fun toString() = tag
    }

    // Hit count tracking by target UUID. Reset whenever the target changes.
    private var trackedUuid: java.util.UUID? = null
    private var previousHealth: Float = -1f
    private var hitCount: Int = 0

    // World3D animation state
    private var cachedScreenX: Float = 0f
    private var cachedScreenY: Float = 0f
    private var lastTargetUuid: java.util.UUID? = null
    private var animatedHealth: Float = 0f

    @Suppress("unused")
    private val worldRenderHandler = handler<WorldRenderEvent> { event ->
        if (mode != Mode.WORLD_3D) return@handler
        val target = KillAuraTargetTracker.target?.takeIf { it.isAlive && !it.isRemoved }
            ?: return@handler
        if (target === player) return@handler
        if (target !is LivingEntity) return@handler

        updateHitCount(target)

        val worldPos = target.getEyePosition(event.partialTicks)
            .add(0.0, world3DOffsetY.toDouble(), 0.0)
        val cam = event.camera
        val screen = WorldToScreen.calculateScreenPos(worldPos, cam.position()) ?: return@handler

        if (target.uuid != lastTargetUuid) {
            cachedScreenX = screen.x
            cachedScreenY = screen.y
            lastTargetUuid = target.uuid
            animatedHealth = target.health
        } else {
            val lerp = world3DLerpSpeed.coerceIn(0f, 1f)
            cachedScreenX = Mth.lerp(lerp, cachedScreenX, screen.x)
            cachedScreenY = Mth.lerp(lerp, cachedScreenY, screen.y)
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val target = KillAuraTargetTracker.target?.takeIf { it.isAlive && !it.isRemoved }
            ?: return@handler
        if (target !== player && target is LivingEntity) {
            when (mode) {
                Mode.SCREEN_2D -> drawScreen2DHud(event, target)
                Mode.WORLD_3D -> drawWorld3DHud(event, target)
            }
        }
    }

    private fun drawScreen2DHud(event: OverlayRenderEvent, target: LivingEntity) {
        val context = event.context
        val font = mc.font
        val screenW = context.guiWidth()
        val screenH = context.guiHeight()

        updateHitCount(target)

        val name = if (showName) target.displayName?.string ?: target.name.string else null
        val healthPct = (target.health / target.maxHealth.toFloat()).coerceIn(0f, 1f)
        val armorPct = if (target is net.minecraft.world.entity.player.Player) {
            (target.armorValue.toFloat() / 20f).coerceIn(0f, 1f)
        } else null
        val distance = player.distanceTo(target)
        val hits = if (showHitCount) hitCount else null

        val lines = mutableListOf<Pair<String, Color4b>>()
        if (name != null) lines += name to Color4b.WHITE
        if (showHealth) {
            val hp = "${"%.1f".format(target.health)}/${target.maxHealth.toFloat()}"
            lines += hp to healthColor(healthPct)
        }
        if (showArmor && armorPct != null) {
            val armor = "${target.armorValue}/20"
            lines += armor to Color4b(160, 200, 255, 255)
        }
        if (showDistance) lines += "${"%.1f".format(distance)}m" to Color4b(200, 200, 200, 255)
        if (hits != null) lines += "Hits: $hits" to Color4b(255, 200, 140, 255)
        if (lines.isEmpty()) return

        val padding = 4
        val nameW = font.width(lines.first().first)
        val valueW = lines.drop(1).maxOfOrNull { font.width(it.first) } ?: 0
        val nameRowH = font.lineHeight + 2
        val valueRowH = font.lineHeight + 1
        val w = (padding * 2 + nameW).coerceAtLeast(padding * 2 + valueW)
        val h = padding * 2 + nameRowH + valueRowH * (lines.size - 1).coerceAtLeast(0)

        val s = scale
        val scaledW = (w * s).toInt()
        val scaledH = (h * s).toInt()

        val (x, y) = computeScreenPosition(screenW, screenH, scaledW, scaledH)

        val bgColor = if (screenStyle == ScreenStyle.GRADIENT) {
            Color4b(20, 20, 20, 160)
        } else {
            Color4b(0, 0, 0, 150)
        }
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
            val textY = curY
            val textX = x + padding
            context.text(font, text, textX, textY, color.argb, true)
            curY += rowH
        }
    }

    private fun drawWorld3DHud(event: OverlayRenderEvent, target: LivingEntity) {
        val context = event.context
        val font = mc.font
        val screenW = context.guiWidth()
        val screenH = context.guiHeight()

        val name = if (showName) target.displayName?.string ?: target.name.string else null
        val currentHealth = target.health.coerceAtLeast(0f)
        val maxHealth = target.maxHealth.toFloat().coerceAtLeast(0.01f)
        val healthPct = (currentHealth / maxHealth).coerceIn(0f, 1f)
        val armorPct = if (target is net.minecraft.world.entity.player.Player) {
            (target.armorValue.toFloat() / 20f).coerceIn(0f, 1f)
        } else null
        val distance = player.distanceTo(target)

        val lines = mutableListOf<Pair<String, Color4b>>()
        if (name != null) lines += name to Color4b.WHITE
        if (showHealth) {
            val hp = "${"%.1f".format(currentHealth)}/${"%.1f".format(maxHealth)}"
            lines += hp to healthColor(healthPct)
        }
        if (showArmor && armorPct != null) {
            lines += "${target.armorValue}/20" to Color4b(160, 200, 255, 255)
        }
        if (showDistance) lines += "${"%.1f".format(distance)}m" to Color4b(200, 200, 200, 255)
        if (showHitCount) lines += "Hits: $hitCount" to Color4b(255, 200, 140, 255)
        if (lines.isEmpty()) return

        val padding = 6
        val lineGap = 2
        val nameW = font.width(lines.first().first)
        val valueW = lines.drop(1).maxOfOrNull { font.width(it.first) } ?: 0
        val innerW = nameW.coerceAtLeast(valueW)
        val nameRowH = font.lineHeight + 1
        val valueRowH = font.lineHeight
        val hasHealthBar = showHealth
        val healthBarH = if (hasHealthBar) 4 else 0
        val healthBarGap = if (hasHealthBar) 4 else 0
        val valuesCount = (lines.size - 1).coerceAtLeast(0)
        val contentH = nameRowH + valuesCount * (valueRowH + lineGap) + healthBarH + healthBarGap
        val w = padding * 2 + innerW
        val h = padding * 2 + contentH

        val s = scale
        val scaledW = (w * s).toInt()
        val scaledH = (h * s).toInt()

        val cx = cachedScreenX
        val cy = cachedScreenY
        val x = (cx - scaledW / 2f).toInt()
        val y = (cy - scaledH - 8f).toInt()

        if (x + scaledW < 0 || x > screenW || y + scaledH < 0 || y > screenH) return

        val time = System.currentTimeMillis() / 1000.0
        val pulse = if (world3DPulseSpeed > 0f) {
            (0.5 + 0.5 * Math.sin(time * world3DPulseSpeed * 2.0 * Math.PI / 1.5)) *
                world3DPulseAmount + (1.0 - world3DPulseAmount)
        } else 1.0
        val glow = world3DGlowColor
        val baseAlpha = (glow.a * pulse).toInt().coerceIn(0, 255)
        val layer1Alpha = (baseAlpha * 0.10f).toInt().coerceIn(0, 255)
        val layer2Alpha = (baseAlpha * 0.22f).toInt().coerceIn(0, 255)
        val layer3Alpha = (baseAlpha * 0.50f).toInt().coerceIn(0, 255)
        val layer1Expand = 6f
        val layer2Expand = 3f
        val layer3Expand = 1f
        val outlineAlpha = (180 * pulse).toInt().coerceIn(0, 255)

        with(context) {
            drawRoundedRect(
                (x - layer1Expand).toFloat(), (y - layer1Expand).toFloat(),
                (x + scaledW + layer1Expand).toFloat(), (y + scaledH + layer1Expand).toFloat(),
                8f,
                fillColor = Color4b(glow.r, glow.g, glow.b, layer1Alpha),
            )
            drawRoundedRect(
                (x - layer2Expand).toFloat(), (y - layer2Expand).toFloat(),
                (x + scaledW + layer2Expand).toFloat(), (y + scaledH + layer2Expand).toFloat(),
                7f,
                fillColor = Color4b(glow.r, glow.g, glow.b, layer2Alpha),
            )
            drawRoundedRect(
                (x - layer3Expand).toFloat(), (y - layer3Expand).toFloat(),
                (x + scaledW + layer3Expand).toFloat(), (y + scaledH + layer3Expand).toFloat(),
                6f,
                fillColor = Color4b(glow.r, glow.g, glow.b, layer3Alpha),
            )

            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + scaledW).toFloat(), (y + scaledH).toFloat(), 4f,
                fillColor = Color4b(15, 15, 18, 200),
            )
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + scaledW).toFloat(), (y + scaledH).toFloat(), 4f,
                fillColor = Color4b.TRANSPARENT,
                outlineColor = Color4b(glow.r, glow.g, glow.b, outlineAlpha),
                outlineWidth = 1.0f,
            )
        }

        var curY = y + padding
        for ((i, line) in lines.withIndex()) {
            val (text, color) = line
            val rowH = if (i == 0) nameRowH else valueRowH
            val textX = x + padding + (innerW - font.width(text)) / 2
            context.text(font, text, textX, curY, color.argb, true)
            curY += rowH + lineGap
        }

        if (hasHealthBar) {
            val barY = y + scaledH - padding - healthBarH
            val barX = x + padding
            val barW = scaledW - padding * 2
            val barH = healthBarH

            with(context) {
                drawRoundedRect(
                    barX.toFloat(), barY.toFloat(),
                    (barX + barW).toFloat(), (barY + barH).toFloat(), 1.5f,
                    fillColor = Color4b(40, 40, 40, 200),
                )
            }

            val healthLerp = world3DHealthLerpSpeed.coerceIn(0f, 1f)
            animatedHealth = Mth.lerp(healthLerp, animatedHealth, currentHealth)
            val displayedPct = (animatedHealth / maxHealth).coerceIn(0f, 1f)
            val filledW = (barW * displayedPct).toInt().coerceAtLeast(0)
            if (filledW > 0) {
                val hcolor = healthColor(displayedPct)
                with(context) {
                    drawRoundedRect(
                        barX.toFloat(), barY.toFloat(),
                        (barX + filledW).toFloat(), (barY + barH).toFloat(), 1.5f,
                        fillColor = hcolor,
                    )
                }
            }
        }

        val triCenterX = cx.toInt()
        val triTopY = y + scaledH
        val triSize = 5
        val triFill = Color4b(15, 15, 18, 200)
        val triOutline = Color4b(glow.r, glow.g, glow.b, outlineAlpha)
        with(context) {
            drawTriangle(
                triCenterX.toFloat(), triTopY.toFloat(),
                (triCenterX - triSize).toFloat(), (triTopY + triSize).toFloat(),
                (triCenterX + triSize).toFloat(), (triTopY + triSize).toFloat(),
                fillColor = triFill,
                outlineColor = triOutline,
            )
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
        val r = (pct > 0.5f).let { if (it) (1f - (pct - 0.5f) * 2f) else 1f }
        val g = (pct > 0.5f).let { if (it) 1f else pct * 2f }
        val rr = (r * 255).toInt().coerceIn(0, 255)
        val gg = (g * 255).toInt().coerceIn(0, 255)
        return Color4b(rr, gg, 80, 255)
    }

    private fun computeScreenPosition(
        screenW: Int,
        screenH: Int,
        w: Int,
        h: Int,
    ): Pair<Int, Int> {
        val mx = screenOffsetX
        val my = screenOffsetY
        return when (screenPosition) {
            ScreenPosition.TOP_LEFT -> mx to (4 + my)
            ScreenPosition.TOP_CENTER -> (screenW / 2 - w / 2 + mx) to (4 + my)
            ScreenPosition.TOP_RIGHT -> (screenW - w - 4 + mx) to (4 + my)
            ScreenPosition.MIDDLE_LEFT -> mx to (screenH / 2 - h / 2 + my)
            ScreenPosition.MIDDLE_RIGHT -> (screenW - w - 4 + mx) to (screenH / 2 - h / 2 + my)
        }
    }
}
