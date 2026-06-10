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

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.setting.GenericSetting
import net.ccbluex.liquidbounce.render.gui.clickgui.setting.createSetting
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth

class ModuleCard(val module: ClientModule) {

    var expanded: Boolean = false
    private var expandProgress: Float = 0f

    private val settings: List<GenericSetting> by lazy {
        val out = mutableListOf<GenericSetting>()
        for (v in collectRenderableValues(module)) {
            createSetting(v)?.let(out::add)
        }
        out
    }

    val expandedHeight: Int
        get() = settings.sumOf { it.height } + 8

    val fullHeight: Int
        get() = ClickGuiTheme.cardHeight + (expandProgress * expandedHeight).toInt()

    fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
    ) {
        val target = if (expanded) 1f else 0f
        val step = (partialTick.coerceAtLeast(0.0001f) * 1000f) / ClickGuiTheme.animSlideMs.coerceAtLeast(1)
        expandProgress = Mth.lerp(step.coerceIn(0f, 1f), expandProgress, target)
        if (expandProgress > 0.99f) expandProgress = 1f
        if (expandProgress < 0.01f) expandProgress = 0f

        val totalH = fullHeight
        val isHovered = mouseX in x..(x + width) && mouseY in y..(y + ClickGuiTheme.cardHeight)

        val bgColor = when {
            module.enabled -> ClickGuiTheme.bgCardEnabled
            isHovered -> ClickGuiTheme.bgCardHover
            else -> ClickGuiTheme.bgCard
        }
        val radius = ClickGuiTheme.cardRadius

        with(context) {
            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + width).toFloat(), (y + totalH).toFloat(),
                radius,
                fillColor = bgColor,
                outlineColor = if (isHovered && !module.enabled) ClickGuiTheme.borderLight else ClickGuiTheme.border,
                outlineWidth = 1f,
            )

            val toggleX = x + width - ClickGuiTheme.toggleWidth - 10
            val toggleY = y + (ClickGuiTheme.cardHeight - ClickGuiTheme.toggleHeight) / 2
            drawToggle(this@with, toggleX, toggleY, module.enabled)

            val nameX = x + 10
            val nameY = y + (ClickGuiTheme.cardHeight - 8) / 2
            text(
                mc.font, spacedName(module.name),
                nameX, nameY,
                if (module.enabled) ClickGuiTheme.accent.argb else ClickGuiTheme.textPrimary.argb,
                true
            )

            if (settings.isNotEmpty()) {
                val arrowX = toggleX - 16
                val arrowY = y + ClickGuiTheme.cardHeight / 2
                val arrowColor = if (expandProgress > 0.5f) ClickGuiTheme.accent else ClickGuiTheme.textDimmed
                val s = 4f
                if (expandProgress < 0.5f) {
                    fill((arrowX - s).toInt(), arrowY - 1, arrowX.toInt(), arrowY, arrowColor.argb)
                    fill(arrowX - 1, arrowY, arrowX, (arrowY + s / 2).toInt(), arrowColor.argb)
                } else {
                    fill((arrowX - s / 2).toInt(), arrowY - 1, (arrowX - s / 2 + 1).toInt(), arrowY, arrowColor.argb)
                    fill((arrowX - s).toInt(), arrowY, arrowX.toInt(), arrowY + 1, arrowColor.argb)
                }
            }
        }

        if (expandProgress > 0f && settings.isNotEmpty()) {
            val settingsX = x + 8
            val settingsW = width - 16
            var settingsY = y + ClickGuiTheme.cardHeight + 4

            for (setting in settings) {
                val hovered = mouseX in settingsX..(settingsX + settingsW) &&
                    mouseY in settingsY..(settingsY + setting.height)
                val used = try {
                    setting.render(context, settingsX, settingsY, settingsW, mouseX, mouseY, partialTick, hovered)
                } catch (_: Exception) {
                    setting.height
                }
                if (expandProgress < 1f) break
                settingsY += used
            }
        }
    }

    private fun drawToggle(context: GuiGraphicsExtractor, x: Int, y: Int, enabled: Boolean) {
        with(context) {
            val w = ClickGuiTheme.toggleWidth
            val h = ClickGuiTheme.toggleHeight
            val radius = h / 2f

            drawRoundedRect(
                x.toFloat(), y.toFloat(),
                (x + w).toFloat(), (y + h).toFloat(),
                radius,
                fillColor = if (enabled) ClickGuiTheme.toggleEnabled else ClickGuiTheme.toggleBg,
            )

            val knobRadius = (h - 4) / 2f
            val knobX = if (enabled) x + w - h + 2 else x + 2
            val knobY = y + 2
            drawRoundedRect(
                knobX.toFloat(), knobY.toFloat(),
                (knobX + knobRadius * 2).toFloat(), (knobY + knobRadius * 2).toFloat(),
                knobRadius,
                fillColor = ClickGuiTheme.toggleKnob,
            )
        }
    }

    fun handleMouseClick(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, button: Int): Boolean {
        if (mouseY !in y..(y + fullHeight)) return false
        if (mouseX !in x..(x + width)) return false

        if (mouseY in y..(y + ClickGuiTheme.cardHeight)) {
            if (button == 0) {
                module.enabled = !module.enabled
                return true
            }
            if (button == 1 && settings.isNotEmpty()) {
                expanded = !expanded
                return true
            }
        }

        if (expandProgress > 0.5f && expanded) {
            val settingsX = x + 8
            val settingsW = width - 16
            var settingsY = y + ClickGuiTheme.cardHeight + 4
            for (setting in settings) {
                if (mouseX in settingsX..(settingsX + settingsW) && mouseY in settingsY..(settingsY + setting.height)) {
                    if (setting.mouseClicked(mouseX, mouseY, button)) return true
                }
                settingsY += setting.height
            }
        }
        return false
    }

    fun handleMouseRelease(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, button: Int): Boolean {
        if (!expanded || expandProgress < 0.5f) return false
        if (mouseY !in y..(y + fullHeight)) return false
        val settingsX = x + 8
        val settingsW = width - 16
        var settingsY = y + ClickGuiTheme.cardHeight + 4
        for (setting in settings) {
            if (mouseX in settingsX..(settingsX + settingsW) && mouseY in settingsY..(settingsY + setting.height)) {
                if (setting.mouseReleased(mouseX, mouseY, button)) return true
            }
            settingsY += setting.height
        }
        return false
    }

    fun handleMouseDrag(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        if (!expanded || expandProgress < 0.5f) return false
        if (mouseY !in y..(y + fullHeight)) return false
        val settingsX = x + 8
        val settingsW = width - 16
        var settingsY = y + ClickGuiTheme.cardHeight + 4
        for (setting in settings) {
            if (mouseX in settingsX..(settingsX + settingsW) && mouseY in settingsY..(settingsY + setting.height)) {
                if (setting.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true
            }
            settingsY += setting.height
        }
        return false
    }

    fun handleKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!expanded) return false
        for (setting in settings) {
            if (setting.keyPressed(keyCode, scanCode, modifiers)) return true
        }
        return false
    }

    fun handleCharTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!expanded) return false
        for (setting in settings) {
            if (setting.charTyped(codePoint, modifiers)) return true
        }
        return false
    }

    fun description(): String = runCatching { module.description.get() }.getOrNull().orEmpty()

    fun aliases(): List<String> = module.aliases

    companion object {
        fun collectRenderableValues(module: ClientModule): List<Value<*>> {
            val out = mutableListOf<Value<*>>()
            fun walk(parent: Iterable<Value<*>>) {
                for (v in parent) {
                    if (v.doNotInclude.asBoolean) continue
                    if (v.notAnOption) continue
                    if (v.name.equals("Bind", ignoreCase = true)) continue
                    if (v.name.equals("Hidden", ignoreCase = true)) continue
                    if (v is ModeValueGroup<*>) {
                        out.add(v)
                        walk(v.modes)
                    } else if (v is ValueGroup) {
                        continue
                    } else {
                        out.add(v)
                    }
                }
            }
            walk((module as Value<*>).inner as Iterable<Value<*>>)
            return out
        }
    }
}
