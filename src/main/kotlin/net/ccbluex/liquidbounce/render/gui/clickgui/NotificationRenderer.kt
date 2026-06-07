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
package net.ccbluex.liquidbounce.render.gui.clickgui

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

/**
 * Renders a stack of toast notifications in the bottom-right corner of the
 * screen. Subscribes to [NotificationEvent] and animates each toast with a
 * slide-in / slide-out transition.
 *
 * The implementation is intentionally lightweight: at most 5 visible toasts
 * are stored at any time, and per-frame work is O(visible-toasts).
 */
object NotificationRenderer : EventListener {
    override val debugDisplayName: Component = Component.literal("NotificationRenderer")
    override val running: Boolean = true
    override fun parent(): EventListener? = null
    override fun children(): List<EventListener> = emptyList()

    private const val MAX_VISIBLE = 5
    private const val DISPLAY_MS = 2500L
    private const val ANIM_MS = 300L
    private const val WIDTH = 280
    private const val HEIGHT = 56
    private const val GAP = 8
    private const val MARGIN = 16

    private data class Toast(
        val title: String,
        val message: String,
        val severity: NotificationEvent.Severity,
        val createdAtMs: Long,
    )

    private val toasts: ArrayDeque<Toast> = ArrayDeque()

    @Suppress("unused")
    private val eventHandler = handler<NotificationEvent> { event ->
        toasts.addLast(
            Toast(
                event.title,
                event.message,
                event.severity,
                System.currentTimeMillis(),
            )
        )
        while (toasts.size > MAX_VISIBLE) {
            toasts.removeFirst()
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val context = event.context
        val screenW = context.guiWidth()
        val screenH = context.guiHeight()
        val now = System.currentTimeMillis()
        val toRemove = mutableListOf<Toast>()

        // Oldest first (top of stack)
        for ((i, toast) in toasts.withIndex()) {
            val age = now - toast.createdAtMs
            val totalLife = DISPLAY_MS + ANIM_MS * 2
            if (age > totalLife) {
                toRemove.add(toast)
                continue
            }
            val y = screenH - MARGIN - HEIGHT - i * (HEIGHT + GAP)
            // Slide animation
            val slideProgress = when {
                age < ANIM_MS -> age.toFloat() / ANIM_MS
                age > DISPLAY_MS + ANIM_MS -> 1f - ((age - DISPLAY_MS - ANIM_MS).toFloat() / ANIM_MS)
                else -> 1f
            }
            val slideT = easeOut(slideProgress.coerceIn(0f, 1f))
            val xOff = (WIDTH + 32) * (1f - slideT)
            val alpha = (255f * slideT.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)

            val x = screenW - MARGIN - WIDTH + xOff.toInt()
            val drawY = y

            drawToast(context, toast, x, drawY, alpha)
        }
        for (t in toRemove) toasts.remove(t)
    }

    private fun drawToast(
        context: GuiGraphicsExtractor,
        toast: Toast,
        x: Int,
        y: Int,
        alpha: Int,
    ) {
        val accent = severityColor(toast.severity)
        val accentA = Color4b(accent.r, accent.g, accent.b, alpha)
        val bg = Color4b(20, 20, 28, (220 * (alpha / 255f)).toInt().coerceIn(0, 255))
        val border = Color4b(50, 50, 64, (200 * (alpha / 255f)).toInt().coerceIn(0, 255))

        with(context) {
            // Background
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + WIDTH).toFloat(), (y + HEIGHT).toFloat(), 6f,
                fillColor = bg,
                outlineColor = border,
                outlineWidth = 1.0f,
            )
            // Accent left bar
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + 4).toFloat(), (y + HEIGHT).toFloat(), 1.5f,
                fillColor = accentA,
            )
            // Severity circle
            drawRoundedRect(
                (x + 12).toFloat(), (y + 12).toFloat(),
                (x + 12 + 32).toFloat(), (y + 12 + 32).toFloat(),
                16f,
                fillColor = Color4b(accent.r, accent.g, accent.b, (40 * (alpha / 255f)).toInt().coerceIn(0, 255)),
                outlineColor = accentA,
                outlineWidth = 1.5f,
            )
        }

        // Severity letter inside the circle
        val letter = severityLetter(toast.severity)
        val lw = mc.font.width(letter)
        val lx = x + 12 + (32 - lw) / 2
        val ly = y + 12 + (32 - 8) / 2
        context.text(
            mc.font, letter,
            lx, ly, accentA.argb, false,
        )

        // Title
        val tx = x + 12 + 32 + 10
        val ty = y + 12
        context.text(
            mc.font, toast.title,
            tx, ty,
            Color4b(240, 240, 245, alpha).argb, false,
        )
        // Message (truncate if too long)
        val msgMaxW = WIDTH - (tx - x) - 12
        val msg = truncate(toast.message, msgMaxW)
        context.text(
            mc.font, msg,
            tx, ty + 12,
            Color4b(180, 180, 195, alpha).argb, false,
        )

        if (toast == toasts.lastOrNull()) {
            // Subtle highlight border on the most recent toast
            with(context) {
                drawRoundedRect(
                    x.toFloat(), y.toFloat(),
                    (x + WIDTH).toFloat(), (y + HEIGHT).toFloat(), 6f,
                    fillColor = Color4b.TRANSPARENT,
                    outlineColor = Color4b(255, 255, 255, (40 * (alpha / 255f)).toInt().coerceIn(0, 255)),
                    outlineWidth = 1.0f,
                )
            }
        }
    }

    private fun severityColor(severity: NotificationEvent.Severity): Color4b = when (severity) {
        NotificationEvent.Severity.INFO -> Color4b(74, 143, 255, 255)
        NotificationEvent.Severity.SUCCESS -> Color4b(96, 200, 120, 255)
        NotificationEvent.Severity.ERROR -> Color4b(255, 90, 100, 255)
        NotificationEvent.Severity.ENABLED -> Color4b(96, 200, 120, 255)
        NotificationEvent.Severity.DISABLED -> Color4b(160, 160, 175, 255)
    }

    private fun severityLetter(severity: NotificationEvent.Severity): String = when (severity) {
        NotificationEvent.Severity.INFO -> "i"
        NotificationEvent.Severity.SUCCESS, NotificationEvent.Severity.ENABLED -> "v"
        NotificationEvent.Severity.ERROR -> "x"
        NotificationEvent.Severity.DISABLED -> "o"
    }

    private fun truncate(message: String, maxWidth: Int): String {
        if (mc.font.width(message) <= maxWidth) return message
        val ellipsis = "..."
        val avail = (maxWidth - mc.font.width(ellipsis)).coerceAtLeast(0)
        var cut = message.length
        while (cut > 0 && mc.font.width(message.substring(0, cut)) > avail) cut--
        return message.substring(0, cut) + ellipsis
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)
}
