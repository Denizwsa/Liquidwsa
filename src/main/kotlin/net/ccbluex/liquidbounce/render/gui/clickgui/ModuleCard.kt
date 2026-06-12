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
import net.ccbluex.liquidbounce.render.gui.clickgui.setting.GroupHeaderSetting
import net.ccbluex.liquidbounce.render.gui.clickgui.setting.createSetting
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth

class ModuleCard(val module: ClientModule) {

    var expanded: Boolean = false
    private var expandProgress: Float = 0f

    private val settings: List<Any> by lazy {
        buildSettingsList()
    }

    private fun buildSettingsList(): List<Any> {
        val out = mutableListOf<Any>()
        val visited = mutableSetOf<ValueGroup>()

        fun walk(parent: Iterable<Value<*>>, groupDepth: Int) {
            for (v in parent) {
                if (v.doNotInclude.asBoolean) continue
                if (v.notAnOption) continue
                if (v.name.equals("Bind", ignoreCase = true)) continue
                if (v.name.equals("Hidden", ignoreCase = true)) continue
                if (v is ModeValueGroup<*>) {
                    createSetting(v)?.let(out::add)
                    walk(v.modes, groupDepth)
                } else if (v is ValueGroup) {
                    if (visited.add(v)) {
                        if (groupDepth >= 0 && v.name != module.name) {
                            out.add(GroupHeaderSetting(v.name))
                        }
                        @Suppress("UNCHECKED_CAST")
                        walk(v.inner as Iterable<Value<*>>, groupDepth + 1)
                    }
                } else {
                    createSetting(v)?.let(out::add)
                }
            }
        }
        walk((module as Value<*>).inner as Iterable<Value<*>>, 0)
        return out
    }

    val expandedHeight: Int
        get() = settings.sumOf {
            when (it) {
                is GenericSetting -> it.height
                is GroupHeaderSetting -> it.height
                else -> 0
            }
        } + 8

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
                val h = when (setting) {
                    is GenericSetting -> setting.height
                    is GroupHeaderSetting -> setting.height
                    else -> continue
                }
                val hovered = mouseX in settingsX..(settingsX + settingsW) &&
                    mouseY in settingsY..(settingsY + h)
                val used = try {
                    when (setting) {
                        is GenericSetting -> setting.render(context, settingsX, settingsY, settingsW, mouseX, mouseY, partialTick, hovered)
                        is GroupHeaderSetting -> setting.render(context, settingsX, settingsY, settingsW, mouseX, mouseY, partialTick, hovered)
                        else -> h
                    }
                } catch (_: Exception) {
                    h
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
                val h = when (setting) {
                    is GenericSetting -> setting.height
                    is GroupHeaderSetting -> setting.height
                    else -> { settingsY += 0; continue }
                }
                if (setting is GenericSetting && mouseX in settingsX..(settingsX + settingsW) && mouseY in settingsY..(settingsY + h)) {
                    if (setting.mouseClicked(mouseX, mouseY, button)) return true
                }
                settingsY += h
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
            val h = when (setting) {
                is GenericSetting -> setting.height
                is GroupHeaderSetting -> setting.height
                else -> continue
            }
            if (setting is GenericSetting && mouseX in settingsX..(settingsX + settingsW) && mouseY in settingsY..(settingsY + h)) {
                if (setting.mouseReleased(mouseX, mouseY, button)) return true
            }
            settingsY += h
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
            val h = when (setting) {
                is GenericSetting -> setting.height
                is GroupHeaderSetting -> setting.height
                else -> continue
            }
            if (setting is GenericSetting && mouseX in settingsX..(settingsX + settingsW) && mouseY in settingsY..(settingsY + h)) {
                if (setting.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true
            }
            settingsY += h
        }
        return false
    }

    fun handleKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!expanded) return false
        for (setting in settings) {
            if (setting is GenericSetting && setting.keyPressed(keyCode, scanCode, modifiers)) return true
        }
        return false
    }

    fun handleCharTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!expanded) return false
        for (setting in settings) {
            if (setting is GenericSetting && setting.charTyped(codePoint, modifiers)) return true
        }
        return false
    }

    fun description(): String = runCatching { module.description.get() }.getOrNull().orEmpty()

    fun aliases(): List<String> = module.aliases
}
