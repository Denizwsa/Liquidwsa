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
import net.ccbluex.liquidbounce.render.gui.clickgui.theme.ClickGuiTheme
import net.ccbluex.liquidbounce.render.gui.clickgui.theme.LerpState
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.ChoiceCycle
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.ColorSwatch
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.PillSwitch
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.RangeSlider
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.SearchBar
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.SegmentedControl
import net.ccbluex.liquidbounce.render.gui.clickgui.widgets.TabBar
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Modern ClickGUI with draggable header, search, category tab bar,
 * and a two-pane content area (modules list + settings pane).
 *
 * Inspired by the legacy LiquidBounce layout but rendered with the new
 * widget toolkit: pill switches, range sliders, segmented controls, color
 * swatches, and a cycle selector for choices.
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

    // Layout — sized to fit the current resolution
    private var totalWidth: Int = 700
    private var totalHeight: Int = 440
    private var screenOriginX: Int = 0
    private var screenOriginY: Int = 0
    private var dragOffsetX: Int = 0
    private var dragOffsetY: Int = 0
    private var isDragging: Boolean = false

    // Rect helper
    private data class Rect(val x1: Int, val x2: Int, val y1: Int, val y2: Int)

    // Content state
    private val categoryLabels: List<String> = categories.map { it.tag.replaceFirstChar { c -> c.titlecase() } }
    private var tabBar: TabBar? = null
    private var searchBar: SearchBar? = null
    private var searchQuery: String = ""

    // Per-module state
    private var expandedModule: ClientModule? = null
    private var moduleScroll: Int = 0
    private var settingsScroll: Int = 0

    // Active settings widgets (recreated on layout pass)
    private val activePillSwitches: MutableMap<Int, PillSwitch> = mutableMapOf()
    private val activeSliders: MutableMap<Int, RangeSlider> = mutableMapOf()
    private val activeChoiceCycles: MutableMap<Int, ChoiceCycle> = mutableMapOf()
    private val activeSegmented: MutableMap<Int, SegmentedControl> = mutableMapOf()
    private val activeColorSwatches: MutableMap<Int, ColorSwatch> = mutableMapOf()

    // Active drag target (slider)
    private var activeSliderId: Int = -1

    // Bind capture
    private var binding: Value<*>? = null

    // Recursive expand/collapse state
    private val expandedGroups: MutableSet<Int> = mutableSetOf()
    private val expandedMultiChoices: MutableSet<Int> = mutableSetOf()
    private val groupHeight: MutableMap<Int, LerpState> = mutableMapOf()

    private data class PlacedEntry(val entry: SettingsEntry, val rowIndex: Int, val id: Int)
    private var placed: List<PlacedEntry> = emptyList()

    private sealed class SettingsEntry {
        abstract val height: Int
    }
    private class GroupHeaderEntry(
        val group: ValueGroup,
        val toggleable: Boolean,
        val enabled: Boolean,
    ) : SettingsEntry() { override val height: Int = 1 }

    private class SettingsValueEntry(
        val value: Value<*>,
        val depth: Int,
        val isBind: Boolean,
    ) : SettingsEntry() { override val height: Int = 1 }

    private class MultiChoiceEntry(
        val value: MultiChoiceListValue<*>,
        val depth: Int,
        val expanded: Boolean,
    ) : SettingsEntry() { override val height: Int = 1 }

    private class MultiChoiceOptionEntry(
        val ownerId: Int,
        val option: Tagged,
        val depth: Int,
        val selected: Boolean,
    ) : SettingsEntry() { override val height: Int = 1 }

    private class ModeGroupEntry(
        val value: ModeValueGroup<*>,
        val depth: Int,
        val activeModeName: String,
        val expanded: Boolean,
    ) : SettingsEntry() { override val height: Int = 1 }

    init {
        computeLayout()
    }

    /**
     * Computes the window size and centered origin based on the current
     * Minecraft GUI-scaled resolution. The window adapts to anything
     * from 800x600 to 4K: a fixed-minimum (so the GUI stays usable on
     * tiny windows) and a soft cap at ~62% of the screen so it never
     * overgrows its host.
     */
    private fun computeLayout() {
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        val minW = 560
        val minH = 380
        val targetW = (sw * 0.62f).toInt()
        val targetH = (sh * 0.66f).toInt()
        totalWidth = targetW.coerceIn(minW, sw.coerceAtLeast(minW))
        totalHeight = targetH.coerceIn(minH, sh.coerceAtLeast(minH))
        // If the window is still wider than the screen, clamp down
        if (totalWidth > sw) totalWidth = sw
        if (totalHeight > sh) totalHeight = sh
        screenOriginX = (sw - totalWidth) / 2
        screenOriginY = (sh - totalHeight) / 2
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui.enabled = false
        mc.setScreen(previous)
    }

    // --- Lifecycle / input ---

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
            if (searchQuery.isNotEmpty()) {
                searchQuery = ""
                searchBar?.text = ""
            } else if (expandedModule != null) {
                expandedModule = null
                settingsScroll = 0
            } else {
                onClose()
            }
            return true
        }
        if (searchBar?.focused == true) {
            if (event.key == InputConstants.KEY_BACKSPACE) {
                searchBar?.backspace()
                searchQuery = searchBar?.text ?: ""
                return true
            }
            val keyName = InputConstants.Type.KEYSYM.getOrCreate(event.key).name
            if (keyName.length == 1) {
                val c = keyName[0]
                if (c.isLetterOrDigit() || c == ' ' || c == '_' || c == '-') {
                    searchBar?.appendChar(c)
                    searchQuery = searchBar?.text ?: ""
                    return true
                }
            }
        }
        return super.keyPressed(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()
        val button = event.button()

        // Header drag (button 0 only)
        if (button == 0 && my in screenOriginY..(screenOriginY + ClickGuiTheme.headerHeight)) {
            // Don't start drag if the click was on the close button or search
            val closeRect = closeButtonRect()
            val searchRect = searchBarRect()
            if (mx in closeRect.x1..closeRect.x2 &&
                my in closeRect.y1..closeRect.y2
            ) {
                onClose()
                return true
            }
            if (mx in searchRect.x1..searchRect.x2 &&
                my in searchRect.y1..searchRect.y2
            ) {
                searchBar?.setFocused(true, System.currentTimeMillis())
                return true
            }
            // Otherwise, start drag
            dragOffsetX = mx - screenOriginX
            dragOffsetY = my - screenOriginY
            isDragging = true
            return true
        }

        // Tab bar
        tabBar?.let { tb ->
            if (tb.handleClick(mx, my)) {
                expandedModule = null
                moduleScroll = 0
                settingsScroll = 0
                return true
            }
        }

        val contentTop = screenOriginY + ClickGuiTheme.headerHeight + ClickGuiTheme.tabBarHeight
        val contentBottom = screenOriginY + totalHeight - ClickGuiTheme.statusBarHeight
        if (my in contentTop..contentBottom) {
            if (mx in moduleListRect().x1..moduleListRect().x2) {
                val module = moduleAtY(my, contentTop) ?: return true
                when (button) {
                    0 -> {
                        expandedModule = if (expandedModule == module) null else module
                        settingsScroll = 0
                    }
                    1 -> module.enabled = !module.enabled
                }
                return true
            }
            if (mx in settingsListRect().x1..settingsListRect().x2) {
                handleSettingsClick(mx, my, button)
                return true
            }
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (isDragging) {
            isDragging = false
            return true
        }
        if (activeSliderId >= 0) {
            activeSliderId = -1
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dx: Double,
        dy: Double,
    ): Boolean {
        if (isDragging) {
            val mx = event.x.toInt()
            val my = event.y.toInt()
            screenOriginX = (mx - dragOffsetX).coerceIn(0, mc.window.guiScaledWidth - totalWidth)
            screenOriginY = (my - dragOffsetY).coerceIn(0, mc.window.guiScaledHeight - totalHeight)
            return true
        }
        if (activeSliderId >= 0) {
            val slider = activeSliders[activeSliderId] ?: return false
            if (slider.updateFromMouse(event.x.toInt())) {
                val entry = placed.firstOrNull { it.id == activeSliderId }?.entry
                if (entry is SettingsValueEntry && entry.value is RangedValue<*>) {
                    applySliderValue(entry.value, slider.value)
                }
            }
            return true
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
    ): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val contentTop = screenOriginY + ClickGuiTheme.headerHeight + ClickGuiTheme.tabBarHeight
        val contentBottom = screenOriginY + totalHeight - ClickGuiTheme.statusBarHeight
        if (my !in contentTop..contentBottom) return false
        if (mx in moduleListRect().x1..moduleListRect().x2) {
            val modules = filteredModules()
            val visible = maxVisibleRows(contentTop, contentBottom)
            val max = ((modules.size - visible).coerceAtLeast(0) * ClickGuiTheme.rowHeight)
            moduleScroll = (moduleScroll - (deltaY * ClickGuiTheme.rowHeight).toInt()).coerceIn(0, max)
            return true
        }
        if (mx in settingsListRect().x1..settingsListRect().x2) {
            val module = expandedModule ?: return false
            val total = buildEntries(module).sumOf { it.height }
            val visible = maxVisibleRows(contentTop, contentBottom)
            val max = ((total - visible).coerceAtLeast(0) * ClickGuiTheme.rowHeight)
            settingsScroll = (settingsScroll - (deltaY * ClickGuiTheme.rowHeight).toInt()).coerceIn(0, max)
            return true
        }
        return false
    }

    // --- Render ---

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        val now = System.currentTimeMillis()

        // Drop the search focus when clicking outside the search bar
        if (searchBar?.focused == true) {
            val r = searchBarRect()
            if (!(mouseX in r.x1..r.x2 && mouseY in r.y1..r.y2)) {
                // Note: don't unfocus on every frame; only via click handler.
            }
        }

        // Outer background (subtle dim)
        with(context) {
            drawRoundedRect(
                screenOriginX.toFloat() - 2f, screenOriginY.toFloat() - 2f,
                (screenOriginX + totalWidth).toFloat() + 2f,
                (screenOriginY + totalHeight).toFloat() + 2f,
                ClickGuiTheme.panelRadius + 2f,
                fillColor = ClickGuiTheme.shadow,
            )
            drawRoundedRect(
                screenOriginX.toFloat(), screenOriginY.toFloat(),
                (screenOriginX + totalWidth).toFloat(),
                (screenOriginY + totalHeight).toFloat(),
                ClickGuiTheme.panelRadius,
                fillColor = ClickGuiTheme.panelBg,
                outlineColor = ClickGuiTheme.border,
                outlineWidth = 1.0f,
            )
        }

        drawHeader(context, mouseX, mouseY, now)
        drawTabBar(context, mouseX, mouseY, now)
        drawContent(context, mouseX, mouseY, now)
        drawStatusBar(context)
    }

    // --- Header / Tabs / Status ---

    private fun drawHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        nowMs: Long,
    ) {
        // Header background
        with(context) {
            drawRoundedRect(
                screenOriginX.toFloat(), screenOriginY.toFloat(),
                (screenOriginX + totalWidth).toFloat(),
                (screenOriginY + ClickGuiTheme.headerHeight).toFloat(),
                ClickGuiTheme.panelRadius,
                fillColor = ClickGuiTheme.headerBg,
            )
        }

        // Title
        val titleY = screenOriginY + (ClickGuiTheme.headerHeight - 8) / 2
        context.text(
            mc.font, "LiquidBounce",
            screenOriginX + 12, titleY,
            ClickGuiTheme.textPrimary.argb, false,
        )
        val subtitle = "ClickGui"
        context.text(
            mc.font, subtitle,
            screenOriginX + 12 + mc.font.width("LiquidBounce") + 6, titleY,
            ClickGuiTheme.textMuted.argb, false,
        )

        // Search bar
        val sRect = searchBarRect()
        if (searchBar == null) {
            searchBar = SearchBar(
                sRect.x1.toFloat(), sRect.y1.toFloat(),
                (sRect.x2 - sRect.x1).toFloat(),
                (sRect.y2 - sRect.y1).toFloat(),
            )
        }
        searchBar?.x = sRect.x1.toFloat()
        searchBar?.y = sRect.y1.toFloat()
        searchBar?.width = (sRect.x2 - sRect.x1).toFloat()
        searchBar?.height = (sRect.y2 - sRect.y1).toFloat()
        searchBar?.text = searchQuery
        searchBar?.draw(context, mouseX, mouseY, nowMs)

        // Close button
        val cRect = closeButtonRect()
        val closeHover = mouseX in cRect.x1..cRect.x2 && mouseY in cRect.y1..cRect.y2
        val closeColor = if (closeHover) Color4b(255, 90, 100, 220) else Color4b(80, 80, 95, 200)
        with(context) {
            drawRoundedRect(
                cRect.x1.toFloat(), cRect.y1.toFloat(),
                cRect.x2.toFloat(), cRect.y2.toFloat(),
                4f,
                fillColor = closeColor,
            )
        }
        val cx = (cRect.x1 + cRect.x2) / 2
        val cy = (cRect.y1 + cRect.y2) / 2
        val sz = 4
        context.text(
            mc.font, "x",
            cx - mc.font.width("x") / 2, cy - 4,
            ClickGuiTheme.textPrimary.argb, false,
        )
        // Unused sz to silence "unused" warning
        @Suppress("UNUSED_VARIABLE")
        val _z = sz
    }

    private fun drawTabBar(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        nowMs: Long,
    ) {
        val x = screenOriginX
        val y = screenOriginY + ClickGuiTheme.headerHeight
        if (tabBar == null) {
            tabBar = TabBar(
                x.toFloat(), y.toFloat(),
                totalWidth.toFloat(), ClickGuiTheme.tabBarHeight.toFloat(),
                categoryLabels, 0,
            )
        }
        tabBar?.x = x.toFloat()
        tabBar?.y = y.toFloat()
        tabBar?.width = totalWidth.toFloat()
        tabBar?.draw(context, mouseX, mouseY, nowMs)
    }

    private fun drawStatusBar(context: GuiGraphicsExtractor) {
        val y = screenOriginY + totalHeight - ClickGuiTheme.statusBarHeight
        val x1 = screenOriginX.toFloat()
        val x2 = (screenOriginX + totalWidth).toFloat()
        with(context) {
            drawRoundedRect(
                x1, y.toFloat(), x2, (screenOriginY + totalHeight).toFloat(),
                ClickGuiTheme.panelRadius,
                fillColor = ClickGuiTheme.headerBg,
            )
            drawRoundedRect(
                x1, y.toFloat(), x2, y + 1f, 0f,
                fillColor = ClickGuiTheme.separator,
            )
        }
        val enabled = ModuleManager.count { it.running }
        val total = ModuleManager.size
        val text = "$enabled / $total modules enabled"
        context.text(
            mc.font, text,
            screenOriginX + 12, y + (ClickGuiTheme.statusBarHeight - 8) / 2,
            ClickGuiTheme.textSecondary.argb, false,
        )
        val hint = "ESC: close  |  Search: type  |  Right-click: toggle"
        val tw = mc.font.width(hint)
        context.text(
            mc.font, hint,
            screenOriginX + totalWidth - 12 - tw,
            y + (ClickGuiTheme.statusBarHeight - 8) / 2,
            ClickGuiTheme.textMuted.argb, false,
        )
    }

    // --- Content ---

    private fun drawContent(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        nowMs: Long,
    ) {
        val contentTop = screenOriginY + ClickGuiTheme.headerHeight + ClickGuiTheme.tabBarHeight
        val contentBottom = screenOriginY + totalHeight - ClickGuiTheme.statusBarHeight

        val mRect = moduleListRect()
        with(context) {
            drawRoundedRect(
                mRect.x1.toFloat(), contentTop.toFloat(),
                mRect.x2.toFloat(), contentBottom.toFloat(),
                0f,
                fillColor = ClickGuiTheme.sidebarBg,
            )
        }

        val sRect = settingsListRect()
        with(context) {
            drawRoundedRect(
                sRect.x1.toFloat(), contentTop.toFloat(),
                sRect.x2.toFloat(), contentBottom.toFloat(),
                0f,
                fillColor = ClickGuiTheme.panelBg,
            )
            drawRoundedRect(
                mRect.x2.toFloat(), contentTop.toFloat(),
                (mRect.x2 + 1).toFloat(), contentBottom.toFloat(),
                0f,
                fillColor = ClickGuiTheme.separator,
            )
        }

        drawModuleList(context, mouseX, mouseY, contentTop, contentBottom, nowMs)
        drawSettings(context, mouseX, mouseY, contentTop, contentBottom, nowMs)
    }

    private fun drawModuleList(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        top: Int,
        bottom: Int,
        nowMs: Long,
    ) {
        val modules = filteredModules()
        val rowH = ClickGuiTheme.rowHeight
        val visible = maxVisibleRows(top, bottom)
        val mRect = moduleListRect()
        val maxScroll = ((modules.size - visible).coerceAtLeast(0) * rowH)
        if (moduleScroll > maxScroll) moduleScroll = maxScroll
        if (moduleScroll < 0) moduleScroll = 0
        val firstIndex = moduleScroll / rowH

        // Track and clear orphan pill switches
        val keepIds = mutableSetOf<Int>()
        for (i in 0 until visible) {
            val moduleIndex = firstIndex + i
            if (moduleIndex !in modules.indices) break
            val module = modules[moduleIndex]
            val id = System.identityHashCode(module)
            keepIds.add(id)
            val y = top + i * rowH
            val isExpanded = expandedModule == module
            val isHover = mouseX in mRect.x1..mRect.x2 && mouseY in y..(y + rowH - 2)
            val bg = when {
                isExpanded -> ClickGuiTheme.rowSelected
                module.running -> Color4b(
                    (ClickGuiTheme.accent.r * 0.25f).toInt().coerceIn(0, 255),
                    (ClickGuiTheme.accent.g * 0.35f).toInt().coerceIn(0, 255),
                    (ClickGuiTheme.accent.b * 0.50f).toInt().coerceIn(0, 255),
                    90,
                )
                isHover -> ClickGuiTheme.rowHover
                else -> ClickGuiTheme.rowIdle
            }
            with(context) {
                drawRoundedRect(
                    (mRect.x1 + 4).toFloat(), y.toFloat(),
                    (mRect.x2 - 4).toFloat(), (y + rowH - 2).toFloat(),
                    ClickGuiTheme.buttonRadius,
                    fillColor = bg,
                )
            }
            val nameColor = if (module.running) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary
            context.text(
                mc.font, module.name,
                mRect.x1 + 12, y + (rowH - 8) / 2,
                nameColor.argb, false,
            )

            // Pill switch
            val pillX = (mRect.x2 - 12 - 36).toFloat()
            val pillY = (y + (rowH - 18) / 2).toFloat()
            val pill = activePillSwitches.getOrPut(id) {
                PillSwitch(pillX, pillY).also { it.setOn(module.running, nowMs) }
            }
            pill.x = pillX
            pill.y = pillY
            pill.setOn(module.running, nowMs)
            pill.draw(context, mouseX, mouseY, nowMs)
        }
        activePillSwitches.keys.retainAll(keepIds)

        // Scrollbar
        if (modules.size > visible) {
            val trackH = bottom - top
            val thumbH = ((visible.toFloat() / modules.size) * trackH).coerceAtLeast(20f)
            val thumbY = top + (moduleScroll.toFloat() / maxScroll) * (trackH - thumbH)
            with(context) {
                drawRoundedRect(
                    (mRect.x2 - 3).toFloat(), thumbY,
                    (mRect.x2 - 1).toFloat(), thumbY + thumbH, 1f,
                    fillColor = Color4b(120, 120, 130, 200),
                )
            }
        }
    }

    private fun drawSettings(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        top: Int,
        bottom: Int,
        nowMs: Long,
    ) {
        val module = expandedModule
        val sRect = settingsListRect()
        if (module == null) {
            context.text(
                mc.font, "Select a module",
                sRect.x1 + 12, top + 12,
                ClickGuiTheme.textMuted.argb, false,
            )
            placed = emptyList()
            return
        }

        val entries = buildEntries(module)
        val totalRows = entries.sumOf { it.height }
        val visible = maxVisibleRows(top, bottom)
        val maxScroll = ((totalRows - visible).coerceAtLeast(0) * ClickGuiTheme.rowHeight)
        if (settingsScroll > maxScroll) settingsScroll = maxScroll
        if (settingsScroll < 0) settingsScroll = 0
        val firstIndex = settingsScroll / ClickGuiTheme.rowHeight

        // Header strip with module name
        with(context) {
            drawRoundedRect(
                sRect.x1.toFloat(), top.toFloat(),
                sRect.x2.toFloat(), (top + 28).toFloat(),
                0f,
                fillColor = ClickGuiTheme.headerBg,
            )
        }
        val nameText = if (module.tag != null) "${module.name}  ${module.tag}" else module.name
        val nameColor = if (module.running) ClickGuiTheme.textEnabled else ClickGuiTheme.textPrimary
        context.text(
            mc.font, nameText,
            sRect.x1 + 12, top + 10,
            nameColor.argb, false,
        )

        val placedMutable = mutableListOf<PlacedEntry>()
        var row = 0
        val widgetTrackIds = mutableSetOf<Int>()
        for (e in entries) {
            val y = top + 28 + (row - firstIndex) * ClickGuiTheme.rowHeight
            val id = System.identityHashCode(e)
            widgetTrackIds.add(id)
            placedMutable.add(PlacedEntry(e, row, id))
            when (e) {
                is GroupHeaderEntry -> drawGroupHeader(context, mouseX, mouseY, y, e)
                is SettingsValueEntry -> drawValueEntry(context, mouseX, mouseY, y, e, id, nowMs)
                is MultiChoiceEntry -> drawMultiChoiceHeader(context, mouseX, mouseY, y, e)
                is MultiChoiceOptionEntry -> drawMultiChoiceOption(context, mouseX, mouseY, y, e)
                is ModeGroupEntry -> drawModeGroupHeader(context, mouseX, mouseY, y, e, id, nowMs)
            }
            row += e.height
        }
        placed = placedMutable
        cleanupWidgets(widgetTrackIds)

        // Scrollbar
        if (totalRows > visible) {
            val trackH = bottom - top
            val thumbH = ((visible.toFloat() / totalRows) * trackH).coerceAtLeast(20f)
            val thumbY = top + (settingsScroll.toFloat() / maxScroll) * (trackH - thumbH)
            with(context) {
                drawRoundedRect(
                    (sRect.x2 - 3).toFloat(), thumbY,
                    (sRect.x2 - 1).toFloat(), thumbY + thumbH, 1f,
                    fillColor = Color4b(120, 120, 130, 200),
                )
            }
        }
    }

    private fun cleanupWidgets(keepIds: Set<Int>) {
        activePillSwitches.keys.retainAll { id ->
            keepIds.contains(id) || id in activePillSwitches.keys.filter { it !in keepIds && it > 0 }
        }
        activeSliders.keys.retainAll(keepIds)
        activeChoiceCycles.keys.retainAll(keepIds)
        activeSegmented.keys.retainAll(keepIds)
        activeColorSwatches.keys.retainAll(keepIds)
        // Drop all widgets that aren't in keepIds
        val keysToRemove = (activePillSwitches.keys + activeSliders.keys +
            activeChoiceCycles.keys + activeSegmented.keys +
            activeColorSwatches.keys) - keepIds
        for (k in keysToRemove) {
            activePillSwitches.remove(k)
            activeSliders.remove(k)
            activeChoiceCycles.remove(k)
            activeSegmented.remove(k)
            activeColorSwatches.remove(k)
        }
    }

    // --- Settings row renderers ---

    private fun drawGroupHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: GroupHeaderEntry,
    ) {
        val sRect = settingsListRect()
        val isHover = mouseX in sRect.x1..sRect.x2 && mouseY in y..(y + ClickGuiTheme.groupHeaderHeight - 2)
        val bg = if (isHover) ClickGuiTheme.groupHeaderHover else ClickGuiTheme.groupHeader
        with(context) {
            drawRoundedRect(
                (sRect.x1 + 4).toFloat(), y.toFloat(),
                (sRect.x2 - 4).toFloat(), (y + ClickGuiTheme.groupHeaderHeight - 2).toFloat(),
                ClickGuiTheme.buttonRadius,
                fillColor = bg,
            )
        }
        val id = System.identityHashCode(entry.group)
        val expanded = expandedGroups.contains(id)
        val arrow = if (expanded) "v" else ">"
        context.text(
            mc.font, arrow,
            sRect.x1 + 12, y + 8,
            ClickGuiTheme.textAccent.argb, false,
        )
        val label = if (entry.toggleable) {
            "${entry.group.name}: " + (if (entry.enabled) "ON" else "OFF")
        } else {
            entry.group.name
        }
        val nameColor = when {
            entry.toggleable && entry.enabled -> ClickGuiTheme.textPrimary
            else -> ClickGuiTheme.textPrimary
        }
        context.text(
            mc.font, label,
            sRect.x1 + 12 + 12, y + 8,
            nameColor.argb, false,
        )
    }

    private fun drawValueEntry(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: SettingsValueEntry,
        id: Int,
        nowMs: Long,
    ) {
        val sRect = settingsListRect()
        val isHover = mouseX in sRect.x1..sRect.x2 && mouseY in y..(y + ClickGuiTheme.rowHeight - 2)
        val bg = if (isHover) ClickGuiTheme.rowHover else ClickGuiTheme.rowIdle
        with(context) {
            drawRoundedRect(
                (sRect.x1 + 4).toFloat(), y.toFloat(),
                (sRect.x2 - 4).toFloat(), (y + ClickGuiTheme.rowHeight - 2).toFloat(),
                ClickGuiTheme.buttonRadius,
                fillColor = bg,
            )
        }
        val value = entry.value
        val leftX = sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth
        val rightX = sRect.x2 - 12
        val rowY = y + (ClickGuiTheme.rowHeight - 8) / 2

        when {
            entry.isBind && value is BindValue -> {
                val bindText = "Bind: " + (value.get().keyName.takeIf { it.isNotEmpty() } ?: "None")
                context.text(
                    mc.font, bindText,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
                if (binding == value) {
                    val t = (System.currentTimeMillis() / 500L) % 2L == 0L
                    val blink = if (t) "Press a key..." else ""
                    context.text(
                        mc.font, blink,
                        rightX - mc.font.width(blink), rowY,
                        ClickGuiTheme.textAccent.argb, false,
                    )
                }
            }
            value is BindValue -> {
                val bindText = "Bind: " + (value.get().keyName.takeIf { it.isNotEmpty() } ?: "None")
                context.text(
                    mc.font, bindText,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
            }
            value.valueType == ValueType.BOOLEAN -> {
                context.text(
                    mc.font, value.name,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
                val pillX = (rightX - 36).toFloat()
                val pillY = (y + (ClickGuiTheme.rowHeight - 18) / 2).toFloat()
                val pill = activePillSwitches.getOrPut(id) {
                    PillSwitch(pillX, pillY).also { it.setOn(value.get() as Boolean, nowMs) }
                }
                pill.x = pillX
                pill.y = pillY
                pill.setOn(value.get() as Boolean, nowMs)
                pill.draw(context, mouseX, mouseY, nowMs)
            }
            value is RangedValue<*> -> {
                context.text(
                    mc.font, value.name,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
                val sliderX = leftX + mc.font.width(value.name) + 12
                val sliderW = rightX - sliderX - 60
                val (start, end, isInt) = rangeInfo(value)
                val initial = numericValue(value)
                val slider = activeSliders.getOrPut(id) {
                    RangeSlider(sliderX.toFloat(), y.toFloat(), sliderW.toFloat(), start, end, isInt, initial)
                }
                slider.x = sliderX.toFloat()
                slider.y = y.toFloat()
                slider.value = numericValue(value)
                val text = slider.draw(context, mouseX, mouseY, ClickGuiTheme.textAccent)
                val tw = mc.font.width(text)
                context.text(
                    mc.font, text,
                    rightX - tw, rowY,
                    ClickGuiTheme.textAccent.argb, false,
                )
            }
            value is ChoiceListValue<*> -> {
                context.text(
                    mc.font, value.name,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
                val cycleX = leftX + mc.font.width(value.name) + 12
                val cycleW = rightX - cycleX
                val options = value.choices.toList().map { (it as Tagged).tag }
                val currentIdx = options.indexOf((value.get() as? Tagged)?.tag ?: "")
                val cycle = activeChoiceCycles.getOrPut(id) {
                    ChoiceCycle(cycleX.toFloat(), (y + 2).toFloat(), cycleW.toFloat(), options = options, initialIndex = currentIdx)
                }
                cycle.x = cycleX.toFloat()
                cycle.y = (y + 2).toFloat()
                cycle.width = cycleW.toFloat()
                cycle.index = currentIdx.coerceAtLeast(0)
                cycle.draw(context, mouseX, mouseY, ClickGuiTheme.textAccent)
            }
            value.valueType == ValueType.COLOR -> {
                context.text(
                    mc.font, value.name,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
                val swatch = activeColorSwatches.getOrPut(id) {
                    ColorSwatch((rightX - 80).toFloat(), (y + 2).toFloat(), 18f, value.get() as Color4b)
                }
                swatch.x = (rightX - 80).toFloat()
                swatch.y = (y + 2).toFloat()
                swatch.color = value.get() as Color4b
                swatch.draw(context)
                swatch.drawHex(context, rightX.toFloat(), (y + ClickGuiTheme.rowHeight / 2).toFloat())
            }
            else -> {
                context.text(
                    mc.font, value.name,
                    leftX, rowY, ClickGuiTheme.textPrimary.argb, false,
                )
                val raw = value.get().toString()
                val rw = mc.font.width(raw)
                context.text(
                    mc.font, raw,
                    rightX - rw, rowY,
                    ClickGuiTheme.textMuted.argb, false,
                )
            }
        }
    }

    private fun drawMultiChoiceHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: MultiChoiceEntry,
    ) {
        val sRect = settingsListRect()
        val isHover = mouseX in sRect.x1..sRect.x2 && mouseY in y..(y + ClickGuiTheme.rowHeight - 2)
        val bg = if (isHover) ClickGuiTheme.rowHover else ClickGuiTheme.rowIdle
        with(context) {
            drawRoundedRect(
                (sRect.x1 + 4 + entry.depth * ClickGuiTheme.indentWidth).toFloat(), y.toFloat(),
                (sRect.x2 - 4).toFloat(), (y + ClickGuiTheme.rowHeight - 2).toFloat(),
                ClickGuiTheme.buttonRadius,
                fillColor = bg,
            )
        }
        val arrow = if (entry.expanded) "v" else ">"
        val count = (entry.value.get() as Collection<*>).size
        val label = "${entry.value.name}: $count/${entry.value.choices.size}"
        context.text(
            mc.font, arrow,
            sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth, y + (ClickGuiTheme.rowHeight - 8) / 2,
            ClickGuiTheme.textAccent.argb, false,
        )
        context.text(
            mc.font, label,
            sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth + 12, y + (ClickGuiTheme.rowHeight - 8) / 2,
            ClickGuiTheme.textPrimary.argb, false,
        )
    }

    private fun drawMultiChoiceOption(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: MultiChoiceOptionEntry,
    ) {
        val sRect = settingsListRect()
        val isHover = mouseX in sRect.x1..sRect.x2 && mouseY in y..(y + ClickGuiTheme.rowHeight - 2)
        val bg = when {
            isHover -> ClickGuiTheme.rowHover
            entry.selected -> Color4b(
                (ClickGuiTheme.accent.r * 0.20f).toInt().coerceIn(0, 255),
                (ClickGuiTheme.accent.g * 0.35f).toInt().coerceIn(0, 255),
                (ClickGuiTheme.accent.b * 0.55f).toInt().coerceIn(0, 255),
                100,
            )
            else -> ClickGuiTheme.rowIdle
        }
        with(context) {
            drawRoundedRect(
                (sRect.x1 + 4 + entry.depth * ClickGuiTheme.indentWidth).toFloat(), y.toFloat(),
                (sRect.x2 - 4).toFloat(), (y + ClickGuiTheme.rowHeight - 2).toFloat(),
                ClickGuiTheme.buttonRadius,
                fillColor = bg,
            )
        }
        val mark = if (entry.selected) "[x]" else "[ ]"
        val textColor = if (entry.selected) ClickGuiTheme.textAccent else ClickGuiTheme.textMuted
        context.text(
            mc.font, mark,
            sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth,
            y + (ClickGuiTheme.rowHeight - 8) / 2,
            textColor.argb, false,
        )
        context.text(
            mc.font, entry.option.tag,
            sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth + 24,
            y + (ClickGuiTheme.rowHeight - 8) / 2,
            ClickGuiTheme.textPrimary.argb,
            false,
        )
    }

    private fun drawModeGroupHeader(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        y: Int,
        entry: ModeGroupEntry,
        id: Int,
        nowMs: Long,
    ) {
        val sRect = settingsListRect()
        val isHover = mouseX in sRect.x1..sRect.x2 && mouseY in y..(y + ClickGuiTheme.rowHeight - 2)
        val bg = if (isHover) ClickGuiTheme.rowHover else ClickGuiTheme.rowIdle
        with(context) {
            drawRoundedRect(
                (sRect.x1 + 4 + entry.depth * ClickGuiTheme.indentWidth).toFloat(), y.toFloat(),
                (sRect.x2 - 4).toFloat(), (y + ClickGuiTheme.rowHeight - 2).toFloat(),
                ClickGuiTheme.buttonRadius,
                fillColor = bg,
            )
        }
        val arrow = if (entry.expanded) "v" else ">"
        context.text(
            mc.font, arrow,
            sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth,
            y + (ClickGuiTheme.rowHeight - 8) / 2,
            ClickGuiTheme.textAccent.argb, false,
        )
        context.text(
            mc.font, "${entry.value.name}:",
            sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth + 12,
            y + (ClickGuiTheme.rowHeight - 8) / 2,
            ClickGuiTheme.textPrimary.argb, false,
        )
        val options = entry.value.getModeStrings().toList()
        val currentIdx = options.indexOf(entry.activeModeName)
        val leftOfCycle = sRect.x1 + 12 + entry.depth * ClickGuiTheme.indentWidth + 12 +
            mc.font.width("${entry.value.name}:") + 12
        val segW = (sRect.x2 - 8 - leftOfCycle).toFloat()
        if (segW > 40f) {
            val seg = activeSegmented.getOrPut(id) {
                SegmentedControl(leftOfCycle.toFloat(), (y + 2).toFloat(), segW, options = options.toList(), initialIndex = currentIdx)
            }
            seg.x = leftOfCycle.toFloat()
            seg.y = (y + 2).toFloat()
            seg.width = segW
            seg.index = currentIdx.coerceAtLeast(0)
            seg.draw(context, mouseX, mouseY)
        }
    }

    // --- Walk (re-used) ---

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
                    if (expanded) walk(value.activeMode, depth + 1, out)
                }
                is ValueGroup -> {
                    val toggleable = value is ToggleableValueGroup
                    val enabled = if (toggleable) (value as ToggleableValueGroup).enabled else true
                    out.add(GroupHeaderEntry(value, toggleable, enabled))
                    if (expandedGroups.contains(System.identityHashCode(value))) {
                        walk(value, depth + 1, out)
                    }
                }
                is BindValue -> out.add(SettingsValueEntry(value, depth, isBind = true))
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
                else -> out.add(SettingsValueEntry(value, depth, isBind = false))
            }
        }
    }

    // --- Hit-testing helpers ---

    private fun handleSettingsClick(mx: Int, my: Int, button: Int) {
        val placedEntry = placed.firstOrNull { entry ->
            val top = settingsListRect().y1 + 28 + (entry.rowIndex - settingsScroll / ClickGuiTheme.rowHeight) *
                ClickGuiTheme.rowHeight
            my in top..(top + ClickGuiTheme.rowHeight - 2)
        } ?: return

        when (val entry = placedEntry.entry) {
            is GroupHeaderEntry -> {
                if (button == 0) {
                    val id = System.identityHashCode(entry.group)
                    if (!expandedGroups.add(id)) expandedGroups.remove(id)
                } else if (button == 1 && entry.toggleable) {
                    (entry.group as ToggleableValueGroup).enabled = !entry.enabled
                }
            }
            is SettingsValueEntry -> {
                if (entry.isBind) {
                    if (button == 0) {
                        binding = if (binding == entry.value) null else entry.value
                    }
                } else {
                    handleValueClick(entry.value, entry.depth, mx, my, button, placedEntry.id)
                }
            }
            is MultiChoiceEntry -> {
                if (button == 0) {
                    val id = System.identityHashCode(entry.value)
                    if (!expandedMultiChoices.add(id)) expandedMultiChoices.remove(id)
                }
            }
            is MultiChoiceOptionEntry -> {
                if (button == 0) toggleMultiChoiceOption(entry.ownerId, entry.option)
            }
            is ModeGroupEntry -> {
                if (button == 0) {
                    val seg = activeSegmented[placedEntry.id]
                    if (seg != null && seg.handleClick(mx, my)) {
                        val modes = entry.value.getModeStrings()
                        if (seg.index in modes.indices) {
                            entry.value.setByString(modes[seg.index])
                        }
                    } else {
                        // Cycle to next mode
                        val modes = entry.value.getModeStrings()
                        if (modes.isNotEmpty()) {
                            val currentIdx = modes.indexOf(entry.activeModeName)
                            val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % modes.size
                            entry.value.setByString(modes[nextIdx])
                        }
                    }
                } else if (button == 1) {
                    val id = System.identityHashCode(entry.value)
                    if (!expandedGroups.add(id)) expandedGroups.remove(id)
                }
            }
        }
    }

    private fun handleValueClick(
        value: Value<*>,
        depth: Int,
        mx: Int,
        my: Int,
        button: Int,
        id: Int,
    ) {
        when {
            value.valueType == ValueType.BOOLEAN -> {
                val pill = activePillSwitches[id] ?: return
                if (pill.handleClick(mx, my, System.currentTimeMillis())) {
                    @Suppress("UNCHECKED_CAST")
                    (value as Value<Boolean>).set(pill.isOn())
                }
            }
            value is RangedValue<*> -> {
                val slider = activeSliders[id] ?: return
                if (slider.isOnTrack(mx, my)) {
                    if (slider.updateFromMouse(mx)) {
                        applySliderValue(value, slider.value)
                    }
                    activeSliderId = id
                }
            }
            value is ChoiceListValue<*> -> {
                val cycle = activeChoiceCycles[id] ?: return
                if (cycle.handleClick(mx, my)) {
                    val choices = value.choices.toList()
                    val idx = cycle.index.coerceIn(0, choices.size - 1)
                    @Suppress("UNCHECKED_CAST")
                    (value as Value<Any>).set(choices[idx])
                }
            }
            value.valueType == ValueType.COLOR -> {
                val swatch = activeColorSwatches[id] ?: return
                if (swatch.isHovered(mx, my)) {
                    swatch.cycleAlpha()
                    @Suppress("UNCHECKED_CAST")
                    (value as Value<Color4b>).set(swatch.color)
                }
            }
        }
        // unused
        @Suppress("UNUSED_VARIABLE")
        val _d = depth
    }

    private fun toggleMultiChoiceOption(ownerId: Int, option: Tagged) {
        val entry = placed.firstOrNull { it.entry is MultiChoiceEntry &&
            System.identityHashCode((it.entry as MultiChoiceEntry).value) == ownerId }
        val multi = (entry?.entry as? MultiChoiceEntry)?.value ?: return
        val current = (multi.get() as Collection<*>)
        val list = current.toMutableList()
        val idx = list.indexOfFirst { (it as? Tagged)?.tag == option.tag }
        if (idx >= 0) list.removeAt(idx) else list.add(option)
        @Suppress("UNCHECKED_CAST")
        (multi as Value<Any>).set(list)
    }

    // --- Layout / rect helpers ---

    private fun moduleListRect(): Rect {
        val x1 = screenOriginX
        val x2 = screenOriginX + (totalWidth * 0.42f).toInt()
        val y1 = screenOriginY + ClickGuiTheme.headerHeight + ClickGuiTheme.tabBarHeight
        val y2 = screenOriginY + totalHeight - ClickGuiTheme.statusBarHeight
        return Rect(x1, x2, y1, y2)
    }

    private fun settingsListRect(): Rect {
        val x1 = screenOriginX + (totalWidth * 0.42f).toInt()
        val x2 = screenOriginX + totalWidth
        val y1 = screenOriginY + ClickGuiTheme.headerHeight + ClickGuiTheme.tabBarHeight
        val y2 = screenOriginY + totalHeight - ClickGuiTheme.statusBarHeight
        return Rect(x1, x2, y1, y2)
    }

    private fun searchBarRect(): Rect {
        val w = 220
        val h = 22
        val right = screenOriginX + totalWidth - 12 - 32
        val left = right - w
        val top = screenOriginY + (ClickGuiTheme.headerHeight - h) / 2
        val bottom = top + h
        return Rect(left, right, top, bottom)
    }

    private fun closeButtonRect(): Rect {
        val size = 22
        val right = screenOriginX + totalWidth - 8
        val left = right - size
        val top = screenOriginY + (ClickGuiTheme.headerHeight - size) / 2
        val bottom = top + size
        return Rect(left, right, top, bottom)
    }

    private fun moduleAtY(my: Int, contentTop: Int): ClientModule? {
        val modules = filteredModules()
        val firstIndex = moduleScroll / ClickGuiTheme.rowHeight
        val localY = my - contentTop
        val rowIndex = firstIndex + (localY / ClickGuiTheme.rowHeight)
        return modules.getOrNull(rowIndex)
    }

    private fun filteredModules(): List<ClientModule> {
        val idx = tabBar?.index ?: 0
        val cat = categories.getOrNull(idx) ?: return emptyList()
        val all = ModuleManager.filter { it.category == cat }
        val list = if (searchQuery.isBlank()) all else all.filter { matches(it.name) }
        return list.sortedBy { it.name.lowercase() }
    }

    private fun matches(name: String): Boolean {
        if (searchQuery.isBlank()) return true
        return name.lowercase().contains(searchQuery.lowercase())
    }

    private fun maxVisibleRows(top: Int, bottom: Int): Int {
        val avail = (bottom - top - 28).coerceAtLeast(ClickGuiTheme.rowHeight)
        return (avail / ClickGuiTheme.rowHeight).coerceAtLeast(1)
    }

    // --- Value helpers ---

    private fun rangeInfo(value: RangedValue<*>): Triple<Double, Double, Boolean> {
        val range = value.range
        val start = when (val s: Any = range.start) {
            is Int -> s.toDouble(); is Float -> s.toDouble(); is Double -> s; else -> 0.0
        }
        val end = when (val e: Any = range.endInclusive) {
            is Int -> e.toDouble(); is Float -> e.toDouble(); is Double -> e; else -> 1.0
        }
        val isInt = value.valueType == ValueType.INT
        return Triple(start, end, isInt)
    }

    private fun numericValue(value: RangedValue<*>): Double {
        return when (val c = value.get()) {
            is Int -> c.toDouble(); is Float -> c.toDouble(); is Double -> c; else -> 0.0
        }
    }

    private fun applySliderValue(value: RangedValue<*>, raw: Double) {
        if (value.valueType == ValueType.INT) {
            @Suppress("UNCHECKED_CAST")
            (value as Value<Int>).set(raw.toInt())
        } else {
            @Suppress("UNCHECKED_CAST")
            (value as Value<Double>).set(raw)
        }
    }
}
