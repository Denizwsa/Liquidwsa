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
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Desktop
import java.net.URI

class MultiplayerWithAccountsScreen(
    previous: Screen?,
) : JoinMultiplayerScreen(previous ?: TitleScreen()) {

    private val panelWidth = 280
    private val panelMargin = 8
    private val panelHeaderH = 30
    private val rowHeight = 32

    private var accountsVisible = false

    private var scrollOffset = 0f
    private var maxScroll = 0f

    private var crackedField: EditBox? = null
    private var alteningField: EditBox? = null
    private var addCrackedButton: Button? = null
    private var addAlteningButton: Button? = null
    private var addMicrosoftButton: Button? = null
    private var accountsToggleButton: Button? = null

    private var statusMessage: String? = null
    private var statusMessageColor: Color4b = Color4b.WHITE
    private var statusMessageTime: Long = 0L

    private val listAreaTopY: Int
        get() = panelHeaderH + 10

    private fun maxVisibleAccounts(): Int =
        ((this.height - listAreaTopY - 150) / rowHeight).coerceAtLeast(2)

    override fun init() {
        super.init()

        accountsToggleButton = Button.builder(
            Component.literal("Account Manager"),
        ) {
            accountsVisible = !accountsVisible
            if (accountsVisible) {
                addAccountWidgets()
            } else {
                removeAccountWidgets()
            }
            updateToggleButtonLabel()
        }.bounds(
            this.width - 154, 6, 148, 18,
        ).build().also { addRenderableWidget(it) }

        updateToggleButtonLabel()
    }

    private fun addAccountWidgets() {
        if (crackedField != null) return

        val rightX = this.width - panelWidth - panelMargin
        val rightW = panelWidth - 8
        val inputY = this.height - 120

        val fieldW = rightW - 84
        crackedField = EditBox(
            mc.font, rightX + 4, inputY, fieldW, 16,
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
        }.bounds(rightX + rightW - 76, inputY, 72, 16).build().also { addRenderableWidget(it) }

        val alteningY = inputY + 22
        alteningField = EditBox(
            mc.font, rightX + 4, alteningY, fieldW, 16,
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
        }.bounds(rightX + rightW - 76, alteningY, 72, 16).build().also { addRenderableWidget(it) }

        val microsoftY = alteningY + 26
        addMicrosoftButton = Button.builder(
            Component.literal("Login with Microsoft"),
        ) { startMicrosoftLogin() }.bounds(
            rightX + 4, microsoftY, rightW, 20,
        ).build().also { addRenderableWidget(it) }
    }

    private fun removeAccountWidgets() {
        crackedField?.let { removeWidget(it) }
        alteningField?.let { removeWidget(it) }
        addCrackedButton?.let { removeWidget(it) }
        addAlteningButton?.let { removeWidget(it) }
        addMicrosoftButton?.let { removeWidget(it) }
        crackedField = null
        alteningField = null
        addCrackedButton = null
        addAlteningButton = null
        addMicrosoftButton = null
    }

    private fun updateToggleButtonLabel() {
        accountsToggleButton?.setMessage(
            Component.literal(
                if (accountsVisible) "Hide Accounts" else "Account Manager",
            ),
        )
    }

    private fun setStatus(message: String, color: Color4b = Color4b.WHITE) {
        statusMessage = message
        statusMessageColor = color
        statusMessageTime = System.currentTimeMillis()
    }

    private fun startMicrosoftLogin() {
        AccountManager.newMicrosoftAccount { url ->
            try {
                val clipboard = java.awt.datatransfer.StringSelection(url)
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(clipboard, null)
                setStatus("Login link copied to clipboard!", Color4b(140, 255, 140))

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url))
                }
            } catch (e: Exception) {
                setStatus("URL copied: $url", Color4b(255, 200, 100))
            }
        }
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        if (accountsVisible) {
            drawAccountsPanel(context, mouseX, mouseY)
        }
    }

    private fun drawAccountsPanel(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        val rightX = this.width - panelWidth - panelMargin
        val rightY = panelMargin
        val rightW = panelWidth
        val rightH = this.height - panelMargin * 2
        val theme = ClickGuiTheme

        with(context) {
            drawRoundedRect(
                rightX.toFloat(), rightY.toFloat(),
                (rightX + rightW).toFloat(), (rightY + rightH).toFloat(), theme.cardRadius,
                fillColor = theme.bgPrimary,
            )
            drawRoundedRect(
                rightX.toFloat(), rightY.toFloat(),
                (rightX + rightW).toFloat(), (rightY + panelHeaderH).toFloat(), theme.cardRadius,
                fillColor = theme.bgSecondary,
            )
        }

        drawText(context, "Accounts", (rightX + 10).toFloat(), (rightY + 9).toFloat(), theme.textPrimary)
        val currentUser = mc.user?.name
        if (currentUser != null) {
            val currentLabel = "Active: $currentUser"
            val tw = mc.font.width(currentLabel)
            drawText(
                context, currentLabel,
                (rightX + rightW - 10 - tw).toFloat(), (rightY + 9).toFloat(),
                Color4b(140, 255, 140),
            )
        }

        val accounts = AccountManager.accounts
        val visibleCount = maxVisibleAccounts()
        val listTop = rightY + listAreaTopY
        val listBottom = listTop + visibleCount * rowHeight

        val totalRows = accounts.size
        maxScroll = ((totalRows - visibleCount).coerceAtLeast(0) * rowHeight).toFloat()
        if (scrollOffset > maxScroll) scrollOffset = maxScroll
        if (scrollOffset < 0f) scrollOffset = 0f

        val listLeft = rightX + 4
        val listRight = rightX + rightW - 4
        with(context) {
            drawRoundedRect(
                listLeft.toFloat(), listTop.toFloat(),
                listRight.toFloat(), listBottom.toFloat(), 4f,
                fillColor = theme.bgContent,
            )
        }

        if (accounts.isEmpty()) {
            drawText(
                context, "No accounts",
                (listLeft + (listRight - listLeft - mc.font.width("No accounts")) / 2).toFloat(),
                (listTop + 8).toFloat(),
                theme.textDimmed,
            )
        } else {
            for (i in accounts.indices) {
                val account = accounts[i]
                val rowY = listTop + i * rowHeight - scrollOffset.toInt()
                if (rowY + rowHeight <= listTop || rowY >= listBottom) continue
                val bg = if (currentUser != null && account.profile?.username == currentUser) {
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
                drawText(
                    context, username,
                    (listLeft + 8).toFloat(), (rowY + 5).toFloat(),
                    theme.textPrimary,
                )
                val service = AccountService.getService(account).tag
                drawText(
                    context, service,
                    (listLeft + 8).toFloat(), (rowY + 17).toFloat(),
                    theme.textDimmed,
                )

                val loginLabel = "Login"
                val loginW = mc.font.width(loginLabel) + 16
                val loginH = 18
                val loginX = listRight - loginW - 6
                val loginY = rowY + (rowHeight - loginH) / 2
                val loginBg = if (isOverLogin(i, mouseX, mouseY)) {
                    theme.accent
                } else {
                    theme.border
                }
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
            val scrollBarH = ((visibleCount.toFloat() / totalRows) * (listBottom - listTop)).coerceAtLeast(20f)
            val scrollBarY = listTop + (scrollOffset / maxScroll) * ((listBottom - listTop) - scrollBarH)
            with(context) {
                drawRoundedRect(
                    (listRight - 3).toFloat(), scrollBarY,
                    listRight.toFloat(), scrollBarY + scrollBarH, 1.5f,
                    fillColor = theme.scrollbarThumb,
                )
            }
        }

        if (System.currentTimeMillis() - statusMessageTime < 5000 && statusMessage != null) {
            val statusY = this.height - 24
            drawText(
                context, statusMessage!!,
                (rightX + 10).toFloat(), statusY.toFloat(),
                statusMessageColor,
            )
        }
    }

    private fun hoveredAccountIndex(mouseX: Int, mouseY: Int): Int {
        val rightX = this.width - panelWidth - panelMargin
        if (mouseX < rightX + 4 || mouseX > rightX + panelWidth - 4) return -1
        val listTop = panelMargin + listAreaTopY
        val listBottom = listTop + maxVisibleAccounts() * rowHeight
        if (mouseY < listTop || mouseY > listBottom) return -1
        return ((mouseY - listTop + scrollOffset.toInt()) / rowHeight)
    }

    private fun isOverLogin(index: Int, mouseX: Int, mouseY: Int): Boolean {
        if (index != hoveredAccountIndex(mouseX, mouseY)) return false
        val rightX = this.width - panelWidth - panelMargin
        val listTop = panelMargin + listAreaTopY
        val listRight = rightX + panelWidth - 4
        val rowY = listTop + index * rowHeight - scrollOffset.toInt()
        val loginW = mc.font.width("Login") + 16
        val loginX = listRight - loginW - 6
        val loginY = rowY + (rowHeight - 18) / 2
        return mouseX in loginX..(loginX + loginW) && mouseY in loginY..(loginY + 18)
    }

    private fun isOverAccountRow(index: Int, mouseX: Int, mouseY: Int): Boolean {
        if (index != hoveredAccountIndex(mouseX, mouseY)) return false
        return !isOverLogin(index, mouseX, mouseY)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (accountsVisible) {
            val mx = event.x.toInt()
            val my = event.y.toInt()
            if (mx >= this.width - panelWidth - panelMargin) {
                val idx = hoveredAccountIndex(mx, my)
                if (idx in AccountManager.accounts.indices) {
                    if (event.button() == 0 && isOverLogin(idx, mx, my)) {
                        AccountManager.loginAccount(idx)
                        setStatus("Logging in...", Color4b(140, 200, 255))
                        return true
                    } else if (event.button() == 1 && isOverAccountRow(idx, mx, my)) {
                        try {
                            val removed = AccountManager.removeAccount(idx)
                            setStatus("Removed ${removed.profile?.username ?: "account"}", Color4b(255, 180, 100))
                        } catch (e: Exception) {
                            setStatus("Failed: ${e.message}", Color4b(255, 100, 100))
                        }
                        return true
                    }
                }
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
        if (accountsVisible && mouseX.toInt() >= this.width - panelWidth - panelMargin) {
            scrollOffset = (scrollOffset - deltaY.toFloat() * 12f).coerceIn(0f, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)
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
}
