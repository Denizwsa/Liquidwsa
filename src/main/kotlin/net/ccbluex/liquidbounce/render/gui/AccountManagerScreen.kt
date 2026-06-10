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
import net.ccbluex.liquidbounce.features.account.AccountService
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiTheme
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class AccountManagerScreen(private val parent: Screen?) : Screen(Component.literal("Account Manager")) {

    private val panelWidth = 420
    private val rowHeight = 36
    private val theme = ClickGuiTheme

    private var scrollOffset = 0f
    private var maxScroll = 0f

    private var crackedField: EditBox? = null
    private var alteningField: EditBox? = null
    private var addCrackedButton: Button? = null
    private var addAlteningButton: Button? = null
    private var addMicrosoftButton: Button? = null

    private var statusMessage: String? = null
    private var statusMessageColor: Color4b = Color4b.WHITE
    private var statusMessageTime: Long = 0L

    private fun panelX() = (this.width - panelWidth) / 2
    private fun panelTopY() = 40
    private fun accountListTop() = panelTopY() + 36
    private fun accountListBottom(): Int = this.height - 130
    private fun maxVisibleAccounts(): Int =
        ((accountListBottom() - accountListTop()) / rowHeight).coerceAtLeast(1)
    private fun panelBottom(): Int = this.height - 50

    override fun init() {
        val px = panelX()
        val pw = panelWidth - 16
        val inputY = accountListBottom() + 10
        val fieldW = pw - 84

        crackedField = EditBox(
            mc.font, px + 4, inputY, fieldW, 16,
            Component.literal("Username"),
        ).also { f ->
            f.setMaxLength(16)
            f.setHint(Component.literal("Cracked username"))
            addRenderableWidget(f)
        }
        addCrackedButton = Button.builder(Component.literal("Add")) {
            val name = crackedField?.value?.trim().orEmpty()
            if (name.isNotEmpty()) {
                AccountManager.newCrackedAccount(name, online = false)
                crackedField?.value = ""
                setStatus("Cracked account '$name' added", Color4b(140, 255, 140))
            }
        }.bounds(px + pw - 76, inputY, 72, 16).build().also { addRenderableWidget(it) }

        val alteningY = inputY + 22
        alteningField = EditBox(
            mc.font, px + 4, alteningY, fieldW, 16,
            Component.literal("Token"),
        ).also { f ->
            f.setMaxLength(64)
            f.setHint(Component.literal("TheAltening token"))
            addRenderableWidget(f)
        }
        addAlteningButton = Button.builder(Component.literal("Add")) {
            val token = alteningField?.value?.trim().orEmpty()
            if (token.isNotEmpty()) {
                AccountManager.newAlteningAccount(token)
                alteningField?.value = ""
                setStatus("Altening account added", Color4b(140, 255, 140))
            }
        }.bounds(px + pw - 76, alteningY, 72, 16).build().also { addRenderableWidget(it) }

        val microsoftY = alteningY + 26
        addMicrosoftButton = Button.builder(
            Component.literal("Login with Microsoft"),
        ) {
            mc.setScreen(MicrosoftLoginScreen(this@AccountManagerScreen))
        }.bounds(
            px + 4, microsoftY, pw, 20,
        ).build().also { addRenderableWidget(it) }

        val cookieY = microsoftY + 24
        addRenderableWidget(
            Button.builder(
                Component.literal("Login with Cookie"),
            ) {
                mc.setScreen(CookieLoginScreen(this@AccountManagerScreen))
            }.bounds(
                px + 4, cookieY, pw, 20,
            ).build()
        )
    }

    override fun removed() {
        crackedField = null
        alteningField = null
        addCrackedButton = null
        addAlteningButton = null
        addMicrosoftButton = null
    }

    private fun setStatus(message: String, color: Color4b = Color4b.WHITE) {
        statusMessage = message
        statusMessageColor = color
        statusMessageTime = System.currentTimeMillis()
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        context.fill(0, 0, this.width, this.height, 0x60080810.toInt())

        val px = panelX()
        val pw = panelWidth
        val panelTop = panelTopY()
        val panelBot = panelBottom()

        with(context) {
            drawRoundedRect(
                px.toFloat(), panelTop.toFloat(),
                (px + pw).toFloat(), panelBot.toFloat(), theme.cardRadius,
                fillColor = theme.bgPrimary,
            )
            drawRoundedRect(
                px.toFloat(), panelTop.toFloat(),
                (px + pw).toFloat(), (panelTop + 30).toFloat(), theme.cardRadius,
                fillColor = theme.bgSecondary,
            )
        }

        drawText(context, "Account Manager", (px + 10).toFloat(), (panelTop + 9).toFloat(), theme.textPrimary)

        val currentUser = mc.user?.name
        if (currentUser != null) {
            val label = "Active: $currentUser"
            val tw = mc.font.width(label)
            drawText(
                context, label,
                (px + pw - 10 - tw).toFloat(), (panelTop + 9).toFloat(),
                Color4b(140, 255, 140),
            )
        }

        super.extractRenderState(context, mouseX, mouseY, delta)

        drawAccountList(context, mouseX, mouseY)

        if (System.currentTimeMillis() - statusMessageTime < 5000 && statusMessage != null) {
            drawText(context, statusMessage!!, (px + 10).toFloat(), (panelBot + 4).toFloat(), statusMessageColor)
        }
    }

    private fun drawAccountList(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        val px = panelX()
        val pw = panelWidth
        val listTop = accountListTop()
        val listBot = accountListBottom()

        val accounts = AccountManager.accounts
        val visibleCount = maxVisibleAccounts()

        val totalRows = accounts.size
        maxScroll = ((totalRows - visibleCount).coerceAtLeast(0) * rowHeight).toFloat()
        if (scrollOffset > maxScroll) scrollOffset = maxScroll
        if (scrollOffset < 0f) scrollOffset = 0f

        val listLeft = px + 4
        val listRight = px + pw - 4
        with(context) {
            drawRoundedRect(
                listLeft.toFloat(), listTop.toFloat(),
                listRight.toFloat(), listBot.toFloat(), 4f,
                fillColor = theme.bgContent,
            )
        }

        if (accounts.isEmpty()) {
            val noAcc = "No accounts"
            drawText(
                context, noAcc,
                (listLeft + (listRight - listLeft - mc.font.width(noAcc)) / 2).toFloat(),
                (listTop + 8).toFloat(),
                theme.textDimmed,
            )
        } else {
            val currentUsername = mc.user?.name
            for (i in accounts.indices) {
                val account = accounts[i]
                val rowY = listTop + i * rowHeight - scrollOffset.toInt()
                if (rowY + rowHeight <= listTop || rowY >= listBot) continue
                val bg = if (currentUsername != null && account.profile?.username == currentUsername) {
                    theme.accentDim
                } else if (i == hoveredAccountIndex(mouseX, mouseY)) {
                    theme.bgCardHover
                } else {
                    theme.bgCard
                }
                with(context) {
                    drawRoundedRect(
                        (listLeft + 2).toFloat(), (rowY + 1).toFloat(),
                        (listRight - 2).toFloat(), (rowY + rowHeight - 1).toFloat(), 3f,
                        fillColor = bg,
                    )
                }
                val username = account.profile?.username ?: "Unknown"
                drawText(context, username, (listLeft + 8).toFloat(), (rowY + 6).toFloat(), theme.textPrimary)
                val service = AccountService.getService(account).tag
                drawText(context, service, (listLeft + 8).toFloat(), (rowY + 18).toFloat(), theme.textDimmed)

                val loginLabel = "Login"
                val loginW = mc.font.width(loginLabel) + 16
                val loginH = 18
                val loginX = listRight - loginW - 6
                val loginY = rowY + (rowHeight - loginH) / 2
                val loginBg = if (isOverLogin(i, mouseX, mouseY)) theme.accent else theme.border
                with(context) {
                    drawRoundedRect(
                        loginX.toFloat(), loginY.toFloat(),
                        (loginX + loginW).toFloat(), (loginY + loginH).toFloat(), 3f,
                        fillColor = loginBg,
                    )
                }
                drawText(
                    context, loginLabel,
                    (loginX + (loginW - mc.font.width(loginLabel)) / 2).toFloat(), (loginY + 5).toFloat(),
                    theme.textPrimary,
                )
            }
        }

        if (totalRows > visibleCount) {
            val scrollBarH = ((visibleCount.toFloat() / totalRows) * (listBot - listTop)).coerceAtLeast(20f)
            val scrollBarY = listTop + (scrollOffset / maxScroll) * ((listBot - listTop) - scrollBarH)
            with(context) {
                drawRoundedRect(
                    (listRight - 3).toFloat(), scrollBarY,
                    listRight.toFloat(), scrollBarY + scrollBarH, 1.5f,
                    fillColor = theme.scrollbarThumb,
                )
            }
        }
    }

    private fun hoveredAccountIndex(mouseX: Int, mouseY: Int): Int {
        val px = panelX()
        if (mouseX < px + 4 || mouseX > px + panelWidth - 4) return -1
        val listTop = accountListTop()
        val listBot = accountListBottom()
        if (mouseY < listTop || mouseY > listBot) return -1
        return ((mouseY - listTop + scrollOffset.toInt()) / rowHeight)
    }

    private fun isOverLogin(index: Int, mouseX: Int, mouseY: Int): Boolean {
        if (index != hoveredAccountIndex(mouseX, mouseY)) return false
        val px = panelX()
        val listTop = accountListTop()
        val listRight = px + panelWidth - 4
        val rowY = listTop + index * rowHeight - scrollOffset.toInt()
        val loginW = mc.font.width("Login") + 16
        val loginX = listRight - loginW - 6
        val loginY = rowY + (rowHeight - 18) / 2
        return mouseX in loginX..(loginX + loginW) && mouseY in loginY..(loginY + 18)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x.toInt()
        val my = event.y.toInt()

        val idx = hoveredAccountIndex(mx, my)
        if (idx in AccountManager.accounts.indices) {
            if (event.button() == 0 && isOverLogin(idx, mx, my)) {
                AccountManager.loginAccount(idx)
                setStatus("Logging in...", Color4b(140, 200, 255))
                return true
            } else if (event.button() == 1) {
                try {
                    val removed = AccountManager.removeAccount(idx)
                    setStatus("Removed ${removed.profile?.username ?: "account"}", Color4b(255, 180, 100))
                } catch (e: Exception) {
                    setStatus("Failed: ${e.message}", Color4b(255, 100, 100))
                }
                return true
            }
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
    ): Boolean {
        val px = panelX()
        if (mouseX.toInt() in px..(px + panelWidth)) {
            scrollOffset = (scrollOffset - deltaY.toFloat() * 12f).coerceIn(0f, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) {
            mc.setScreen(parent)
            return true
        }
        return super.keyPressed(event)
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

    override fun isPauseScreen(): Boolean = false
}
