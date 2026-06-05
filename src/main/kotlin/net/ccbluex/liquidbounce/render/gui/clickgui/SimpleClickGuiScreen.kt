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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.types.BindValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Vanilla-styled ClickGui with a tab-based layout.
 *
 *   +--------------------------------------------+
 *   | [Combat] [Movement] [Player] [Render] ...   |  <- category tabs
 *   +--------------------+-----------------------+
 *   | Modules            | Settings              |
 *   | - KillAura  [ON]   | - Range       3.5     |
 *   | - AutoClick [OFF]  | - Mode        Switch  |
 *   | - Criticals [ON]   | v Rotations           |
 *   | ...                |   - Yaw  3.0          |
 *   |                    |   - Pitch 5.0         |
 *   +--------------------+-----------------------+
 *   | Hint: Left=expand, Right=toggle, Wheel=... |
 *   +--------------------------------------------+
 *
 * Settings support:
 *  - boolean toggles
 *  - ranged (slider)
 *  - single choice (cycle on click)
 *  - multi choice (expandable list of toggles)
 *  - bind capture
 *  - nested ValueGroup / ToggleableValueGroup (expandable, with own state)
 *
 * Mouse wheel scrolls the pane under the cursor. Right-click on a module
 * toggles it. Left-click expands it.
 */
class SimpleClickGuiScreen(
    private val previous: Screen? = mc.screen,
) : Screen(Component.literal("ClickGui")) {

    private val categories: List<ModuleCategory> = listOf(
        ModuleCategories.COMBAT,
        ModuleCategories.MOVEMENT,
        ModuleCategories.PLAYER,
        ModuleCategories.RENDER,
        ModuleCategories.WORLD,
        ModuleCategories.EXPLOIT,
        ModuleCategories.MISC,
        ModuleCategories.FUN,
    )

    private var selectedCategory: ModuleCategory = categories.first()
    private var expandedModule: ClientModule? = null

    private var moduleScroll: Int = 0
    private var settingsScroll: Int = 0

    private var binding: Value<*>? = null

    // Expandable state: identity-based to be stable across rebuilds.
    private val expandedGroups: MutableSet<Int> = mutableSetOf()
    private val expandedMultiChoices: MutableSet<Int> = mutableSetOf()

    // Layout constants
    private val headerHeight: Int = 28
    private val footerHeight: Int = 22
    private val tabH: Int = 20
    private val rowH: Int = 22
    private val indentWidth: Int = 12

    private val moduleListLeft: Int get() = 8
    private val moduleListWidth: Int get() = (this.width * 0.42f).toInt()
    private val moduleListRight: Int get() = moduleListLeft + moduleListWidth

    private val settingsListLeft: Int get() = moduleListRight + 8
    private val settingsListRight: Int get() = this.width - 8

    // Settings pane rendering model. Each entry occupies `height` rows and
    // has a y-offset that the renderer uses to place the row.
    private sealed class SettingsEntry {
        abstract val height: Int
    }

    private class GroupHeaderEntry(
        val group: ValueGroup,
        val toggleable: Boolean,
        val enabled: Boolean,
    ) : SettingsEntry() {
        override val height: Int = 1
    }

    private class SettingsValueEntry(
        val value: Value<*>,
        val depth: Int,
        val isBind: Boolean,
    ) : SettingsEntry() {
        override val height: Int = 1
    }

    private class MultiChoiceEntry(
        val value: MultiChoiceListValue<*>,
        val depth: Int,
        val expanded: Boolean,
    ) : SettingsEntry() {
        override val height: Int = 1
    }

    private class MultiChoiceOptionEntry(
        val ownerId: Int,
        val option: Tagged,
        val depth: Int,
        val selected: Boolean,
    ) : SettingsEntry() {
        override val height: Int = 1
    }

    private class ModeGroupEntry(
        val value: ModeValueGroup<*>,
        val depth: Int,
        val activeModeName: String,
        val expanded: Boolean,
    ) : SettingsEntry() {
        override val height: Int = 1
    }

    // Cached click targets: maps y-offset (rowH units from top) to the entry
    // drawn at that row. Computed once per draw pass for both hit-testing and
    // rendering.
    private data class PlacedEntry(val entry: SettingsEntry, val rowIndex: Int)
    private var placed: List<PlacedEntry> = emptyList()

    // Colors
    private val panelBg = Color4b(20, 20, 24, 220)
    private val rowHover = Color4b(50, 50, 60, 200)
    private val rowIdle = Color4b(28, 28, 34, 180)
    private val rowRunning = Color4b(48, 90, 56, 200)
    private val rowExpanded = Color4b(48, 78, 156, 220)
    private val textColor = Color4b(220, 220, 225)
    private val textMuted = Color4b(150, 150, 160)
    private val textAccent = Color4b(140, 200, 255)
    private val tabActive = Color4b(80, 130, 230, 240)
    private val tabHover = Color4b(60, 60, 70, 230)
    private val tabIdle = Color4b(35, 35, 42, 220)
    private val groupHeader = Color4b(38, 38, 46, 230)
    private val groupHeaderHover = Color4b(60, 60, 80, 230)

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui.enabled = false
        mc.setScreen(previous)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (binding != null) {
            if (event.key == InputConstants.KEY_ESCAPE) {
                binding = null
                return true
            }
            val v = binding
            if (v is BindValue) {
                val key = InputConstants.Type.KEYSYM.getOrCreate(event.key)
                v.set(InputBind(key, InputBind.BindAction.TOGGLE, emptySet()))
                binding = null
            }
            return true
        }
        if (event.key == InputConstants.KEY_ESCAPE) {
            if (expandedModule != null) {
                expandedModule = null
                settingsScroll = 0
            } else {
                onClose()
            }
            return true
        }
        return super.keyPressed(event)
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        drawTabs(context, mouseX, mouseY)

        val top = headerHeight + 4
        val bottom = this.height - footerHeight

        // Module list
        with(context) {
            drawRoundedRect(
                moduleListLeft.toFloat(), top.toFloat(),
                moduleListRight.toFloat(), bottom.toFloat(), 3f,
                fillColor = panelBg,
            )
        }
        drawText(context, selectedCategory.tag.replaceFirstChar { it.titlecase() },
            (moduleListLeft + 6).toFloat(), (top - 14).toFloat(), textMuted)
        drawModuleList(context, mouseX, mouseY, top, bottom)

        // Settings
        with(context) {
            drawRoundedRect(
                settingsListLeft.toFloat(), top.toFloat(),
                settingsListRight.toFloat(), bottom.toFloat(), 3f,
                fillColor = panelBg,
            )
        }
        drawText(context, "Settings", (settingsListLeft + 6).toFloat(),
            (top - 14).toFloat(), textMuted)
        drawSettings(context, mouseX, mouseY, top, bottom)

        // Footer hint
        val hintY = this.height - footerHeight + 6
        drawText(context, "Left=expand  Right=toggle  Wheel=scroll  ESC=back",
            8f, hintY.toFloat(), textMuted)
        val rightHint = "Right Shift to close"
        val rw = mc.font.width(rightHint)
        drawText(context, rightHint, (this.width - 8 - rw).toFloat(), hintY.toFloat(), textMuted)
    }

    // --- Tabs ---

    private fun drawTabs(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        val tabW = 80
        val gap = 2
        val totalW = categories.size * (tabW + gap) - gap
        val startX = (this.width - totalW) / 2
        for ((index, cat) in categories.withIndex()) {
            val tx = startX + index * (tabW + gap)
            val ty = 4
            val isSelected = cat == selectedCategory
            val isHover = mouseX in tx..(tx + tabW) && mouseY in ty..(ty + tabH)
            val color = when {
                isSelected -> tabActive
                isHover -> tabHover
                else -> tabIdle
            }
            with(context) {
                drawRoundedRect(
                    tx.toFloat(), ty.toFloat(),
                    (tx + tabW).toFloat(), (ty + tabH).toFloat(), 3f,
                    fillColor = color,
                )
            }
            val label = cat.tag.replaceFirstChar { it.titlecase() }
            val color4 = if (isSelected) Color4b.WHITE else textColor
            val tw = mc.font.width(label)
            drawText(context, label,
                (tx + (tabW - tw) / 2).toFloat(),
                (ty + (tabH - 8) / 2).toFloat(),
                color4)
        }
    }

    // --- Module list ---

    private fun drawModuleList(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        top: Int,
        bottom: Int,
    ) {
        val modules = ModuleManager
            .filter { it.category == selectedCategory }
            .sortedBy { it.name.lowercase() }
        if (modules.isEmpty()) {
            drawText(context, "No modules",
                (moduleListLeft + 10).toFloat(), (top + 8).toFloat(), textMuted)
            return
        }

        val visible = maxVisible(top, bottom)
        val totalRows = modules.size
        val maxScroll = ((totalRows - visible).coerceAtLeast(0) * rowH)
        if (moduleScroll > maxScroll) moduleScroll = maxScroll
        if (moduleScroll < 0) moduleScroll = 0

        val firstVisible = moduleScroll / rowH
        for (i in 0 until visible) {
            val moduleIndex = firstVisible + i
            if (moduleIndex !in modules.indices) break
            val module = modules[moduleIndex]
            val y = top + i * rowH
            val isExpanded = expandedModule == module
            val isHover = mouseX in moduleListLeft..moduleListRight && mouseY in y..(y + rowH - 2)
            val color = when {
                isExpanded -> rowExpanded
                module.running -> rowRunning
                isHover -> rowHover
                else -> rowIdle
            }
            with(context) {
                drawRoundedRect(
                    (moduleListLeft + 2).toFloat(), y.toFloat(),
                    (moduleListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                    fillColor = color,
                )
            }
            val statusLabel = if (module.running) "ON" else "OFF"
            val statusColor = if (module.running) Color4b(140, 255, 160) else textMuted
            drawText(context, module.name,
                (moduleListLeft + 8).toFloat(), (y + 7).toFloat(),
                if (module.running) Color4b.WHITE else textColor)
            val sw = mc.font.width(statusLabel)
            drawText(context, statusLabel,
                (moduleListRight - 8 - sw).toFloat(), (y + 7).toFloat(), statusColor)
        }

        if (totalRows > visible) {
            val trackH = bottom - top
            val thumbH = ((visible.toFloat() / totalRows) * trackH).coerceAtLeast(20f)
            val thumbY = top + (moduleScroll.toFloat() / maxScroll) * (trackH - thumbH)
            with(context) {
                drawRoundedRect(
                    (moduleListRight - 4).toFloat(), thumbY,
                    (moduleListRight - 1).toFloat(), thumbY + thumbH, 1f,
                    fillColor = Color4b(120, 120, 130, 200),
                )
            }
        }
    }

    // --- Settings ---

    private fun drawSettings(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        top: Int,
        bottom: Int,
    ) {
        val module = expandedModule
        if (module == null) {
            drawText(context, "Select a module",
                (settingsListLeft + 10).toFloat(), (top + 8).toFloat(), textMuted)
            placed = emptyList()
            return
        }

        // Walk the entire module, expanding groups and multi-choices where
        // requested, and place each row on the canvas.
        val entries = buildEntries(module)
        val totalRows = entries.sumOf { it.height }
        val visible = maxVisible(top, bottom)
        val maxScroll = ((totalRows - visible).coerceAtLeast(0) * rowH)
        if (settingsScroll > maxScroll) settingsScroll = maxScroll
        if (settingsScroll < 0) settingsScroll = 0
        val firstIndex = settingsScroll / rowH

        val placedMutable = mutableListOf<PlacedEntry>()
        var row = 0
        for (e in entries) {
            val y = top + (row - firstIndex) * rowH
            placedMutable.add(PlacedEntry(e, row))
            when (e) {
                is GroupHeaderEntry -> drawGroupHeader(context, mouseX, mouseY, y, e)
                is SettingsValueEntry -> drawValueEntry(context, mouseX, mouseY, y, e)
                is MultiChoiceEntry -> drawMultiChoiceHeader(context, mouseX, mouseY, y, e)
                is MultiChoiceOptionEntry -> drawMultiChoiceOption(context, mouseX, mouseY, y, e)
                is ModeGroupEntry -> drawModeGroupHeader(context, mouseX, mouseY, y, e)
            }
            row += e.height
        }
        placed = placedMutable

        if (totalRows > visible) {
            val trackH = bottom - top
            val thumbH = ((visible.toFloat() / totalRows) * trackH).coerceAtLeast(20f)
            val thumbY = top + (settingsScroll.toFloat() / maxScroll) * (trackH - thumbH)
            with(context) {
                drawRoundedRect(
                    (settingsListRight - 4).toFloat(), thumbY,
                    (settingsListRight - 1).toFloat(), thumbY + thumbH, 1f,
                    fillColor = Color4b(120, 120, 130, 200),
                )
            }
        }
    }

    private fun buildEntries(module: ClientModule): List<SettingsEntry> {
        val out = mutableListOf<SettingsEntry>()
        walk(module, depth = 0, out)
        return out
    }

    private fun walk(group: ValueGroup, depth: Int, out: MutableList<SettingsEntry>) {
        for (value in group.inner) {
            when (value) {
                is ModeValueGroup<*> -> {
                    val id = System.identityHashCode(value)
                    val expanded = expandedGroups.contains(id)
                    out.add(ModeGroupEntry(value, depth, value.activeMode.name, expanded))
                    if (expanded) {
                        walk(value.activeMode, depth + 1, out)
                    }
                }
                is ValueGroup -> {
                    val toggleable = value is ToggleableValueGroup
                    val enabled = if (toggleable) {
                        (value as ToggleableValueGroup).enabled
                    } else true
                    out.add(GroupHeaderEntry(value, toggleable, enabled))
                    if (expandedGroups.contains(System.identityHashCode(value))) {
                        walk(value, depth + 1, out)
                    }
                }
                is BindValue -> {
                    out.add(SettingsValueEntry(value, depth, isBind = true))
                }
                is MultiChoiceListValue<*> -> {
                    val id = System.identityHashCode(value)
                    val expanded = expandedMultiChoices.contains(id)
                    out.add(MultiChoiceEntry(value, depth, expanded))
                    if (expanded) {
                        for (option in value.choices) {
                            val selected = (value.get() as Collection<*>).any {
                                (it as? Tagged)?.tag == option.tag
                            }
                            out.add(MultiChoiceOptionEntry(id, option, depth + 1, selected))
                        }
                    }
                }
                else -> {
                    out.add(SettingsValueEntry(value, depth, isBind = false))
                }
            }
        }
    }

    private fun drawGroupHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: GroupHeaderEntry,
    ) {
        val isHover = mouseX in settingsListLeft..settingsListRight &&
            mouseY in y..(y + rowH - 2)
        val bg = if (isHover) groupHeaderHover else groupHeader
        with(context) {
            drawRoundedRect(
                (settingsListLeft + 2).toFloat(), y.toFloat(),
                (settingsListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                fillColor = bg,
            )
        }
        val id = System.identityHashCode(entry.group)
        val expanded = expandedGroups.contains(id)
        val arrow = if (expanded) "v" else ">"
        drawText(context, arrow,
            (settingsListLeft + 6).toFloat(), (y + 7).toFloat(),
            textAccent)
        val label = if (entry.toggleable) {
            "${entry.group.name}: " + (if (entry.enabled) "ON" else "OFF")
        } else {
            entry.group.name
        }
        val nameColor = when {
            entry.toggleable && entry.enabled -> Color4b.WHITE
            else -> textColor
        }
        drawText(context, label,
            (settingsListLeft + 6 + indentWidth).toFloat(),
            (y + 7).toFloat(),
            nameColor)
    }

    private fun drawValueEntry(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: SettingsValueEntry,
    ) {
        val isHover = mouseX in settingsListLeft..settingsListRight &&
            mouseY in y..(y + rowH - 2)
        when (val value = entry.value) {
            is BindValue -> {
                val label = "Bind: " + (value.get().keyName.takeIf { it.isNotEmpty() } ?: "None")
                val isBinding = binding == value
                drawStandardRow(context, y, isHover || isBinding, label, isBinding, isHover)
            }
            else -> drawSingleValue(context, mouseX, mouseY, y, value, isHover, entry.depth)
        }
    }

    private fun drawMultiChoiceHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: MultiChoiceEntry,
    ) {
        val isHover = mouseX in settingsListLeft..settingsListRight &&
            mouseY in y..(y + rowH - 2)
        val bg = if (isHover) rowHover else rowIdle
        with(context) {
            drawRoundedRect(
                (settingsListLeft + 2 + entry.depth * 4).toFloat(), y.toFloat(),
                (settingsListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                fillColor = bg,
            )
        }
        val arrow = if (entry.expanded) "v" else ">"
        val count = (entry.value.get() as Collection<*>).size
        val label = "${entry.value.name}: $count/${entry.value.choices.size}"
        drawText(context, arrow,
            (settingsListLeft + 6 + entry.depth * 4).toFloat(), (y + 7).toFloat(), textAccent)
        drawText(context, label,
            (settingsListLeft + 6 + entry.depth * 4 + indentWidth).toFloat(),
            (y + 7).toFloat(),
            Color4b.WHITE)
    }

    private fun drawMultiChoiceOption(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: MultiChoiceOptionEntry,
    ) {
        val isHover = mouseX in settingsListLeft..settingsListRight &&
            mouseY in y..(y + rowH - 2)
        val bg = when {
            isHover -> rowHover
            entry.selected -> rowRunning
            else -> rowIdle
        }
        with(context) {
            drawRoundedRect(
                (settingsListLeft + 2 + entry.depth * 4).toFloat(), y.toFloat(),
                (settingsListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                fillColor = bg,
            )
        }
        val mark = if (entry.selected) "[x]" else "[ ]"
        drawText(context, mark,
            (settingsListLeft + 6 + entry.depth * 4).toFloat(),
            (y + 7).toFloat(),
            if (entry.selected) Color4b(140, 255, 160) else textMuted)
        drawText(context, entry.option.tag,
            (settingsListLeft + 6 + entry.depth * 4 + 24).toFloat(),
            (y + 7).toFloat(),
            if (entry.selected) Color4b.WHITE else textColor)
    }

    private fun drawModeGroupHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: ModeGroupEntry,
    ) {
        val isHover = mouseX in settingsListLeft..settingsListRight &&
            mouseY in y..(y + rowH - 2)
        val bg = if (isHover) rowHover else rowIdle
        with(context) {
            drawRoundedRect(
                (settingsListLeft + 2 + entry.depth * 4).toFloat(), y.toFloat(),
                (settingsListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                fillColor = bg,
            )
        }
        val arrow = if (entry.expanded) "v" else ">"
        val label = "${entry.value.name}: ${entry.activeModeName}"
        drawText(context, arrow,
            (settingsListLeft + 6 + entry.depth * 4).toFloat(), (y + 7).toFloat(), textAccent)
        drawText(context, label,
            (settingsListLeft + 6 + entry.depth * 4 + indentWidth).toFloat(),
            (y + 7).toFloat(),
            Color4b.WHITE)
    }

    private fun drawStandardRow(
        context: GuiGraphicsExtractor,
        y: Int,
        forceHover: Boolean,
        label: String,
        accent: Boolean,
        isHover: Boolean,
    ) {
        val bg = if (forceHover || isHover) rowHover else rowIdle
        with(context) {
            drawRoundedRect(
                (settingsListLeft + 2).toFloat(), y.toFloat(),
                (settingsListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                fillColor = bg,
            )
        }
        val vw = mc.font.width(label.substringAfter(": "))
        val name = label.substringBefore(": ")
        val nameColor = if (accent) Color4b.WHITE else textMuted
        drawText(context, name, (settingsListLeft + 8).toFloat(), (y + 7).toFloat(), nameColor)
        if (label.contains(": ")) {
            drawText(context, label.substringAfter(": "),
                (settingsListRight - 8 - vw).toFloat(), (y + 7).toFloat(),
                if (accent) textAccent else textMuted)
        }
    }

    private fun drawSingleValue(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        value: Value<*>,
        isHover: Boolean,
        depth: Int,
    ) {
        val bg = if (isHover) rowHover else rowIdle
        with(context) {
            drawRoundedRect(
                (settingsListLeft + 2 + depth * 4).toFloat(), y.toFloat(),
                (settingsListRight - 2).toFloat(), (y + rowH - 2).toFloat(), 2f,
                fillColor = bg,
            )
        }
        when {
            value.valueType == ValueType.BOOLEAN -> {
                val on = (value.get() as? Boolean) ?: false
                drawText(context, value.name,
                    (settingsListLeft + 8 + depth * 4).toFloat(),
                    (y + 7).toFloat(),
                    if (on) Color4b.WHITE else textColor)
                val stateLabel = if (on) "ON" else "OFF"
                val sw = mc.font.width(stateLabel)
                drawText(context, stateLabel,
                    (settingsListRight - 8 - sw).toFloat(),
                    (y + 7).toFloat(),
                    if (on) Color4b(140, 255, 160) else textMuted)
            }
            value is RangedValue<*> -> {
                val range = value.range
                val start = when (val s: Any = range.start) {
                    is Int -> s.toDouble(); is Float -> s.toDouble(); is Double -> s
                    else -> 0.0
                }
                val end = when (val e: Any = range.endInclusive) {
                    is Int -> e.toDouble(); is Float -> e.toDouble(); is Double -> e
                    else -> 1.0
                }
                val current = when (val c = value.get()) {
                    is Int -> c.toDouble(); is Float -> c.toDouble(); is Double -> c
                    else -> start
                }
                val isInt = value.valueType == ValueType.INT
                val valueText = if (isInt) current.toInt().toString() else "%.2f".format(current)
                drawText(context, value.name,
                    (settingsListLeft + 8 + depth * 4).toFloat(),
                    (y + 7).toFloat(),
                    Color4b.WHITE)
                val vw = mc.font.width(valueText)
                drawText(context, valueText,
                    (settingsListRight - 8 - vw).toFloat(),
                    (y + 7).toFloat(),
                    textAccent)
                // Slider track
                val trackX1 = (settingsListLeft + 8 + depth * 4).toFloat()
                val trackX2 = (settingsListRight - 8).toFloat()
                val trackY = (y + 18).toFloat()
                with(context) {
                    drawRoundedRect(trackX1, trackY - 1f, trackX2, trackY + 2f, 1f,
                        fillColor = Color4b(60, 60, 70, 255))
                }
                if (end > start) {
                    val ratio = ((current - start) / (end - start)).coerceIn(0.0, 1.0)
                    with(context) {
                        drawRoundedRect(trackX1, trackY - 1f,
                            trackX1 + (trackX2 - trackX1) * ratio.toFloat(), trackY + 2f, 1f,
                            fillColor = Color4b(80, 130, 230, 255))
                    }
                }
            }
            value is ChoiceListValue<*> -> {
                val current = value.get()
                val tag = (current as? Tagged)?.tag ?: current.toString()
                drawText(context, value.name,
                    (settingsListLeft + 8 + depth * 4).toFloat(),
                    (y + 7).toFloat(),
                    Color4b.WHITE)
                val tw = mc.font.width(tag)
                drawText(context, tag,
                    (settingsListRight - 8 - tw).toFloat(),
                    (y + 7).toFloat(),
                    textAccent)
            }
            else -> {
                val raw = value.get().toString()
                drawText(context, value.name,
                    (settingsListLeft + 8 + depth * 4).toFloat(),
                    (y + 7).toFloat(),
                    textColor)
                val vw = mc.font.width(raw)
                drawText(context, raw,
                    (settingsListRight - 8 - vw).toFloat(),
                    (y + 7).toFloat(),
                    textMuted)
            }
        }
    }

    private fun minOrZero(v: Double) = v

    // --- Hit testing ---

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        val button = event.button()

        val tabW = 80
        val gap = 2
        val totalW = categories.size * (tabW + gap) - gap
        val startX = (this.width - totalW) / 2
        if (my in 4..(4 + tabH)) {
            val rel = mx - startX
            if (rel >= 0) {
                val index = rel / (tabW + gap)
                if (index in categories.indices && rel % (tabW + gap) <= tabW) {
                    if (selectedCategory != categories[index]) {
                        selectedCategory = categories[index]
                        expandedModule = null
                        moduleScroll = 0
                        settingsScroll = 0
                    }
                    return true
                }
            }
        }

        val top = headerHeight + 4
        val bottom = this.height - footerHeight

        if (mx in moduleListLeft..moduleListRight && my in top..bottom) {
            val modules = ModuleManager
                .filter { it.category == selectedCategory }
                .sortedBy { it.name.lowercase() }
            if (modules.isNotEmpty()) {
                val visible = maxVisible(top, bottom)
                val firstVisible = moduleScroll / rowH
                val localY = my - top
                val rowIndex = firstVisible + (localY / rowH)
                if (rowIndex in modules.indices) {
                    val module = modules[rowIndex]
                    if (button == 0) {
                        expandedModule = if (expandedModule == module) null else module
                        settingsScroll = 0
                    } else if (button == 1) {
                        module.enabled = !module.enabled
                    }
                    return true
                }
            }
            return true
        }

        if (mx in settingsListLeft..settingsListRight && my in top..bottom) {
            val localY = my - top
            val rowIndex = (settingsScroll + localY) / rowH
            val placedEntry = placed.firstOrNull { it.rowIndex == rowIndex }
            if (placedEntry != null) {
                handleSettingsClick(placedEntry.entry, button)
                return true
            }
            return true
        }

        return super.mouseClicked(event, doubleClick)
    }

    private fun handleSettingsClick(entry: SettingsEntry, button: Int) {
        when (entry) {
            is GroupHeaderEntry -> {
                if (button == 0) {
                    val id = System.identityHashCode(entry.group)
                    if (!expandedGroups.add(id)) expandedGroups.remove(id)
                } else if (button == 1 && entry.toggleable) {
                    (entry.group as ToggleableValueGroup).enabled = !entry.enabled
                }
            }
            is SettingsValueEntry -> {
                val v = entry.value
                if (entry.isBind) {
                    if (button == 0) {
                        binding = if (binding == v) null else v
                    }
                } else {
                    handleValueClick(v, button)
                }
            }
            is MultiChoiceEntry -> {
                if (button == 0) {
                    val id = System.identityHashCode(entry.value)
                    if (!expandedMultiChoices.add(id)) expandedMultiChoices.remove(id)
                }
            }
            is MultiChoiceOptionEntry -> {
                if (button == 0) {
                    toggleMultiChoiceOption(entry.ownerId, entry.option)
                }
            }
            is ModeGroupEntry -> {
                if (button == 0) {
                    val modeGroup = entry.value
                    val modes = modeGroup.getModeStrings()
                    if (modes.isNotEmpty()) {
                        val currentIdx = modes.indexOf(entry.activeModeName)
                        val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % modes.size
                        modeGroup.setByString(modes[nextIdx])
                    }
                } else if (button == 1) {
                    val id = System.identityHashCode(entry.value)
                    if (!expandedGroups.add(id)) expandedGroups.remove(id)
                }
            }
        }
    }

    private fun handleValueClick(value: Value<*>, button: Int) {
        when {
            value.valueType == ValueType.BOOLEAN -> {
                @Suppress("UNCHECKED_CAST")
                (value as Value<Boolean>).set(!(value.get() as Boolean))
            }
            value is RangedValue<*> -> {
                // Sliders are set by drag. Single click does nothing.
            }
            value is ChoiceListValue<*> -> {
                val choices = value.choices.toList()
                if (choices.isNotEmpty()) {
                    val current = value.get()
                    val idx = choices.indexOf(current)
                    val next = if (button == 0) {
                        (idx + 1).mod(choices.size)
                    } else {
                        (idx - 1 + choices.size).mod(choices.size)
                    }
                    @Suppress("UNCHECKED_CAST")
                    (value as Value<Any>).set(choices[next])
                }
            }
        }
    }

    private fun toggleMultiChoiceOption(ownerId: Int, option: Tagged) {
        val entry = placed.firstOrNull { it.entry is MultiChoiceEntry &&
            System.identityHashCode((it.entry as MultiChoiceEntry).value) == ownerId }
        val mc = (entry?.entry as? MultiChoiceEntry)?.value ?: return
        val current = (mc.get() as Collection<*>)
        val list = current.toMutableList()
        val existingIdx = list.indexOfFirst { (it as? Tagged)?.tag == option.tag }
        if (existingIdx >= 0) {
            list.removeAt(existingIdx)
        } else {
            list.add(option)
        }
        @Suppress("UNCHECKED_CAST")
        (mc as Value<Any>).set(list)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
    ): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val top = headerHeight + 4
        val bottom = this.height - footerHeight
        if (my !in top..bottom) return false

        return if (mx in moduleListLeft..moduleListRight) {
            val modules = ModuleManager
                .filter { it.category == selectedCategory }
            val visible = maxVisible(top, bottom)
            val max = ((modules.size - visible).coerceAtLeast(0) * rowH)
            moduleScroll = (moduleScroll - (deltaY * rowH).toInt()).coerceIn(0, max)
            true
        } else if (mx in settingsListLeft..settingsListRight) {
            val module = expandedModule ?: return false
            val entries = buildEntries(module)
            val total = entries.sumOf { it.height }
            val visible = maxVisible(top, bottom)
            val max = ((total - visible).coerceAtLeast(0) * rowH)
            settingsScroll = (settingsScroll - (deltaY * rowH).toInt()).coerceIn(0, max)
            true
        } else {
            false
        }
    }

    // --- Drag for sliders ---

    private var activeSlider: Pair<Value<*>, Double>? = null

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (activeSlider != null) {
            activeSlider = null
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dx: Double,
        dy: Double,
    ): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        val top = headerHeight + 4
        val bottom = this.height - footerHeight
        if (my !in top..bottom) return false
        if (mx !in settingsListLeft..settingsListRight) return false

        val localY = my - top
        val rowIndex = (settingsScroll + localY) / rowH
        val placedEntry = placed.firstOrNull { it.rowIndex == rowIndex } ?: return false
        val entry = placedEntry.entry
        if (entry !is SettingsValueEntry) return false
        val value = entry.value
        if (value !is RangedValue<*>) return false

        val range = value.range
        val start = when (val s: Any = range.start) {
            is Int -> s.toDouble(); is Float -> s.toDouble(); is Double -> s
            else -> 0.0
        }
        val end = when (val e: Any = range.endInclusive) {
            is Int -> e.toDouble(); is Float -> e.toDouble(); is Double -> e
            else -> 1.0
        }
        val trackX1 = settingsListLeft + 8 + entry.depth * 4
        val trackX2 = settingsListRight - 8
        val ratio = ((mx - trackX1).toDouble() / (trackX2 - trackX1)).coerceIn(0.0, 1.0)
        val raw = start + (end - start) * ratio
        if (value.valueType == ValueType.INT) {
            @Suppress("UNCHECKED_CAST")
            (value as Value<Int>).set(raw.toInt())
        } else {
            @Suppress("UNCHECKED_CAST")
            (value as Value<Double>).set(raw)
        }
        return true
    }

    // --- Helpers ---

    private fun maxVisible(top: Int, bottom: Int): Int =
        ((bottom - top) / rowH).coerceAtLeast(1)

    private fun drawText(
        context: GuiGraphicsExtractor,
        text: String,
        x: Float,
        y: Float,
        color: Color4b,
    ) {
        context.text(mc.font, text, x.toInt(), y.toInt(), color.argb, false)
    }
}
