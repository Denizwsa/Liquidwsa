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
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.gui.clickgui.setting.GenericSetting
import net.ccbluex.liquidbounce.render.gui.clickgui.setting.createSetting
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth

/**
 * One row in a [Panel] representing a single [ClientModule]. Tracks its
 * own expand/collapse state for the settings sub-panel and lazily creates
 * [GenericSetting] widgets the first time it opens.
 */
class ModuleElement(val module: ClientModule) {

    var expanded: Boolean = false

    private val settings: List<GenericSetting> by lazy {
        val out = mutableListOf<GenericSetting>()
        for (v in collectRenderableValues(module)) {
            createSetting(v)?.let(out::add)
        }
        out
    }

    private var expandProgress: Float = 0f

    val expandedHeight: Int
        get() = settings.sumOf { it.height } + (if (settings.isNotEmpty()) 4 else 0)

    val height: Int
        get() = ClickGuiTheme.moduleRowHeight + (expandProgress * expandedHeight).toInt()

    fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        isHighlighted: Boolean,
    ) {
        // Animate expand/collapse via partialTick-driven lerp
        val target = if (expanded) 1f else 0f
        val step = (partialTick.coerceAtLeast(0.0001f) * 1000f) / ClickGuiTheme.slideMs.coerceAtLeast(1)
        expandProgress = Mth.lerp(step.coerceIn(0f, 1f), expandProgress, target)
        if (expandProgress > 0.99f) expandProgress = 1f
        if (expandProgress < 0.01f) expandProgress = 0f

        val isHovered = mouseX in x..(x + width) && mouseY in y..(y + ClickGuiTheme.moduleRowHeight)
        context.fill(x, y, x + width, y + ClickGuiTheme.moduleRowHeight, ClickGuiTheme.moduleRowBg.argb)
        if (isHovered) {
            context.fill(x, y, x + width, y + ClickGuiTheme.moduleRowHeight, ClickGuiTheme.moduleHoverBg.argb)
        }
        if (isHighlighted) {
            context.fill(x, y, x + 2, y + ClickGuiTheme.moduleRowHeight, ClickGuiTheme.moduleHighlight.argb)
            context.fill(x + width - 2, y, x + width, y + ClickGuiTheme.moduleRowHeight, ClickGuiTheme.moduleHighlight.argb)
        }
        val color = when {
            module.enabled -> ClickGuiTheme.moduleEnabled
            isHovered -> ClickGuiTheme.textNormal
            else -> ClickGuiTheme.textDimmed
        }
        val textY = y + (ClickGuiTheme.moduleRowHeight - 8) / 2
        val name = spacedName(module.name)
        context.text(mc.font, name, x + 8, textY, color.argb, true)

        if (settings.isNotEmpty()) {
            val arrowCx = x + width - 8
            val arrowCy = y + ClickGuiTheme.moduleRowHeight / 2
            val s = ClickGuiTheme.expandArrowSize
            if (expandProgress > 0.5f) {
                context.fill(arrowCx - s.toInt(), arrowCy, arrowCx, arrowCy + 1, ClickGuiTheme.textDimmed.argb)
                context.fill(arrowCx - s.toInt() / 2, arrowCy - 1, arrowCx - s.toInt() / 2 + 1, arrowCy, ClickGuiTheme.textDimmed.argb)
            } else {
                context.fill(arrowCx - s.toInt(), arrowCy - 1, arrowCx, arrowCy, ClickGuiTheme.textDimmed.argb)
                context.fill(arrowCx - 1, arrowCy, arrowCx, arrowCy + s.toInt() / 2, ClickGuiTheme.textDimmed.argb)
            }
        }

        if (expandProgress > 0f) {
            val innerPad = 10
            val innerWidth = width - innerPad * 2
            val innerX = x + innerPad
            var sy = y + ClickGuiTheme.moduleRowHeight + (2 * expandProgress).toInt()
            // Full settings-panel background (covers all rows)
            val panelBottom = (sy + (settings.sumOf { it.height } * expandProgress).toInt()).coerceAtLeast(sy)
            context.fill(innerX - 2, sy, x + width - innerPad + 2, panelBottom, ClickGuiTheme.settingsPanelBg.argb)
            // Left accent bar
            context.fill(innerX - 2, sy, innerX, panelBottom, ClickGuiTheme.settingsBorder.argb)
            for (s in settings) {
                val hovered = mouseX in innerX..(innerX + innerWidth) && mouseY in sy..(sy + s.height)
                val used = try {
                    s.render(context, innerX, sy, innerWidth, mouseX, mouseY, partialTick, hovered)
                } catch (_: Exception) {
                    s.height
                }
                if (expandProgress < 1f) {
                    break
                }
                sy += used
            }
        }
    }

    fun handleMouseClick(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, button: Int): Boolean {
        if (mouseY in y..(y + ClickGuiTheme.moduleRowHeight) && mouseX in x..(x + width)) {
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
            val innerPad = 10
            val innerX = x + innerPad
            val innerWidth = width - innerPad * 2
            var sy = y + ClickGuiTheme.moduleRowHeight + 2
            for (s in settings) {
                if (mouseX in innerX..(innerX + innerWidth) && mouseY in sy..(sy + s.height)) {
                    if (s.mouseClicked(mouseX, mouseY, button)) return true
                }
                sy += s.height
            }
        }
        return false
    }

    fun handleMouseRelease(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, button: Int): Boolean {
        if (!expanded) return false
        val innerPad = 10
        val innerX = x + innerPad
        val innerWidth = width - innerPad * 2
        var sy = y + ClickGuiTheme.moduleRowHeight + 2
        var handled = false
        for (s in settings) {
            if (mouseX in innerX..(innerX + innerWidth) && mouseY in sy..(sy + s.height)) {
                if (s.mouseReleased(mouseX, mouseY, button)) handled = true
            }
            sy += s.height
        }
        return handled
    }

    fun handleMouseDrag(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        if (!expanded) return false
        val innerPad = 10
        val innerX = x + innerPad
        val innerWidth = width - innerPad * 2
        var sy = y + ClickGuiTheme.moduleRowHeight + 2
        var handled = false
        for (s in settings) {
            if (mouseX in innerX..(innerX + innerWidth) && mouseY in sy..(sy + s.height)) {
                if (s.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true
            }
            sy += s.height
        }
        return handled
    }

    fun handleKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!expanded) return false
        for (s in settings) {
            if (s.keyPressed(keyCode, scanCode, modifiers)) return true
        }
        return false
    }

    fun handleCharTyped(codePoint: Char, modifiers: Int): Boolean {
        if (!expanded) return false
        for (s in settings) {
            if (s.charTyped(codePoint, modifiers)) return true
        }
        return false
    }

    fun description(): String = runCatching { module.description.get() }.getOrNull().orEmpty()

    fun aliases(): List<String> = module.aliases

    companion object {
        /**
         * Walks a [ClientModule]'s value tree and yields the user-facing
         * [Value] objects in the order the Svelte theme rendered them:
         * top-level values first, then recursively into nested
         * [ModeValueGroup]s and excluded `Bind` / `Hidden` meta values.
         */
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

internal fun spacedName(name: String): String =
    name.replace("([a-z])([A-Z])".toRegex(), "$1 $2").replace("_", " ")
