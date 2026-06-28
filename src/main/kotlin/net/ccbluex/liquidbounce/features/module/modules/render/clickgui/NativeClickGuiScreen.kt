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
package net.ccbluex.liquidbounce.features.module.modules.render.clickgui

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.hud.NativeHudEditorScreen
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.getBounds
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.lwjgl.glfw.GLFW

/**
 * The native (non-browser) ClickGUI, shown only while the `native` theme is selected. It is rendered
 * entirely with the client's own 2D primitives and font renderer, so it works without CEF/MCEF.
 *
 * One draggable [Panel] per module category, modules toggle on click, and module settings expand into
 * interactive [SettingWidget] rows. Panel positions and expansion are persisted via [ClickGuiLayout].
 */
class NativeClickGuiScreen : Screen("NativeClickGui".asPlainText()) {

    private val panels: MutableList<Panel> = ModuleCategories.entries.mapIndexed { index, category ->
        val saved = ClickGuiLayout.data[category.tag]
        val defaultX = 6f + index * (PANEL_WIDTH + 6f)
        val defaultY = 6f + SEARCH_HEIGHT + 6f
        Panel(category, saved?.x?.toFloat() ?: defaultX, saved?.y?.toFloat() ?: defaultY).apply {
            expanded = saved?.expanded ?: true
        }
    }.toMutableList()

    private var query: String = ""
    private var searchFocused = false

    private var draggingPanel: Panel? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = true

    @Suppress("EmptyFunctionBlock")
    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()

        // Dim the game behind the GUI.
        context.drawQuad(0f, 0f, width.toFloat(), height.toFloat(), Color4b(0, 0, 0, 110))

        // Search bar (top-left).
        renderSearchBar(context)
        renderHudEditorButton(context, mx, my)
        renderSettingsButton(context, mx, my)

        for (panel in panels) {
            panel.render(context, mx, my, query)
        }
    }

    private fun renderHudEditorButton(ctx: GuiGraphicsExtractor, mx: Double, my: Double) {
        val hovered = hudButtonHovered(mx, my)
        ctx.drawRoundedRect(
            HUD_BTN_X, 6f, HUD_BTN_X + HUD_BTN_W, 6f + SEARCH_HEIGHT, 3f,
            if (hovered) ClickGuiPalette.ACCENT_DIM else ClickGuiPalette.PANEL_TITLE_BG, ClickGuiPalette.OUTLINE
        )
        val ty = 6f + (SEARCH_HEIGHT - ClickGuiText.lineHeight) / 2f
        ClickGuiText.draw(ctx, "HUD Editor", HUD_BTN_X + HUD_BTN_W / 2f, ty, ClickGuiPalette.TEXT, center = true)
    }

    private fun hudButtonHovered(mx: Double, my: Double) =
        mx >= HUD_BTN_X && mx <= HUD_BTN_X + HUD_BTN_W && my >= 6.0 && my <= 6.0 + SEARCH_HEIGHT

    /**
     * Opens Minecraft's own (vanilla) options screen. We only hand control over to it — the screen itself
     * is left untouched. Passing [this] as the parent makes vanilla return to the ClickGUI on close.
     */
    private fun renderSettingsButton(ctx: GuiGraphicsExtractor, mx: Double, my: Double) {
        val hovered = settingsButtonHovered(mx, my)
        ctx.drawRoundedRect(
            SETTINGS_BTN_X, 6f, SETTINGS_BTN_X + SETTINGS_BTN_W, 6f + SEARCH_HEIGHT, 3f,
            if (hovered) ClickGuiPalette.ACCENT_DIM else ClickGuiPalette.PANEL_TITLE_BG, ClickGuiPalette.OUTLINE
        )
        val ty = 6f + (SEARCH_HEIGHT - ClickGuiText.lineHeight) / 2f
        ClickGuiText.draw(
            ctx, "MC Settings", SETTINGS_BTN_X + SETTINGS_BTN_W / 2f, ty, ClickGuiPalette.TEXT, center = true
        )
    }

    private fun settingsButtonHovered(mx: Double, my: Double) =
        mx >= SETTINGS_BTN_X && mx <= SETTINGS_BTN_X + SETTINGS_BTN_W && my >= 6.0 && my <= 6.0 + SEARCH_HEIGHT

    private fun renderSearchBar(ctx: GuiGraphicsExtractor) {
        val w = 150f
        ctx.drawRoundedRect(6f, 6f, 6f + w, 6f + SEARCH_HEIGHT, 3f, ClickGuiPalette.PANEL_TITLE_BG,
            if (searchFocused) ClickGuiPalette.ACCENT else ClickGuiPalette.OUTLINE)
        val ty = 6f + (SEARCH_HEIGHT - ClickGuiText.lineHeight) / 2f
        val shown = if (query.isEmpty() && !searchFocused) "Search..." else query + if (searchFocused) "_" else ""
        val color = if (query.isEmpty() && !searchFocused) ClickGuiPalette.TEXT_DIM else ClickGuiPalette.TEXT
        ClickGuiText.draw(ctx, shown, 11f, ty, color)
    }

    private fun searchHovered(mx: Double, my: Double) = mx in 6.0..156.0 && my in 6.0..(6.0 + SEARCH_HEIGHT)

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = click.x
        val my = click.y
        val button = click.button()

        // HUD editor button.
        if (hudButtonHovered(mx, my)) {
            mc.execute { mc.gui.setScreen(NativeHudEditorScreen()) }
            return true
        }

        // Minecraft's own (vanilla) options screen.
        if (settingsButtonHovered(mx, my)) {
            mc.execute { mc.gui.setScreen(OptionsScreen(this, mc.options, mc.level != null)) }
            return true
        }

        // Search bar focus.
        if (searchHovered(mx, my)) {
            searchFocused = true
            ClickGuiFocus.focus(null)
            ClickGuiFocus.focusBind(null)
            return true
        }
        searchFocused = false

        // Topmost panel first (reverse order = last rendered is on top).
        for (panel in panels.asReversed()) {
            val hit = panel.mouseClicked(mx, my, button, query)
            if (hit == Panel.HitResult.START_DRAG) {
                draggingPanel = panel
                dragOffsetX = (mx - panel.x).toFloat()
                dragOffsetY = (my - panel.y).toFloat()
                bringToFront(panel)
                return true
            }
            if (hit == Panel.HitResult.CONSUMED) {
                return true
            }
        }
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        val panel = draggingPanel
        if (panel != null) {
            panel.dragged = true
            panel.x = (click.x - dragOffsetX).toFloat()
            panel.y = (click.y - dragOffsetY).toFloat()
            return true
        }
        for (p in panels) {
            if (p.mouseDragged(click.x, click.y, query)) {
                return true
            }
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        val panel = draggingPanel
        if (panel != null) {
            if (!panel.dragged) {
                panel.expanded = !panel.expanded
            }
            panel.dragged = false
            draggingPanel = null
            persistLayout()
        }
        panels.forEach { it.mouseReleased(click.x, click.y, click.button()) }
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(
        mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double
    ): Boolean {
        for (panel in panels.asReversed()) {
            if (panel.mouseScrolled(mouseX, mouseY, verticalAmount, query)) {
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        // Field / bind capture takes priority.
        ClickGuiFocus.bind?.let {
            if (it.keyPressed(input.key, input.modifiers())) {
                return true
            }
        }
        if (searchFocused) {
            when (input.key) {
                GLFW.GLFW_KEY_BACKSPACE -> { if (query.isNotEmpty()) query = query.dropLast(1) }
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> searchFocused = false
                else -> return true
            }
            return true
        }
        ClickGuiFocus.text?.let {
            if (it.keyPressed(input.key, input.modifiers())) {
                return true
            }
        }
        return super.keyPressed(input)
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        val codepoint = input.codepoint()
        if (searchFocused) {
            if (codepoint >= 0x20) {
                query += codepoint.toChar()
            }
            return true
        }
        ClickGuiFocus.text?.let {
            if (it.charTyped(codepoint)) {
                return true
            }
        }
        return super.charTyped(input)
    }

    private fun bringToFront(panel: Panel) {
        panels.remove(panel)
        panels.add(panel)
    }

    private fun persistLayout() {
        panels.forEach { ClickGuiLayout.data[it.category.tag] = PanelSave(it.x.toInt(), it.y.toInt(), it.expanded) }
        ClickGuiLayout.save()
    }

    override fun onClose() {
        ClickGuiFocus.reset()
        persistLayout()
        super.onClose()
    }

    companion object {
        const val PANEL_WIDTH = 110f
        const val SEARCH_HEIGHT = 14f
        private const val HUD_BTN_X = 160f
        private const val HUD_BTN_W = 62f
        private const val SETTINGS_BTN_X = 226f
        private const val SETTINGS_BTN_W = 66f
    }
}

/**
 * A draggable category window listing its modules. Module rows toggle the module; a right-click expands
 * a module's settings into interactive [SettingWidget]s. Long lists scroll with the mouse wheel.
 */
internal class Panel(val category: ModuleCategory, var x: Float, var y: Float) {

    enum class HitResult { NONE, CONSUMED, START_DRAG }

    var expanded = true
    var dragged = false
    private var scroll = 0f

    private val buttons: List<ModuleButton> = ModuleManager
        .filter { it.category == category }
        .sortedBy { it.name }
        .map { ModuleButton(it) }

    private val titleHeight = ClickGuiText.lineHeight + 6f
    private val width get() = NativeClickGuiScreen.PANEL_WIDTH

    private fun visibleButtons(query: String): List<ModuleButton> =
        if (query.isBlank()) buttons else buttons.filter { it.module.name.contains(query, ignoreCase = true) }

    private fun maxContentHeight(): Float = (mc.window.guiScaledHeight - y - titleHeight - 6f).coerceAtLeast(40f)

    private fun contentHeight(query: String): Float {
        var h = 0f
        for (b in visibleButtons(query)) {
            h += b.height(width)
        }
        return h
    }

    private fun visibleContentHeight(query: String): Float =
        if (!expanded) 0f else minOf(contentHeight(query), maxContentHeight())

    fun render(ctx: GuiGraphicsExtractor, mouseX: Double, mouseY: Double, query: String) {
        // Title bar
        ctx.drawRoundedRect(x, y, x + width, y + titleHeight, 3f, ClickGuiPalette.PANEL_TITLE_BG)
        ctx.drawQuad(x, y + titleHeight - 2f, x + width, y + titleHeight, ClickGuiPalette.ACCENT)
        val ty = y + (titleHeight - ClickGuiText.lineHeight) / 2f
        ClickGuiText.draw(ctx, category.tag, x + 5f, ty, ClickGuiPalette.TEXT)
        ClickGuiText.draw(ctx, if (expanded) "-" else "+", x + width - 5f, ty, ClickGuiPalette.TEXT_DIM, anchorRight = true)

        if (!expanded) {
            return
        }

        val visible = visibleButtons(query)
        val visH = visibleContentHeight(query)
        clampScroll(query)

        ctx.drawQuad(x, y + titleHeight, x + width, y + titleHeight + visH, ClickGuiPalette.PANEL_BG)

        // Nothing to clip/draw: an empty (or off-screen) content area would push a 0x0 scissor,
        // which crashes vanilla's GuiRenderer.enableScissor ("Scissor size must be >0").
        if (visH <= 0f || visible.isEmpty()) {
            return
        }

        val clip = ctx.getBounds(x, y + titleHeight, x + width, y + titleHeight + visH)
        if (clip.width() <= 0 || clip.height() <= 0) {
            return
        }
        ctx.scissorStack.withPush(clip) {
            var cy = y + titleHeight - scroll
            for (b in visible) {
                val bh = b.height(width)
                if (cy + bh >= y + titleHeight && cy <= y + titleHeight + visH) {
                    b.render(ctx, x, cy, width, mouseX, mouseY)
                }
                cy += bh
            }
        }
    }

    private fun clampScroll(query: String) {
        val max = (contentHeight(query) - maxContentHeight()).coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, max)
    }

    private fun inTitle(mx: Double, my: Double) =
        mx >= x && mx <= x + width && my >= y && my <= y + titleHeight

    private fun inContent(mx: Double, my: Double, query: String): Boolean {
        if (!expanded) {
            return false
        }
        val visH = visibleContentHeight(query)
        return mx >= x && mx <= x + width && my >= y + titleHeight && my <= y + titleHeight + visH
    }

    fun mouseClicked(mx: Double, my: Double, button: Int, query: String): HitResult {
        if (inTitle(mx, my)) {
            return if (button == 0) HitResult.START_DRAG else HitResult.NONE
        }
        if (!inContent(mx, my, query)) {
            return HitResult.NONE
        }
        var cy = y + titleHeight - scroll
        for (b in visibleButtons(query)) {
            val bh = b.height(width)
            if (my >= cy && my < cy + bh) {
                b.mouseClicked(mx, my, button, x, cy, width)
                return HitResult.CONSUMED
            }
            cy += bh
        }
        return HitResult.CONSUMED
    }

    fun mouseDragged(mx: Double, my: Double, query: String): Boolean {
        if (!expanded) {
            return false
        }
        var cy = y + titleHeight - scroll
        for (b in visibleButtons(query)) {
            b.mouseDragged(mx, my, x, cy, width)
            cy += b.height(width)
        }
        return false
    }

    fun mouseReleased(mx: Double, my: Double, button: Int) {
        buttons.forEach { it.mouseReleased(mx, my, button) }
    }

    fun mouseScrolled(mx: Double, my: Double, amount: Double, query: String): Boolean {
        if (!inContent(mx, my, query)) {
            return false
        }
        scroll -= (amount * 14.0).toFloat()
        clampScroll(query)
        return true
    }
}

/**
 * A single module row inside a [Panel]. Left-click toggles the module, right-click expands its settings.
 */
internal class ModuleButton(val module: ClientModule) {

    private var expanded = false
    private val rowHeight = ClickGuiText.lineHeight + 6f
    private val widgets: MutableList<SettingWidget> by lazy { SettingWidget.childrenOf(module) }

    fun height(width: Float): Float {
        var h = rowHeight
        if (expanded) {
            val cw = width - SETTINGS_INDENT * 2f
            for (w in widgets) {
                h += w.height(cw)
            }
        }
        return h
    }

    fun render(ctx: GuiGraphicsExtractor, x: Float, y: Float, width: Float, mouseX: Double, mouseY: Double) {
        val hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + rowHeight
        ctx.drawQuad(x, y, x + width, y + rowHeight, if (hovered) ClickGuiPalette.ROW_HOVER else ClickGuiPalette.ROW_BG)
        if (module.enabled) {
            ctx.drawQuad(x, y, x + 2f, y + rowHeight, ClickGuiPalette.ACCENT)
        }
        val ty = y + (rowHeight - ClickGuiText.lineHeight) / 2f
        ClickGuiText.draw(ctx, module.name, x + 6f, ty, if (module.enabled) ClickGuiPalette.TEXT else ClickGuiPalette.TEXT_DIM)
        if (widgets.isNotEmpty()) {
            ClickGuiText.draw(ctx, if (expanded) "-" else "+", x + width - 5f, ty, ClickGuiPalette.TEXT_DIM, anchorRight = true)
        }

        if (!expanded) {
            return
        }
        var cy = y + rowHeight
        val cx = x + SETTINGS_INDENT
        val cw = width - SETTINGS_INDENT * 2f
        for (w in widgets) {
            w.render(ctx, cx, cy, cw, mouseX, mouseY)
            cy += w.height(cw)
        }
    }

    fun mouseClicked(mx: Double, my: Double, button: Int, x: Float, y: Float, width: Float) {
        if (my < y + rowHeight) {
            when (button) {
                0 -> module.enabled = !module.enabled
                1 -> if (widgets.isNotEmpty()) expanded = !expanded
            }
            return
        }
        if (!expanded) {
            return
        }
        var cy = y + rowHeight
        val cx = x + SETTINGS_INDENT
        val cw = width - SETTINGS_INDENT * 2f
        for (w in widgets) {
            val wh = w.height(cw)
            if (my >= cy && my < cy + wh) {
                w.mouseClicked(mx, my, button, cx, cy, cw)
                return
            }
            cy += wh
        }
    }

    fun mouseDragged(mx: Double, my: Double, x: Float, y: Float, width: Float) {
        if (!expanded) {
            return
        }
        var cy = y + rowHeight
        val cw = width - SETTINGS_INDENT * 2f
        for (w in widgets) {
            w.mouseDragged(mx, my, x + SETTINGS_INDENT, cy, cw)
            cy += w.height(cw)
        }
    }

    fun mouseReleased(mx: Double, my: Double, button: Int) {
        widgets.forEach { it.mouseReleased(mx, my, button) }
    }

    companion object {
        private const val SETTINGS_INDENT = 6f
    }
}
