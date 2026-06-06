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
package net.ccbluex.liquidbounce.render.gui.clickgui.setting

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.types.BindValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiTheme
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth
import org.lwjgl.glfw.GLFW

private const val ROW_HEIGHT: Int = 20
private const val CHECKBOX_SIZE: Int = 12
private const val SLIDER_HEIGHT: Int = 6
private const val SLIDER_KNOB: Int = 8
private const val ARROW_W: Int = 10

/**
 * Dispatches a [Value] to the most appropriate [GenericSetting] widget based
 * on its runtime [ValueType]. Returns `null` for value types the GUI does
 * not yet render (curves, vectors, registry lists, etc.).
 */
fun createSetting(value: Value<*>): GenericSetting? {
    if (value.doNotInclude.asBoolean || value.notAnOption) return null

    @Suppress("UNCHECKED_CAST")
    return when {
        value is ModeValueGroup<*> -> ModeGroupSetting(value)
        value is MultiChoiceListValue<*> -> MultiEnumSetting(value as MultiChoiceListValue<Tagged>)
        value is ChoiceListValue<*> -> ChoiceSetting(value as ChoiceListValue<Tagged>)
        value is BindValue -> BindSetting(value)
        value.valueType == ValueType.COLOR -> ColorSetting(value as Value<Color4b>)
        value is RangedValue<*> -> when (value.valueType) {
            ValueType.INT -> IntSetting(value)
            ValueType.FLOAT -> FloatSetting(value)
            else -> null
        }
        value.valueType == ValueType.BOOLEAN -> BooleanSetting(value as Value<Boolean>)
        else -> null
    }
}

private fun GuiGraphicsExtractor.drawTextClipped(text: String, x: Int, y: Int, color: Color4b, maxWidth: Int = Int.MAX_VALUE) {
    val trimmed = if (mc.font.width(text) > maxWidth && maxWidth > 4) {
        var s = text
        while (s.isNotEmpty() && mc.font.width("$s…") > maxWidth) s = s.dropLast(1)
        "$s…"
    } else text
    text(mc.font, trimmed, x, y, color.argb, true)
}

class BooleanSetting(override val value: Value<Boolean>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBg.argb)
        context.drawTextClipped(
            displayName, x + 4, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textNormal else ClickGuiTheme.textDimmed,
            width - 4 - CHECKBOX_SIZE - 8
        )
        val isOn = value.get()
        val checkX = x + width - CHECKBOX_SIZE - 6
        val checkY = y + (ROW_HEIGHT - CHECKBOX_SIZE) / 2
        context.fill(checkX, checkY, checkX + CHECKBOX_SIZE, checkY + CHECKBOX_SIZE, ClickGuiTheme.checkboxOff.argb)
        if (isOn) {
            context.fill(
                checkX + 2, checkY + 2,
                checkX + CHECKBOX_SIZE - 2, checkY + CHECKBOX_SIZE - 2,
                ClickGuiTheme.checkboxOn.argb
            )
        }
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0) {
            value.set(!value.get())
            return true
        }
        return false
    }
}

class IntSetting(override val value: RangedValue<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT
    private var dragging: Boolean = false
    private var dragStartX: Int = 0
    private var dragStartVal: Int = 0
    private var rowLeft: Int = 0
    private var rowWidth: Int = 0

    @Suppress("UNCHECKED_CAST")
    private val typed: RangedValue<Int>
        get() = value as RangedValue<Int>

    private fun rangeMin(): Int = (value.range as IntRange).first
    private fun rangeMax(): Int = (value.range as IntRange).last

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowLeft = x
        rowWidth = width
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBg.argb)
        context.drawTextClipped(
            displayName, x + 4, y + 2,
            if (hovered) ClickGuiTheme.textNormal else ClickGuiTheme.textDimmed,
            width - 60
        )
        val valueText = value.get().toString()
        val tw = mc.font.width(valueText)
        context.text(mc.font, valueText, x + width - tw - 4, y + 2, ClickGuiTheme.textNormal.argb, true)
        val trackX = x + 4
        val trackW = width - 8
        val trackY = y + ROW_HEIGHT - SLIDER_HEIGHT - 3
        context.fill(trackX, trackY, trackX + trackW, trackY + SLIDER_HEIGHT, ClickGuiTheme.sliderTrack.argb)
        val cur = typed.get()
        val span = (rangeMax() - rangeMin()).coerceAtLeast(1)
        val ratio = ((cur - rangeMin()).toFloat() / span).coerceIn(0f, 1f)
        val filled = (trackW * ratio).toInt()
        context.fill(trackX, trackY, trackX + filled, trackY + SLIDER_HEIGHT, ClickGuiTheme.sliderFill.argb)
        val knobX = trackX + filled - SLIDER_KNOB / 2
        val knobY = trackY - 1
        context.fill(knobX, knobY, knobX + SLIDER_KNOB, knobY + SLIDER_HEIGHT + 2, ClickGuiTheme.sliderKnob.argb)
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        dragging = true
        dragStartX = mouseX
        dragStartVal = typed.get()
        return true
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0 && dragging) {
            dragging = false
            return true
        }
        return false
    }

    override fun mouseDragged(mouseX: Int, mouseY: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        if (dragging) {
            val trackW = rowWidth - 8
            val delta = (mouseX - dragStartX).toFloat()
            val span = (rangeMax() - rangeMin()).coerceAtLeast(1)
            val newVal = dragStartVal + (delta / trackW.coerceAtLeast(1) * span).toInt()
            typed.set(newVal.coerceIn(rangeMin(), rangeMax()))
            return true
        }
        return false
    }
}

class FloatSetting(override val value: RangedValue<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT
    private var dragging: Boolean = false
    private var dragStartX: Int = 0
    private var dragStartVal: Float = 0f
    private var rowWidth: Int = 0

    @Suppress("UNCHECKED_CAST")
    private val typed: RangedValue<Float>
        get() = value as RangedValue<Float>

    private fun rangeMin(): Float = (value.range as ClosedFloatingPointRange<Float>).start
    private fun rangeMax(): Float = (value.range as ClosedFloatingPointRange<Float>).endInclusive

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowWidth = width
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBg.argb)
        context.drawTextClipped(
            displayName, x + 4, y + 2,
            if (hovered) ClickGuiTheme.textNormal else ClickGuiTheme.textDimmed,
            width - 60
        )
        val raw = typed.get()
        val valueText = String.format("%.2f", raw)
        val tw = mc.font.width(valueText)
        context.text(mc.font, valueText, x + width - tw - 4, y + 2, ClickGuiTheme.textNormal.argb, true)
        val trackX = x + 4
        val trackW = width - 8
        val trackY = y + ROW_HEIGHT - SLIDER_HEIGHT - 3
        context.fill(trackX, trackY, trackX + trackW, trackY + SLIDER_HEIGHT, ClickGuiTheme.sliderTrack.argb)
        val span = (rangeMax() - rangeMin()).coerceAtLeast(0.0001f)
        val ratio = ((raw - rangeMin()) / span).coerceIn(0f, 1f)
        val filled = (trackW * ratio).toInt()
        context.fill(trackX, trackY, trackX + filled, trackY + SLIDER_HEIGHT, ClickGuiTheme.sliderFill.argb)
        val knobX = trackX + filled - SLIDER_KNOB / 2
        val knobY = trackY - 1
        context.fill(knobX, knobY, knobX + SLIDER_KNOB, knobY + SLIDER_HEIGHT + 2, ClickGuiTheme.sliderKnob.argb)
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        dragging = true
        dragStartX = mouseX
        dragStartVal = typed.get()
        return true
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0 && dragging) {
            dragging = false
            return true
        }
        return false
    }

    override fun mouseDragged(mouseX: Int, mouseY: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        if (dragging) {
            val trackW = rowWidth - 8
            val delta = (mouseX - dragStartX).toFloat()
            val span = (rangeMax() - rangeMin()).coerceAtLeast(0.0001f)
            val newVal = dragStartVal + (delta / trackW.coerceAtLeast(1)) * span
            typed.set(newVal.coerceIn(rangeMin(), rangeMax()))
            return true
        }
        return false
    }
}

class ChoiceSetting(override val value: ChoiceListValue<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBg.argb)
        context.drawTextClipped(
            displayName, x + 4, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textNormal else ClickGuiTheme.textDimmed,
            width - 4 - ARROW_W * 2 - 8 - 60
        )
        val active = value.get() ?: return ROW_HEIGHT
        val text = active.tag
        val tw = mc.font.width(text)
        val textX = x + width - ARROW_W * 2 - tw - 8
        context.text(mc.font, text, textX, y + (ROW_HEIGHT - 8) / 2, ClickGuiTheme.textNormal.argb, true)
        val arrowY = y + ROW_HEIGHT / 2
        val leftX = x + width - ARROW_W * 2 - 4
        val rightX = x + width - ARROW_W - 2
        context.fill(leftX + 2, arrowY - 2, leftX + ARROW_W - 2, arrowY + 1, ClickGuiTheme.textDimmed.argb)
        context.fill(rightX + 2, arrowY - 1, rightX + ARROW_W - 2, arrowY + 1, ClickGuiTheme.textDimmed.argb)
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        val active = value.get() ?: return false
        val choices = value.choices.toList()
        if (choices.size < 2) return false
        val cur = choices.indexOf(active)
        val next = choices[(cur + 1) % choices.size]
        value.setByString(next.tag)
        return true
    }
}

class ColorSetting(override val value: Value<Color4b>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT + 4 * 8 + 4
    private var draggingIndex: Int = -1
    private var rowWidth: Int = 0

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowWidth = width
        context.fill(x, y, x + width, y + height, ClickGuiTheme.settingsBg.argb)
        context.text(mc.font, displayName, x + 4, y + 2, ClickGuiTheme.textNormal.argb, true)
        val c = value.get()
        val swatchX = x + width - 18
        val swatchY = y + 2
        context.fill(swatchX, swatchY, swatchX + 14, swatchY + 14, c.argb)
        val sliderX = x + 4
        val sliderW = width - 8
        val comps = intArrayOf(c.r, c.g, c.b, c.a)
        for (idx in 0..3) {
            val trackY = y + ROW_HEIGHT + idx * 8
            context.fill(sliderX, trackY, sliderX + sliderW, trackY + SLIDER_HEIGHT, ClickGuiTheme.sliderTrack.argb)
            val ratio = comps[idx] / 255f
            val filled = (sliderW * ratio).toInt()
            context.fill(sliderX, trackY, sliderX + filled, trackY + SLIDER_HEIGHT, ClickGuiTheme.sliderFill.argb)
            val knobX = sliderX + filled - SLIDER_KNOB / 2
            context.fill(knobX, trackY - 1, knobX + SLIDER_KNOB, trackY + SLIDER_HEIGHT + 2, ClickGuiTheme.sliderKnob.argb)
        }
        return height
    }

    private fun sliderAt(my: Int, y: Int): Int {
        val rel = my - (y + ROW_HEIGHT)
        return when {
            rel < 0 -> -1
            rel < 8 -> 0
            rel < 16 -> 1
            rel < 24 -> 2
            rel < 32 -> 3
            else -> -1
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        draggingIndex = -1
        return true
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0 && draggingIndex != -1) {
            draggingIndex = -1
            return true
        }
        return false
    }

    override fun mouseDragged(mouseX: Int, mouseY: Int, button: Int, dragX: Double, dragY: Double): Boolean {
        return false
    }
}

class MultiEnumSetting(override val value: MultiChoiceListValue<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBg.argb)
        context.drawTextClipped(
            displayName, x + 4, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textNormal else ClickGuiTheme.textDimmed,
            width - 50
        )
        val selected = value.get().size
        val total = value.choices.size
        val summary = "$selected / $total"
        val tw = mc.font.width(summary)
        context.text(mc.font, summary, x + width - tw - 4, y + (ROW_HEIGHT - 8) / 2, ClickGuiTheme.textNormal.argb, true)
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        val current = value.get()
        val choices = value.choices
        val next = choices.firstOrNull { it !in current } ?: choices.firstOrNull()
        if (next != null) {
            @Suppress("UNCHECKED_CAST")
            (value as MultiChoiceListValue<Tagged>).toggle(next)
        }
        return true
    }
}

class ModeGroupSetting(override val value: ModeValueGroup<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBorder.argb)
        val text = "${displayName}: ${value.activeMode.tag}"
        context.text(mc.font, text, x + 4, y + (ROW_HEIGHT - 8) / 2, Color4b.WHITE.argb, true)
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        val modes = value.modes
        if (modes.size < 2) return false
        val idx = modes.indexOf(value.activeMode)
        val next = modes[(idx + 1) % modes.size]
        value.setByString(next.tag)
        return true
    }
}

class BindSetting(override val value: BindValue) : GenericSetting() {
    override val height: Int = ROW_HEIGHT
    private var waitingForKey: Boolean = false

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.fill(x, y, x + width, y + ROW_HEIGHT, ClickGuiTheme.settingsBg.argb)
        context.drawTextClipped(
            displayName, x + 4, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textNormal else ClickGuiTheme.textDimmed,
            width - 60
        )
        val bind = value.get()
        val label = if (waitingForKey) "..." else bind.keyName
        val tw = mc.font.width(label)
        context.text(
            mc.font, label, x + width - tw - 4, y + (ROW_HEIGHT - 8) / 2,
            if (waitingForKey) ClickGuiTheme.moduleHighlight.argb else ClickGuiTheme.textNormal.argb,
            true
        )
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0) {
            waitingForKey = !waitingForKey
            return true
        }
        if (button == 1 && !waitingForKey) {
            val current = value.get()
            value.set(InputBind(current.boundKey.type, GLFW.GLFW_KEY_UNKNOWN, current.action))
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!waitingForKey) return false
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            waitingForKey = false
            return true
        }
        val current = value.get()
        value.set(InputBind(current.boundKey.type, keyCode, current.action))
        waitingForKey = false
        return true
    }
}
