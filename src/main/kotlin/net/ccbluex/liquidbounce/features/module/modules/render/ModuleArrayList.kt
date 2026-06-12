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
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.Font
import net.minecraft.util.Mth

@Suppress("MagicNumber")
object ModuleArrayList : ClientModule(
    name = "ArrayList",
    category = ModuleCategories.RENDER,
    state = true,
) {

    private val style by enumChoice("Style", Style.SIMPLE)
    private val side by enumChoice("Side", Side.LEFT)
    private val sort by enumChoice("Sort", Sort.WIDTH)
    private val upperCase by boolean("UpperCase", false)
    private val showTags by boolean("ShowTags", false)
    private val showLogo by boolean("ShowLogo", true)
    private val logoSize by int("LogoSize", 40, 16..80)
    private val glowEnabled by boolean("Glow", true)
    private val glowRadius by int("GlowRadius", 3, 1..8)
    private val yOffset by int("YOffset", 4, 0..200)
    private val lineHeight by int("LineHeight", 11, 8..24)
    private val fadeDuration by int("FadeDuration", 150, 0..500)
    private val primaryColor by color("PrimaryColor", Color4b(74, 143, 255, 255))
    private val bgColor by color("BgColor", Color4b(0, 0, 0, 80))
    private val textColor by color("TextColor", Color4b(255, 255, 255, 200))
    private val tagColor by color("TagColor", Color4b(170, 170, 170, 180))

    private enum class Style(override val tag: String) : Tagged {
        SIMPLE("Simple"),
        VAPE("Vape V4");
        override fun toString() = tag
    }

    private enum class Side(override val tag: String) : Tagged {
        LEFT("Left"),
        RIGHT("Right");
        override fun toString() = tag
    }

    private enum class Sort(override val tag: String) : Tagged {
        WIDTH("Width"),
        LENGTH("Length"),
        ALPHABET("Alphabet");
        override fun toString() = tag
    }

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
            .filter { it !== this && it.category != ModuleCategories.RENDER && !it.hidden }
            .toList()
        if (modules.isEmpty()) return@handler

        val step = if (fadeDuration > 0) dtSec / (fadeDuration / 1000f) else 10f
        for (m in modules) {
            val target = if (m.running) 1f else 0f
            val current = fadeState[m] ?: target
            fadeState[m] = when {
                current < target -> Mth.lerp(step.coerceIn(0f, 1f), current, target).coerceAtMost(target)
                current > target -> Mth.lerp(step.coerceIn(0f, 1f), current, target).coerceAtLeast(target)
                else -> current
            }
        }
        fadeState.entries.removeAll { (m, v) -> v <= 0.001f && !m.running }

        val visible = modules.filter { (fadeState[it] ?: 0f) > 0.01f && it.running }

        val sorted = when (sort) {
            Sort.WIDTH -> visible.sortedByDescending { font.width(moduleText(it)) }
            Sort.LENGTH -> visible.sortedByDescending { moduleText(it).length }
            Sort.ALPHABET -> visible.sortedBy { moduleText(it).lowercase() }
        }
        if (sorted.isEmpty()) return@handler

        when (style) {
            Style.SIMPLE -> renderSimple(context, sorted, font)
            Style.VAPE -> renderVape(context, sorted, font)
        }
    }

    private fun renderLogo(context: GuiGraphicsExtractor, x: Int, y: Int) {
        val font = mc.font
        val text = "LIQUIDWSA"
        val tw = font.width(text)
        val tsz = logoSize
        val scale = tsz.toFloat() / tw.coerceAtLeast(1)
        context.pose().withPush {
            context.pose().translate(x.toFloat(), y.toFloat())
            context.pose().scale(scale, scale)
            context.text(font, text, 0, 0, primaryColor.argb, true)
        }
    }

    private fun renderSimple(
        context: GuiGraphicsExtractor,
        sorted: List<ClientModule>,
        font: Font,
    ) {
        val screenWidth = context.guiWidth()
        val screenH = context.guiHeight()
        val margin = 2
        var y = yOffset.toFloat()

        if (showLogo) {
            renderLogo(context, margin, y.toInt())
            y += logoSize + 2
        }

        for (module in sorted) {
            val alpha = (fadeState[module] ?: 1f).coerceIn(0f, 1f)
            val name = moduleText(module)
            val tagStr = module.tag
            val tag = if (showTags && !tagStr.isNullOrBlank()) " $tagStr" else ""
            val fullText = "$name$tag"
            val tw = font.width(fullText)
            val nameW = font.width(name)

            val xText: Int
            val xBg0: Float
            val xBg1: Float

            when (side) {
                Side.RIGHT -> {
                    xText = screenWidth - tw - margin
                    xBg0 = (xText - 3).toFloat()
                    xBg1 = screenWidth.toFloat()
                }
                else -> {
                    xText = margin
                    xBg0 = (xText - 3).toFloat()
                    xBg1 = (xText + tw + 3).toFloat()
                }
            }

            val bgA = (bgColor.a * alpha).toInt().coerceIn(0, 255)
            val txA = (textColor.a * alpha).toInt().coerceIn(0, 255)
            val tgA = (tagColor.a * alpha).toInt().coerceIn(0, 255)

            if (glowEnabled && alpha > 0.05f) {
                val glowA = (45 * alpha).toInt().coerceIn(0, 255)
                val gc = primaryColor
                context.fill(
                    (xBg0 - glowRadius).toInt(), (y - 1f - glowRadius).toInt(),
                    (xBg1 + glowRadius).toInt(), (y + lineHeight - 1f + glowRadius).toInt(),
                    Color4b(gc.r, gc.g, gc.b, (glowA * 0.15f).toInt().coerceIn(0, 255)).argb,
                )
                context.fill(
                    (xBg0 - 1).toInt(), (y - 2f).toInt(),
                    (xBg1 + 1).toInt(), (y + lineHeight).toInt(),
                    Color4b(gc.r, gc.g, gc.b, (glowA * 0.35f).toInt().coerceIn(0, 255)).argb,
                )
            }

            context.fill(
                xBg0.toInt(), (y - 1f).toInt(),
                xBg1.toInt(), (y + lineHeight - 1f).toInt(),
                Color4b(bgColor.r, bgColor.g, bgColor.b, bgA).argb,
            )

            context.text(font, name, xText, y.toInt(),
                Color4b(textColor.r, textColor.g, textColor.b, txA).argb, true)

            if (tag.isNotBlank()) {
                context.text(font, tag.trim(), xText + nameW, y.toInt(),
                    Color4b(tagColor.r, tagColor.g, tagColor.b, tgA).argb, true)
            }

            y += lineHeight
            if (y > screenH) break
        }
    }

    private fun renderVape(
        context: GuiGraphicsExtractor,
        sorted: List<ClientModule>,
        font: Font,
    ) {
        val screenWidth = context.guiWidth()
        val screenH = context.guiHeight()
        val margin = 4
        var y = yOffset.toFloat()

        if (showLogo) {
            renderLogo(context, margin, y.toInt())
            y += logoSize + 2
        }

        // "VAPE" in bold orange, "V4" in bold yellow
        val vapePart = "VAPE "
        val v4Part = "V4"
        val logoFull = "$vapePart$v4Part"
        val logoWidth = font.width(logoFull)
        val logoX = screenWidth - logoWidth - margin

        val vapeW = font.width(vapePart)
        context.text(font, vapePart, logoX, y.toInt(),
            Color4b(255, 140, 0).argb, true)
        context.text(font, v4Part, logoX + vapeW, y.toInt(),
            Color4b(255, 220, 50).argb, true)

        y += lineHeight + 1

        for ((idx, module) in sorted.withIndex()) {
            val alpha = (fadeState[module] ?: 1f).coerceIn(0f, 1f)
            val name = moduleText(module)
            val tagStr = module.tag
            val tag = if (showTags && !tagStr.isNullOrBlank()) " $tagStr" else ""
            val fullText = "$name$tag"
            val tw = font.width(fullText)
            val nameW = font.width(name)

            val xText = screenWidth - tw - margin

            // Dark glow behind text (shadow effect)
            if (glowEnabled && alpha > 0.05f) {
                val glowA = (alpha * 180).toInt().coerceIn(0, 255)
                context.text(font, name, xText, y.toInt(),
                    Color4b(0, 0, 0, glowA).argb, false)
                if (tag.isNotBlank()) {
                    context.text(font, tag.trim(), xText + nameW, y.toInt(),
                        Color4b(0, 0, 0, glowA).argb, false)
                }
            }

            // Orange (top) to red (bottom) gradient per module
            val progress = idx.toFloat() / (sorted.size.coerceAtLeast(1) - 1).coerceAtLeast(1)
            val gradientR = 255
            val gradientG = (140 - progress * 100).toInt().coerceIn(0, 255)
            val gradientB = (30 - progress * 30).toInt().coerceIn(0, 255)
            val txA = (alpha * 255).toInt().coerceIn(0, 255)
            val moduleColor = Color4b(gradientR, gradientG, gradientB, txA)

            context.text(font, name, xText, y.toInt(),
                moduleColor.argb, true)

            if (tag.isNotBlank()) {
                val tgA = (alpha * 200).toInt().coerceIn(0, 255)
                context.text(font, tag.trim(), xText + nameW, y.toInt(),
                    Color4b(200, 160, 100, tgA).argb, true)
            }

            y += lineHeight
            if (y > screenH) break
        }
    }

    private fun moduleText(module: ClientModule): String {
        val base = module.name
        return if (upperCase) base.uppercase() else base
    }
}
