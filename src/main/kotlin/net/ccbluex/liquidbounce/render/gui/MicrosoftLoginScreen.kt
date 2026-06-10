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
package net.ccbluex.liquidbounce.render.gui

import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiTheme
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Desktop
import java.net.URI

class MicrosoftLoginScreen(private val parent: Screen?) : Screen(Component.literal("Microsoft Login")) {

    private val theme = ClickGuiTheme
    private val boxW = 440

    private var loginUrl: String? = null
    private var loginSuccess = false
    private var loginError: String? = null
    private var previousUsername: String? = null

    private var copyButton: Button? = null
    private var openButton: Button? = null
    private var backButton: Button? = null

    private fun boxX() = (this.width - boxW) / 2

    override fun init() {
        previousUsername = mc.user?.name

        AccountManager.newMicrosoftAccount { url ->
            loginUrl = url
        }

        val bx = boxX()
        val btnY = this.height / 2 + 40
        val btnW = 130
        val btnH = 20

        copyButton = Button.builder(Component.literal("Copy URL")) {
            loginUrl?.let { url ->
                try {
                    org.lwjgl.glfw.GLFW.glfwSetClipboardString(mc.window.handle(), url)
                } catch (_: Exception) {}
            }
        }.bounds(bx + 10, btnY, btnW, btnH).build().also { addRenderableWidget(it) }

        openButton = Button.builder(Component.literal("Open in Browser")) {
            loginUrl?.let { url ->
                try {
                    org.lwjgl.glfw.GLFW.glfwSetClipboardString(mc.window.handle(), url)
                } catch (_: Exception) {}
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI.create(url))
                    }
                } catch (_: Exception) {}
            }
        }.bounds(bx + 10 + btnW + 10, btnY, btnW + 20, btnH).build().also { addRenderableWidget(it) }

        backButton = Button.builder(Component.literal("Back")) {
            mc.setScreen(parent)
        }.bounds(bx + boxW - 80, this.height / 2 - 100, 70, 18).build().also { addRenderableWidget(it) }

        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        val hasUrl = loginUrl != null
        copyButton?.visible = hasUrl
        openButton?.visible = hasUrl
    }

    override fun removed() {
        copyButton = null
        openButton = null
        backButton = null
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val currentUsername = mc.user?.name
        if (!loginSuccess && currentUsername != null && currentUsername != previousUsername) {
            loginSuccess = true
        }

        context.fill(0, 0, this.width, this.height, 0x60080810.toInt())

        val bx = boxX()
        val boxH = 180
        val boxTop = this.height / 2 - boxH / 2

        with(context) {
            drawRoundedRect(
                bx.toFloat(), boxTop.toFloat(),
                (bx + boxW).toFloat(), (boxTop + boxH).toFloat(), 8f,
                fillColor = theme.bgPrimary,
            )
            drawRoundedRect(
                bx.toFloat(), boxTop.toFloat(),
                (bx + boxW).toFloat(), (boxTop + 30).toFloat(), 8f,
                fillColor = theme.bgSecondary,
            )
        }

        drawText(context, "Microsoft Login", (bx + 14).toFloat(), (boxTop + 9).toFloat(), theme.textPrimary)

        super.extractRenderState(context, mouseX, mouseY, delta)

        val centerX = bx + boxW / 2

        if (loginSuccess) {
            val msg = "Login Succeeded!"
            val mw = mc.font.width(msg)
            drawText(
                context, msg,
                (centerX - mw / 2).toFloat(), (boxTop + 55).toFloat(),
                Color4b(100, 255, 100),
            )
            val sub = "Account has been added. You can go back."
            val sw = mc.font.width(sub)
            drawText(
                context, sub,
                (centerX - sw / 2).toFloat(), (boxTop + 72).toFloat(),
                theme.textDimmed,
            )
        } else if (loginError != null) {
            val msg = "Login Failed"
            val mw = mc.font.width(msg)
            drawText(
                context, msg,
                (centerX - mw / 2).toFloat(), (boxTop + 55).toFloat(),
                Color4b(255, 80, 80),
            )
            val errMsg = loginError!!.take(50)
            val ew = mc.font.width(errMsg)
            drawText(
                context, errMsg,
                (centerX - ew / 2).toFloat(), (boxTop + 72).toFloat(),
                theme.textDimmed,
            )
        } else if (loginUrl != null) {
            val url = loginUrl!!
            val displayUrl = if (url.length > 56) url.take(56) + "..." else url
            val label = "Open this URL in your browser:"
            val lw = mc.font.width(label)
            drawText(
                context, label,
                (centerX - lw / 2).toFloat(), (boxTop + 50).toFloat(),
                theme.textPrimary,
            )

            val urlY = boxTop + 66
            with(context) {
                drawRoundedRect(
                    (bx + 10).toFloat(), urlY.toFloat(),
                    (bx + boxW - 10).toFloat(), (urlY + 20).toFloat(), 4f,
                    fillColor = theme.bgInput,
                )
            }
            val uw = mc.font.width(displayUrl)
            drawText(
                context, displayUrl,
                (centerX - uw / 2).toFloat(), (urlY + 6).toFloat(),
                theme.accent,
            )
        } else {
            val waiting = "Waiting for login URL"
            val ww = mc.font.width(waiting)
            val dots = ".".repeat(((System.currentTimeMillis() / 400) % 4).toInt())
            drawText(
                context, waiting + dots,
                (centerX - ww / 2).toFloat(), (boxTop + 60).toFloat(),
                theme.textDimmed,
            )
        }
    }

    private fun drawText(
        context: GuiGraphicsExtractor,
        text: String,
        x: Float,
        y: Float,
        color: Color4b,
    ) {
        context.text(mc.font, text, x.toInt(), y.toInt(), color.argb, false)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) {
            mc.setScreen(parent)
            return true
        }
        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean = false
}
