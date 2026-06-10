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

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.asForm
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.clickgui.ClickGuiTheme
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.CompletableFuture

class CookieLoginScreen(private val parent: Screen?) : Screen(Component.literal("Cookie Login")) {

    private val theme = ClickGuiTheme
    private val boxW = 500

    private var cookieText: String = ""
    private var cursorPos: Int = 0
    private var statusMsg: String? = null
    private var statusColor: Color4b = Color4b.WHITE
    private var logging = false
    private var loginSuccess = false
    private var loginError: String? = null

    private var loginButton: Button? = null
    private var backButton: Button? = null

    private fun boxX() = (this.width - boxW) / 2

    override fun init() {
        val bx = boxX()
        val btnY = this.height / 2 + 80
        val btnW = 120

        loginButton = Button.builder(Component.literal("Login with Cookie")) {
            startCookieLogin()
        }.bounds(bx + boxW / 2 - btnW - 5, btnY, btnW, 20).build().also { addRenderableWidget(it) }

        backButton = Button.builder(Component.literal("Back")) {
            mc.setScreen(parent)
        }.bounds(bx + boxW / 2 + 5, btnY, btnW, 20).build().also { addRenderableWidget(it) }
    }

    override fun removed() {
        loginButton = null
        backButton = null
    }

    private fun startCookieLogin() {
        if (logging) return
        if (cookieText.isBlank()) {
            statusMsg = "Paste your cookies first!"
            statusColor = Color4b(255, 180, 100)
            return
        }

        logging = true
        statusMsg = "Authenticating..."
        statusColor = Color4b(140, 200, 255)

        val cookies = parseNetscapeCookies(cookieText)
        val msaa = cookies[".login.live.com"]?.get("__Host-MSAAUTHP")
            ?: cookies["login.live.com"]?.get("__Host-MSAAUTHP")
            ?: cookies[".login.live.com"]?.get("MSAAUTHP")
            ?: cookies["login.live.com"]?.get("MSAAUTHP")

        if (msaa == null) {
            logging = false
            statusMsg = "No Microsoft auth cookie found (__Host-MSAAUTHP)"
            statusColor = Color4b(255, 100, 100)
            return
        }

        CompletableFuture.supplyAsync {
            try {
                authenticateWithCookie(msaa)
            } catch (e: Exception) {
                logging = false
                loginError = e.message ?: "Unknown error"
                null
            }
        }.thenAccept { result ->
            if (result != null) {
                loginSuccess = true
                statusMsg = "Login Succeeded!"
                statusColor = Color4b(100, 255, 100)
            } else if (loginError == null) {
                loginError = "Authentication failed"
            }
            logging = false
        }
    }

    private fun authenticateWithCookie(msaaToken: String) {
        val jsonType = "application/json".toMediaType()
        val client = HttpClient.client.newBuilder().build()

        try {
            // Step 1: XBL Auth
            val xblBody = """
                {
                    "Properties": {
                        "AuthMethod": "RPS",
                        "RpsTicket": "$msaaToken",
                        "SiteName": "user.auth.xboxlive.com"
                    },
                    "RelyingParty": "http://auth.xboxlive.com",
                    "TokenType": "JWT"
                }
            """.trimIndent()

            val xblRequest = Request.Builder()
                .url("https://user.auth.xboxlive.com/user/authenticate")
                .post(xblBody.toRequestBody(jsonType))
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val xblResponse = client.newCall(xblRequest).execute()
            val xblJson = JsonParser.parseReader(xblResponse.body.charStream()).asJsonObject
            xblResponse.close()

            val xblToken = xblJson.get("Token").asString
            val userHash = xblJson.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0).asJsonObject
                .get("uhs").asString

            // Step 2: XSTS Auth
            val xstsBody = """
                {
                    "Properties": {
                        "SandboxId": "RETAIL",
                        "UserTokens": ["$xblToken"]
                    },
                    "RelyingParty": "rp://api.minecraftservices.com/",
                    "TokenType": "JWT"
                }
            """.trimIndent()

            val xstsRequest = Request.Builder()
                .url("https://xsts.auth.xboxlive.com/xsts/authorize")
                .post(xstsBody.toRequestBody(jsonType))
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val xstsResponse = client.newCall(xstsRequest).execute()
            val xstsJson = JsonParser.parseReader(xstsResponse.body.charStream()).asJsonObject
            xstsResponse.close()

            val xstsToken = xstsJson.get("Token").asString

            // Step 3: MC Login with Xbox
            val mcBody = """
                {
                    "identityToken": "XBL3.0 x=$userHash;$xstsToken"
                }
            """.trimIndent()

            val mcRequest = Request.Builder()
                .url("https://api.minecraftservices.com/authentication/login_with_xbox")
                .post(mcBody.toRequestBody(jsonType))
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val mcResponse = client.newCall(mcRequest).execute()
            val mcJson = JsonParser.parseReader(mcResponse.body.charStream()).asJsonObject
            mcResponse.close()

            val accessToken = mcJson.get("access_token").asString

            // Step 4: Fetch profile
            val profileRequest = Request.Builder()
                .url("https://api.minecraftservices.com/minecraft/profile")
                .get()
                .header("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val profileResponse = client.newCall(profileRequest).execute()
            val profileJson = JsonParser.parseReader(profileResponse.body.charStream()).asJsonObject
            profileResponse.close()

            val username = profileJson.get("name").asString
            val uuid = profileJson.get("id").asString

            // Step 5: Add account and login
            val account = net.ccbluex.liquidbounce.authlib.account.SessionAccount(accessToken).apply {
                refresh()
            }

            val existingAccount = AccountManager.accounts.find {
                it.profile?.username.equals(username, true)
            }

            if (existingAccount != null) {
                AccountManager.accounts[AccountManager.accounts.indexOf(existingAccount)] = account
            } else {
                AccountManager.accounts += account
            }

            ConfigSystem.store(AccountManager)
            AccountManager.loginDirectAccount(account)

            loginSuccess = true
        } catch (e: Exception) {
            logging = false
            loginError = e.message ?: "Authentication failed"
            throw e
        }
    }

    private fun parseNetscapeCookies(text: String): Map<String, MutableMap<String, String>> {
        val cookies = mutableMapOf<String, MutableMap<String, String>>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split("\t")
            if (parts.size < 7) continue
            val domain = parts[0]
            val name = parts[5]
            val value = parts[6]
            cookies.getOrPut(domain) { mutableMapOf() }[name] = value
        }
        return cookies
    }

    override fun extractRenderState(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        context.fill(0, 0, this.width, this.height, 0x60080810.toInt())

        val bx = boxX()
        val boxH = 220
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

        drawText(context, "Cookie Login", (bx + 14).toFloat(), (boxTop + 9).toFloat(), theme.textPrimary)

        super.extractRenderState(context, mouseX, mouseY, delta)

        val centerX = bx + boxW / 2

        if (loginSuccess) {
            val msg = "Login Succeeded!"
            val mw = mc.font.width(msg)
            drawText(
                context, msg,
                (centerX - mw / 2).toFloat(), (boxTop + 50).toFloat(),
                Color4b(100, 255, 100),
            )
            val sub = "Account has been added and logged in."
            val sw = mc.font.width(sub)
            drawText(
                context, sub,
                (centerX - sw / 2).toFloat(), (boxTop + 68).toFloat(),
                theme.textDimmed,
            )
        } else if (loginError != null) {
            val msg = "Login Failed"
            val mw = mc.font.width(msg)
            drawText(
                context, msg,
                (centerX - mw / 2).toFloat(), (boxTop + 45).toFloat(),
                Color4b(255, 80, 80),
            )
            val errMsg = loginError!!.take(60)
            val ew = mc.font.width(errMsg)
            drawText(
                context, errMsg,
                (centerX - ew / 2).toFloat(), (boxTop + 62).toFloat(),
                theme.textDimmed,
            )
        } else {
            val hint = "Paste Netscape cookies below:"
            val hw = mc.font.width(hint)
            drawText(
                context, hint,
                (centerX - hw / 2).toFloat(), (boxTop + 40).toFloat(),
                theme.textPrimary,
            )

            val inputX = bx + 10
            val inputY = boxTop + 55
            val inputW = boxW - 20
            val inputH = 80
            with(context) {
                drawRoundedRect(
                    inputX.toFloat(), inputY.toFloat(),
                    (inputX + inputW).toFloat(), (inputY + inputH).toFloat(), 4f,
                    fillColor = theme.bgInput,
                )
            }

            if (cookieText.isNotEmpty()) {
                val lines = cookieText.lines()
                var y = inputY + 4
                for (line in lines) {
                    if (y > inputY + inputH - 10) break
                    val display = if (line.length > 70) line.take(70) + "..." else line
                    drawText(context, display, (inputX + 4).toFloat(), y.toFloat(), theme.textDimmed)
                    y += 10
                }
            } else {
                val placeholder = "Ctrl+V to paste cookies..."
                val pw = mc.font.width(placeholder)
                drawText(
                    context, placeholder,
                    (centerX - pw / 2).toFloat(), (inputY + 34).toFloat(),
                    theme.textDimmed,
                )
            }
        }

        if (logging) {
            val waiting = "Authenticating"
            val dots = ".".repeat(((System.currentTimeMillis() / 400) % 4).toInt())
            val ww = mc.font.width(waiting + dots)
            drawText(
                context, waiting + dots,
                (centerX - ww / 2).toFloat(), (boxTop + 100).toFloat(),
                theme.accent,
            )
        }
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) {
            mc.setScreen(parent)
            return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        if (loginSuccess || logging) return false
        val c = event.codepoint().toChar()
        if (c.code in 32..126) {
            cookieText = cookieText.substring(0, cursorPos) + c + cookieText.substring(cursorPos)
            cursorPos++
            return true
        }
        return false
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
