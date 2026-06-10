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
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiTheme
import net.ccbluex.liquidbounce.render.gui.clickgui.spacedName
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW

private const val ROW_HEIGHT = 22
private const val SLIDER_HEIGHT = 4
private const val SLIDER_KNOB = 8

fun createSetting(value: Value<*>): GenericSetting? {
    if (value.doNotInclude.asBoolean || value.notAnOption) return null

    @Suppress("UNCHECKED_CAST")
    val widget: GenericSetting = when {
        value is ModeValueGroup<*> -> ModeGroupSetting(value)
        value is MultiChoiceListValue<*> -> MultiEnumSetting(value as MultiChoiceListValue<Tagged>)
        value is ChoiceListValue<*> -> ChoiceSetting(value as ChoiceListValue<Tagged>)
        value is BindValue -> BindSetting(value)
        value is RangedValue<*> -> when (value.valueType) {
            ValueType.INT -> IntSetting(value)
            ValueType.FLOAT -> FloatSetting(value)
            ValueType.INT_RANGE -> IntRangeSetting(value)
            ValueType.FLOAT_RANGE -> fallback(value)
            else -> fallback(value)
        }
        else -> when (value.valueType) {
            ValueType.BOOLEAN -> BooleanSetting(value as Value<Boolean>)
            ValueType.COLOR -> ColorSetting(value as Value<Color4b>)
            ValueType.TEXT -> TextSetting(value as Value<String>)
            else -> fallback(value)
        }
    }
    return widget
}

private fun fallback(value: Value<*>): GenericSetting = FallbackSetting(value)

private class FallbackSetting(override val value: Value<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        val text = "${displayName}: ${value.get()}"
        drawTextClipped(context, text, x + 6, y + (ROW_HEIGHT - 8) / 2,
            ClickGuiTheme.textDimmed, width - 12)
        return ROW_HEIGHT
    }
}

private fun drawTextClipped(context: GuiGraphicsExtractor, text: String, x: Int, y: Int, color: Color4b, maxWidth: Int = Int.MAX_VALUE) {
    val trimmed = if (mc.font.width(text) > maxWidth && maxWidth > 4) {
        var s = text
        while (s.isNotEmpty() && mc.font.width("$s…") > maxWidth) s = s.dropLast(1)
        "$s…"
    } else text
    context.text(mc.font, trimmed, x, y, color.argb, true)
}

class BooleanSetting(override val value: Value<Boolean>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 50
        )
        val isOn = value.get()
        val toggleX = x + width - 44
        val toggleY = y + (ROW_HEIGHT - ClickGuiTheme.toggleHeight) / 2
        val tw = ClickGuiTheme.toggleWidth
        val th = ClickGuiTheme.toggleHeight
        val radius = th / 2f
        context.drawRoundedRect(
            toggleX.toFloat(), toggleY.toFloat(),
            (toggleX + tw).toFloat(), (toggleY + th).toFloat(),
            radius,
            fillColor = if (isOn) ClickGuiTheme.toggleEnabled else ClickGuiTheme.toggleBg,
        )
        val knobRadius = (th - 4) / 2f
        val knobX = if (isOn) toggleX + tw - th + 2 else toggleX + 2
        context.drawRoundedRect(
            knobX.toFloat(), (toggleY + 2).toFloat(),
            (knobX + knobRadius * 2).toFloat(), (toggleY + 2 + knobRadius * 2).toFloat(),
            knobRadius,
            fillColor = ClickGuiTheme.toggleKnob,
        )
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
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowLeft = x
        rowWidth = width
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + 2,
            if (hovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 60
        )
        val valueText = value.get().toString()
        val tw = mc.font.width(valueText)
        context.text(mc.font, valueText, x + width - tw - 6, y + 2, ClickGuiTheme.textPrimary.argb, true)

        val trackX = x + 6
        val trackW = width - 12
        val trackY = y + ROW_HEIGHT - SLIDER_HEIGHT - 5
        context.drawRoundedRect(
            trackX.toFloat(), trackY.toFloat(),
            (trackX + trackW).toFloat(), (trackY + SLIDER_HEIGHT).toFloat(),
            (SLIDER_HEIGHT / 2f),
            fillColor = ClickGuiTheme.sliderBg,
        )
        val cur = typed.get()
        val span = (rangeMax() - rangeMin()).coerceAtLeast(1)
        val ratio = ((cur - rangeMin()).toFloat() / span).coerceIn(0f, 1f)
        val filled = (trackW * ratio).toInt().coerceAtLeast(0)
        if (filled > 0) {
            context.drawRoundedRect(
                trackX.toFloat(), trackY.toFloat(),
                (trackX + filled).toFloat(), (trackY + SLIDER_HEIGHT).toFloat(),
                (SLIDER_HEIGHT / 2f),
                fillColor = ClickGuiTheme.sliderFill,
            )
        }
        val knobX = trackX + filled - SLIDER_KNOB / 2
        val knobY = trackY - (SLIDER_KNOB - SLIDER_HEIGHT) / 2
        context.drawRoundedRect(
            knobX.toFloat(), knobY.toFloat(),
            (knobX + SLIDER_KNOB).toFloat(), (knobY + SLIDER_KNOB).toFloat(),
            (SLIDER_KNOB / 2f),
            fillColor = ClickGuiTheme.sliderKnob,
        )
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
            val trackW = rowWidth - 12
            val delta = (mouseX - dragStartX).toFloat()
            val span = (rangeMax() - rangeMin()).coerceAtLeast(1)
            val newVal = dragStartVal + (delta / trackW.coerceAtLeast(1) * span).toInt()
            typed.set(newVal.coerceIn(rangeMin(), rangeMax()))
            return true
        }
        return false
    }
}

class IntRangeSetting(override val value: RangedValue<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT
    private var rowX: Int = 0
    private var rowW: Int = 0

    @Suppress("UNCHECKED_CAST")
    private val typed: RangedValue<IntRange>
        get() = value as RangedValue<IntRange>

    private fun rangeMin(): Int = (value.range as IntRange).first
    private fun rangeMax(): Int = (value.range as IntRange).last

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowX = x
        rowW = width
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 70
        )
        val range = typed.get()
        val text = "${range.first} - ${range.last}"
        val tw = mc.font.width(text)
        context.text(mc.font, text, x + width - tw - 6, y + (ROW_HEIGHT - 8) / 2,
            ClickGuiTheme.textPrimary.argb, true)
        return ROW_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        val range = typed.get()
        val delta = when (button) {
            0 -> 1
            1 -> -1
            else -> return false
        }
        val mid = rowX + rowW / 2
        val curMin = range.first
        val curMax = range.last
        val (newMin, newMax) = if (mouseX < mid) {
            curMin + delta to curMax
        } else {
            curMin to curMax + delta
        }
        val clampedMin = newMin.coerceIn(rangeMin(), curMax)
        val clampedMax = newMax.coerceIn(curMin, rangeMax())
        typed.set(clampedMin..clampedMax)
        return true
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
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowWidth = width
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + 2,
            if (hovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 60
        )
        val raw = typed.get()
        val valueText = String.format("%.2f", raw)
        val tw = mc.font.width(valueText)
        context.text(mc.font, valueText, x + width - tw - 6, y + 2, ClickGuiTheme.textPrimary.argb, true)

        val trackX = x + 6
        val trackW = width - 12
        val trackY = y + ROW_HEIGHT - SLIDER_HEIGHT - 5
        context.drawRoundedRect(
            trackX.toFloat(), trackY.toFloat(),
            (trackX + trackW).toFloat(), (trackY + SLIDER_HEIGHT).toFloat(),
            (SLIDER_HEIGHT / 2f),
            fillColor = ClickGuiTheme.sliderBg,
        )
        val span = (rangeMax() - rangeMin()).coerceAtLeast(0.0001f)
        val ratio = ((raw - rangeMin()) / span).coerceIn(0f, 1f)
        val filled = (trackW * ratio).toInt().coerceAtLeast(0)
        if (filled > 0) {
            context.drawRoundedRect(
                trackX.toFloat(), trackY.toFloat(),
                (trackX + filled).toFloat(), (trackY + SLIDER_HEIGHT).toFloat(),
                (SLIDER_HEIGHT / 2f),
                fillColor = ClickGuiTheme.sliderFill,
            )
        }
        val knobX = trackX + filled - SLIDER_KNOB / 2
        val knobY = trackY - (SLIDER_KNOB - SLIDER_HEIGHT) / 2
        context.drawRoundedRect(
            knobX.toFloat(), knobY.toFloat(),
            (knobX + SLIDER_KNOB).toFloat(), (knobY + SLIDER_KNOB).toFloat(),
            (SLIDER_KNOB / 2f),
            fillColor = ClickGuiTheme.sliderKnob,
        )
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
            val trackW = rowWidth - 12
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
    override val height: Int = ROW_HEIGHT + 2

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 80
        )
        val active = value.get() ?: return ROW_HEIGHT
        val text = active.tag
        val tw = mc.font.width(text)
        val textX = x + width - tw - 24
        context.text(mc.font, text, textX, y + (ROW_HEIGHT - 8) / 2, ClickGuiTheme.accent.argb, true)

        val arrowX = x + width - 16
        val arrowY = y + ROW_HEIGHT / 2
        context.fill(arrowX - 3, arrowY - 2, arrowX, arrowY + 1, ClickGuiTheme.textDimmed.argb)
        context.fill(arrowX + 3, arrowY - 2, arrowX + 6, arrowY + 1, ClickGuiTheme.textDimmed.argb)
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
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        rowWidth = width
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + height).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        context.text(mc.font, displayName, x + 6, y + 3, ClickGuiTheme.textPrimary.argb, true)
        val c = value.get()
        context.drawRoundedRect(
            (x + width - 20).toFloat(), (y + 3).toFloat(),
            (x + width - 6).toFloat(), (y + 17).toFloat(),
            3f,
            fillColor = c,
            outlineColor = ClickGuiTheme.borderLight,
            outlineWidth = 1f,
        )
        val sliderX = x + 6
        val sliderW = width - 12
        val comps = intArrayOf(c.r, c.g, c.b, c.a)
        for (idx in 0..3) {
            val trackY = y + ROW_HEIGHT + idx * 8
            context.drawRoundedRect(
                sliderX.toFloat(), trackY.toFloat(),
                (sliderX + sliderW).toFloat(), (trackY + SLIDER_HEIGHT).toFloat(),
                (SLIDER_HEIGHT / 2f),
                fillColor = ClickGuiTheme.sliderBg,
            )
            val ratio = comps[idx] / 255f
            val filled = (sliderW * ratio).toInt().coerceAtLeast(0)
            if (filled > 0) {
                context.drawRoundedRect(
                    sliderX.toFloat(), trackY.toFloat(),
                    (sliderX + filled).toFloat(), (trackY + SLIDER_HEIGHT).toFloat(),
                    (SLIDER_HEIGHT / 2f),
                    fillColor = ClickGuiTheme.sliderFill,
                )
            }
            val knobX = sliderX + filled - SLIDER_KNOB / 2
            context.drawRoundedRect(
                knobX.toFloat(), (trackY - 1).toFloat(),
                (knobX + SLIDER_KNOB).toFloat(), (trackY + SLIDER_HEIGHT + 2).toFloat(),
                (SLIDER_KNOB / 2f),
                fillColor = ClickGuiTheme.sliderKnob,
            )
        }
        return height
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
    private var expanded: Boolean = false
    private val checkboxRow = 18
    private val checkSize = 8
    private var renderY: Int = 0

    override val height: Int
        get() = if (expanded) ROW_HEIGHT + value.choices.size * checkboxRow else ROW_HEIGHT

    private val typed: MultiChoiceListValue<Tagged>
        @Suppress("UNCHECKED_CAST")
        get() = value as MultiChoiceListValue<Tagged>

    @Suppress("UNCHECKED_CAST")
    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        renderY = y
        val headerHovered = mouseX in x..(x + width) && mouseY in y..(y + ROW_HEIGHT)
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + (ROW_HEIGHT - 8) / 2,
            if (headerHovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 60
        )
        val selected = value.get()
        val tags = selected.joinToString(", ") { (it as Tagged).tag }
        val summary = if (tags.length <= 28) tags else "${selected.size} / ${value.choices.size}"
        val tw = mc.font.width(summary)
        context.text(
            mc.font, summary,
            x + width - tw - 6, y + (ROW_HEIGHT - 8) / 2,
            ClickGuiTheme.accent.argb, true
        )

        val arrowX = x + width - 16
        val arrowY = y + ROW_HEIGHT / 2
        val arrowColor = if (expanded) ClickGuiTheme.accent else ClickGuiTheme.textDimmed
        if (expanded) {
            context.fill(arrowX - 3, arrowY, arrowX, arrowY + 1, arrowColor.argb)
            context.fill(arrowX - 2, arrowY - 1, arrowX - 1, arrowY, arrowColor.argb)
        } else {
            context.fill(arrowX - 3, arrowY - 1, arrowX, arrowY, arrowColor.argb)
            context.fill(arrowX - 1, arrowY, arrowX, arrowY + 3, arrowColor.argb)
        }

        if (expanded) {
            val choices = value.choices.toList()
            var cy = y + ROW_HEIGHT
            for (choice in choices) {
                val isSelected = choice in selected
                val itemHovered = mouseX in x..(x + width) && mouseY in cy..(cy + checkboxRow)

                if (itemHovered) {
                    context.drawRoundedRect(
                        (x + 2).toFloat(), cy.toFloat(),
                        (x + width - 2).toFloat(), (cy + checkboxRow).toFloat(),
                        3f,
                        fillColor = ClickGuiTheme.bgCardHover,
                    )
                }

                val checkX = x + 10
                val checkY = cy + (checkboxRow - checkSize) / 2
                context.drawRoundedRect(
                    checkX.toFloat(), checkY.toFloat(),
                    (checkX + checkSize).toFloat(), (checkY + checkSize).toFloat(),
                    2f,
                    fillColor = if (isSelected) ClickGuiTheme.toggleEnabled else ClickGuiTheme.toggleBg,
                )
                if (isSelected) {
                    context.fill(checkX + 2, checkY + 3, checkX + 3, checkY + 5, ClickGuiTheme.toggleKnob.argb)
                    context.fill(checkX + 3, checkY + 5, checkX + 5, checkY + 3, ClickGuiTheme.toggleKnob.argb)
                    context.fill(checkX + 5, checkY + 3, checkX + 6, checkY + 2, ClickGuiTheme.toggleKnob.argb)
                }

                val labelX = checkX + checkSize + 6
                context.text(
                    mc.font, choice.tag,
                    labelX, cy + (checkboxRow - 8) / 2,
                    if (isSelected) ClickGuiTheme.textPrimary.argb else ClickGuiTheme.textSecondary.argb,
                    true
                )
                cy += checkboxRow
            }
        }
        return height
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0) {
            if (!expanded) {
                expanded = true
                return true
            }
            val choices = value.choices.toList()
            val listStartY = renderY + ROW_HEIGHT
            val relY = mouseY - listStartY
            val choiceIndex = relY / checkboxRow
            if (choiceIndex in choices.indices && relY >= 0) {
                typed.toggle(choices[choiceIndex])
                return true
            }
            return false
        }
        if (button == 1) {
            if (expanded) {
                expanded = false
                return true
            }
        }
        return false
    }
}

class ModeGroupSetting(override val value: ModeValueGroup<*>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.accentGlow,
            outlineColor = ClickGuiTheme.borderAccent,
            outlineWidth = 1f,
        )
        val text = "${displayName}: ${value.activeMode.tag}"
        context.text(mc.font, text, x + 6, y + (ROW_HEIGHT - 8) / 2, ClickGuiTheme.textPrimary.argb, true)
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
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        drawTextClipped(
            context, displayName, x + 6, y + (ROW_HEIGHT - 8) / 2,
            if (hovered) ClickGuiTheme.textPrimary else ClickGuiTheme.textSecondary,
            width - 60
        )
        val bind = value.get()
        val label = if (waitingForKey) "..." else bind.keyName
        val tw = mc.font.width(label)
        context.text(
            mc.font, label, x + width - tw - 6, y + (ROW_HEIGHT - 8) / 2,
            if (waitingForKey) ClickGuiTheme.accent.argb else ClickGuiTheme.textPrimary.argb,
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

class TextSetting(override val value: Value<String>) : GenericSetting() {
    override val height: Int = ROW_HEIGHT

    override fun render(
        context: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        partialTick: Float,
        hovered: Boolean,
    ): Int {
        context.drawRoundedRect(
            x.toFloat(), y.toFloat(),
            (x + width).toFloat(), (y + ROW_HEIGHT).toFloat(),
            4f,
            fillColor = ClickGuiTheme.bgCard,
        )
        val text = "${displayName}: ${value.get()}"
        drawTextClipped(context, text, x + 6, y + (ROW_HEIGHT - 8) / 2,
            ClickGuiTheme.textDimmed, width - 12)
        return ROW_HEIGHT
    }
}
