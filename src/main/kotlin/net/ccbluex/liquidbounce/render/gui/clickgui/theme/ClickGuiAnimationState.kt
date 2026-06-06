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
package net.ccbluex.liquidbounce.render.gui.clickgui.theme

import net.minecraft.util.Mth

/**
 * Animation state container with lightweight per-frame lerp helpers.
 *
 * Each animation tracks a [from] / [to] pair and a starting timestamp.
 * Call [update] every frame to advance, and [isActive] to query whether
 * the animation has reached its target. The cost is one subtraction +
 * a clamp per field — well under 0.1ms per frame even with 50 fields.
 */
class LerpState(
    var current: Float = 0f,
    var target: Float = 0f,
    private val durationMs: Long,
) {
    private var startValue: Float = 0f
    private var startTimeMs: Long = 0L
    private var lastTarget: Float = 0f

    fun setTarget(value: Float, nowMs: Long) {
        if (value == lastTarget) return
        if (startTimeMs == 0L) {
            startValue = current
            startTimeMs = nowMs
        } else if (lastTarget != target) {
            startValue = current
            startTimeMs = nowMs
        }
        target = value
        lastTarget = value
    }

    fun snap(value: Float) {
        current = value
        target = value
        startValue = value
        lastTarget = value
        startTimeMs = 0L
    }

    fun update(nowMs: Long): Float {
        if (current == target && startTimeMs == 0L) return current
        val elapsed = nowMs - startTimeMs
        if (elapsed >= durationMs || startTimeMs == 0L) {
            current = target
            startTimeMs = 0L
            return current
        }
        val t = (elapsed.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0)
        val eased = smoothstep(t.toFloat())
        current = Mth.lerp(eased, startValue, target)
        return current
    }

    fun isAtTarget(): Boolean = current == target

    companion object {
        fun smoothstep(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            return x * x * (3f - 2f * x)
        }

        fun easeOut(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            return 1f - (1f - x) * (1f - x)
        }

        fun easeInOut(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            return if (x < 0.5f) {
                2f * x * x
            } else {
                val f = -2f * x + 2f
                1f - f * f / 2f
            }
        }
    }
}
