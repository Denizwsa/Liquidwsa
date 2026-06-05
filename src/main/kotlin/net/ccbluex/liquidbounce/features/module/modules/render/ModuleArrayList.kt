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
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Mth

/**
 * Vanilla-style ArrayList HUD.
 *
 * Renders the names of all enabled, in-game modules on the configured side
 * of the screen. Each module row fades in/out with a configurable duration.
 * A [ColorScheme] preset can be selected to override the colors instantly.
 * Modules can be hidden individually via the [arrayListHidden] set.
 */
@Suppress("MagicNumber")
object ModuleArrayList : ClientModule(
    name = "ArrayList",
    category = ModuleCategories.RENDER,
    state = true,
) {

    private val colorScheme by enumChoice("ColorScheme", ColorScheme.LIQUID_BOUNCE)
    private val background by boolean("Background", true)
    private val outline by boolean("Outline", true)
    private val shadow by boolean("Shadow", true)
    private val side by enumChoice("Side", Side.RIGHT)
    private val sort by enumChoice("Sort", Sort.WIDTH)
    private val upperCase by boolean("UpperCase", false)
    private val textColor by color("Color", Color4b(255, 255, 255, 255))
    private val tagColor by color("TagColor", Color4b(170, 170, 170, 255))
    private val backgroundColor by color("BackgroundColor", Color4b(0, 0, 0, 110))
    private val outlineColor by color("OutlineColor", Color4b(0, 0, 0, 200))
    private val yOffset by int("YOffset", 4, 0..200)
    private val lineHeight by int("LineHeight", 11, 8..24)
    private val fadeAnimation by boolean("FadeAnimation", true)
    private val fadeDuration by int("FadeDuration", 200, 50..1000)
    private val showTags by boolean("ShowTags", true)

    private enum class Side(override val tag: String) : Tagged {
        LEFT("left"),
        RIGHT("right");

        override fun toString() = tag
    }

    private enum class Sort(override val tag: String) : Tagged {
        WIDTH("width"),
        ALPHABET("alphabet");

        override fun toString() = tag
    }

    /**
     * Built-in color presets. Selecting one of these overrides the
     * individual `textColor`/`tagColor`/`backgroundColor`/`outlineColor`
     * settings. The [ColorScheme.CUSTOM] option uses the user-defined colors.
     */
    private enum class ColorScheme(
        override val tag: String,
        val textColor: Color4b,
        val tagColor: Color4b,
        val backgroundColor: Color4b,
        val outlineColor: Color4b,
    ) : Tagged {
        LIQUID_BOUNCE(
            "LiquidBounce",
            Color4b(74, 143, 255, 255),
            Color4b(170, 170, 170, 255),
            Color4b(0, 0, 0, 110),
            Color4b(0, 0, 0, 200),
        ),
        RED(
            "Red",
            Color4b(255, 64, 96, 255),
            Color4b(255, 160, 170, 255),
            Color4b(40, 8, 16, 140),
            Color4b(80, 16, 32, 200),
        ),
        ORANGE(
            "Orange",
            Color4b(255, 144, 64, 255),
            Color4b(255, 200, 160, 255),
            Color4b(40, 24, 8, 140),
            Color4b(80, 48, 16, 200),
        ),
        CYAN(
            "Cyan",
            Color4b(64, 224, 255, 255),
            Color4b(180, 240, 255, 255),
            Color4b(8, 28, 40, 140),
            Color4b(16, 56, 80, 200),
        ),
        PINK(
            "Pink",
            Color4b(255, 96, 192, 255),
            Color4b(255, 192, 224, 255),
            Color4b(40, 12, 32, 140),
            Color4b(80, 24, 64, 200),
        ),
        CUSTOM(
            "Custom",
            Color4b(255, 255, 255, 255),
            Color4b(170, 170, 170, 255),
            Color4b(0, 0, 0, 110),
            Color4b(0, 0, 0, 200),
        );

        override fun toString() = tag
    }

    /**
     * Modules that should be excluded from the array list. Public so that
     * future ClickGUI or commands can toggle entries.
     */
    val arrayListHidden: MutableSet<ClientModule> = mutableSetOf()

    /** Per-module fade alpha. 0 = fully hidden, 1 = fully visible. */
    private val fadeState: MutableMap<ClientModule, Float> = mutableMapOf()

    private var lastFrameTime: Long = System.nanoTime()

    @Suppress("unused")
    private val refreshHandler = handler<RefreshArrayListEvent> {
        fadeState.keys.toList()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val context = event.context
        val font = mc.font
        val now = System.nanoTime()
        val dtSec = ((now - lastFrameTime).coerceAtLeast(0L) / 1_000_000_000.0).toFloat()
        lastFrameTime = now

        val modules = ModuleManager
            .filter { it !== this && !arrayListHidden.contains(it) }
            .toList()
        if (modules.isEmpty()) return@handler

        if (fadeAnimation) {
            val step = if (fadeDuration > 0) dtSec / (fadeDuration / 1000f) else 1f
            for (m in modules) {
                val target = if (m.running) 1f else 0f
                val current = fadeState[m] ?: target
                val next = if (current < target) {
                    Mth.lerp(step.coerceIn(0f, 1f), current, target).coerceAtMost(target)
                } else if (current > target) {
                    Mth.lerp(step.coerceIn(0f, 1f), current, target).coerceAtLeast(target)
                } else current
                fadeState[m] = next
            }
            fadeState.entries.removeAll { (m, v) -> v <= 0.001f && !m.running }
        } else {
            for (m in modules) {
                fadeState[m] = if (m.running) 1f else 0f
            }
            fadeState.keys.retainAll(modules)
        }

        val visible = modules.filter { (fadeState[it] ?: 0f) > 0.01f && it.running }

        val sorted = when (sort) {
            Sort.WIDTH -> visible.sortedByDescending { font.width(displayNameWithTag(it)) }
            Sort.ALPHABET -> visible.sortedBy { displayNameWithTag(it).lowercase() }
        }
        if (sorted.isEmpty()) return@handler

        // Resolve effective colors based on the active scheme
        val scheme = colorScheme
        val effectiveText = if (scheme == ColorScheme.CUSTOM) textColor else scheme.textColor
        val effectiveTag = if (scheme == ColorScheme.CUSTOM) tagColor else scheme.tagColor
        val effectiveBg = if (scheme == ColorScheme.CUSTOM) backgroundColor else scheme.backgroundColor
        val effectiveOutline = if (scheme == ColorScheme.CUSTOM) outlineColor else scheme.outlineColor

        val screenWidth = context.guiWidth()
        val margin = 4
        var y = yOffset

        for (module in sorted) {
            val alpha = (fadeState[module] ?: 1f).coerceIn(0f, 1f)
            val text = displayNameWithTag(module)
            val tag = if (showTags) module.tag else null
            val fullText = if (tag != null) "$text $tag" else text
            val textWidth = font.width(fullText)
            val xText: Int
            val xBgStart: Int
            val xBgEnd: Int
            when (side) {
                Side.RIGHT -> {
                    xText = screenWidth - textWidth - margin
                    xBgStart = xText - 3
                    xBgEnd = screenWidth
                }
                Side.LEFT -> {
                    xText = margin
                    xBgStart = xText - 3
                    xBgEnd = xText + textWidth + 3
                }
            }

            val bgAlpha = (effectiveBg.a * alpha).toInt().coerceIn(0, 255)
            val outAlpha = (effectiveOutline.a * alpha).toInt().coerceIn(0, 255)
            val textAlpha = (effectiveText.a * alpha).toInt().coerceIn(0, 255)
            val tagAlpha = (effectiveTag.a * alpha).toInt().coerceIn(0, 255)

            if (background) {
                with(context) {
                    drawRoundedRect(
                        xBgStart.toFloat(), y.toFloat() - 1f,
                        xBgEnd.toFloat(), (y + lineHeight - 1).toFloat(), 2f,
                        fillColor = Color4b(
                            effectiveBg.r, effectiveBg.g, effectiveBg.b, bgAlpha
                        ),
                    )
                }
            }
            if (outline) {
                with(context) {
                    drawRoundedRect(
                        xBgStart.toFloat(), y.toFloat() - 1f,
                        xBgEnd.toFloat(), (y + lineHeight - 1).toFloat(), 2f,
                        fillColor = Color4b.TRANSPARENT,
                        outlineColor = Color4b(
                            effectiveOutline.r, effectiveOutline.g, effectiveOutline.b, outAlpha
                        ),
                        outlineWidth = 1.0f,
                    )
                }
            }

            context.text(
                font, text,
                xText, y,
                Color4b(effectiveText.r, effectiveText.g, effectiveText.b, textAlpha).argb,
                shadow,
            )

            if (tag != null) {
                val nameW = font.width(text + " ")
                val tagX = xText + nameW
                context.text(
                    font, tag,
                    tagX, y,
                    Color4b(effectiveTag.r, effectiveTag.g, effectiveTag.b, tagAlpha).argb,
                    shadow,
                )
            }

            y += lineHeight
            if (y > context.guiHeight() - lineHeight) break
        }
    }

    private fun displayNameWithTag(module: ClientModule): String {
        val base = module.name
        return if (upperCase) base.uppercase() else base
    }
}
