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
import net.minecraft.network.chat.Component
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.CompletableFuture

class CookieLoginScreen(private val parent: Screen?) : Screen(Component.literal("Cookie Login")) {

    private val theme = ClickGuiTheme
    private val boxW = 440

    private var cookieInput = ""
    private var logging = false
    private var loginSuccess = false
    private var loginError: String? = null

    private var pasteButton: Button? = null
    private var loginButton: Button? = null
    private var backButton: Button? = null

    private fun boxX() = (this.width - boxW) / 2

    override fun init() {
        val bx = boxX()
        val btnY = this.height / 2 + 70
        val btnW = 100

        pasteButton = Button.builder(Component.literal("Paste from Clipboard")) {
            try {
                cookieInput = mc.keyboardHandler.clipboard
                if (cookieInput.isNotBlank()) {
                    startCookieLogin(cookieInput)
                }
            } catch (_: Exception) {}
        }.bounds(bx + boxW / 2 - btnW - 5, btnY, btnW + 30, 20).build().also { addRenderableWidget(it) }

        loginButton = Button.builder(Component.literal("Login")) {
            if (cookieInput.isNotBlank()) {
                startCookieLogin(cookieInput)
            }
        }.bounds(bx + boxW / 2 - btnW - 5, btnY + 24, btnW, 20).build().also { addRenderableWidget(it) }

        backButton = Button.builder(Component.literal("Back")) {
            mc.setScreen(parent)
        }.bounds(bx + boxW / 2 + 5, btnY + 24, btnW, 20).build().also { addRenderableWidget(it) }
    }

    override fun removed() {
        pasteButton = null
        loginButton = null
        backButton = null
    }

    private fun startCookieLogin(content: String) {
        if (logging || loginSuccess) return

        val cookies = parseNetscapeCookies(content)
        val msaa = cookies[".login.live.com"]?.get("__Host-MSAAUTHP")
            ?: cookies["login.live.com"]?.get("__Host-MSAAUTHP")
            ?: cookies[".login.live.com"]?.get("MSAAUTHP")
            ?: cookies["login.live.com"]?.get("MSAAUTHP")

        if (msaa == null) {
            loginError = "No __Host-MSAAUTHP cookie found"
            return
        }

        logging = true
        loginError = null

        CompletableFuture.supplyAsync {
            try {
                authenticateWithCookie(msaa)
            } catch (e: Exception) {
                loginError = e.message ?: "Unknown error"
                logging = false
                null
            }
        }.thenAccept { result ->
            if (result != null) {
                loginSuccess = true
            }
            logging = false
        }
    }

    private fun authenticateWithCookie(msaaToken: String): Boolean {
        val jsonType = "application/json; charset=utf-8".toMediaType()
        val client = HttpClient.client

        val xblBody = """
            {
                "Properties": {
                    "AuthMethod": "RPS",
                    "RpsTicket": "d=$msaaToken",
                    "SiteName": "user.auth.xboxlive.com"
                },
                "RelyingParty": "http://auth.xboxlive.com",
                "TokenType": "JWT"
            }
        """.trimIndent()

        val xblRequest = Request.Builder()
            .url("https://user.auth.xboxlive.com/user/authenticate")
            .post(xblBody.toRequestBody(jsonType))
            .header("User-Agent", "MSAL/1.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json")
            .build()

        val xblResponse = client.newCall(xblRequest).execute()
        val xblBodyStr = xblResponse.body?.string() ?: throw Exception("Empty XBL response")
        xblResponse.close()

        if (!xblResponse.isSuccessful) {
            throw Exception("XBL ${xblResponse.code}: $xblBodyStr")
        }

        val xblJson = JsonParser.parseString(xblBodyStr).asJsonObject
        val xblToken = xblJson.get("Token")?.asString
            ?: throw Exception("No XBL Token in response")
        val userHash = xblJson.getAsJsonObject("DisplayClaims")
            ?.getAsJsonArray("xui")
            ?.get(0)?.asJsonObject
            ?.get("uhs")?.asString
            ?: throw Exception("No userHash in XBL response")

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
            .header("User-Agent", "MSAL/1.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json")
            .build()

        val xstsResponse = client.newCall(xstsRequest).execute()
        val xstsBodyStr = xstsResponse.body?.string() ?: throw Exception("Empty XSTS response")
        xstsResponse.close()

        if (!xstsResponse.isSuccessful) {
            throw Exception("XSTS ${xstsResponse.code}: $xstsBodyStr")
        }

        val xstsJson = JsonParser.parseString(xstsBodyStr).asJsonObject
        val xstsToken = xstsJson.get("Token")?.asString
            ?: throw Exception("No XSTS Token in response")

        val mcBody = """
            {
                "identityToken": "XBL3.0 x=$userHash;$xstsToken"
            }
        """.trimIndent()

        val mcRequest = Request.Builder()
            .url("https://api.minecraftservices.com/authentication/login_with_xbox")
            .post(mcBody.toRequestBody(jsonType))
            .header("User-Agent", "MSAL/1.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json")
            .build()

        val mcResponse = client.newCall(mcRequest).execute()
        val mcBodyStr = mcResponse.body?.string() ?: throw Exception("Empty MC response")
        mcResponse.close()

        if (!mcResponse.isSuccessful) {
            throw Exception("MC Login ${mcResponse.code}: $mcBodyStr")
        }

        val mcJson = JsonParser.parseString(mcBodyStr).asJsonObject
        val accessToken = mcJson.get("access_token")?.asString
            ?: throw Exception("No access_token in MC response")

        val profileRequest = Request.Builder()
            .url("https://api.minecraftservices.com/minecraft/profile")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", "MSAL/1.0 (Windows NT 10.0; Win64; x64)")
            .build()

        val profileResponse = client.newCall(profileRequest).execute()
        val profileBodyStr = profileResponse.body?.string() ?: throw Exception("Empty profile response")
        profileResponse.close()

        if (!profileResponse.isSuccessful) {
            throw Exception("Profile ${profileResponse.code}: $profileBodyStr")
        }

        val account = net.ccbluex.liquidbounce.authlib.account.SessionAccount(accessToken).apply {
            refresh()
        }

        val existingAccount = AccountManager.accounts.find {
            it.profile?.username.equals(account.profile?.username, true)
        }

        if (existingAccount != null) {
            AccountManager.accounts[AccountManager.accounts.indexOf(existingAccount)] = account
        } else {
            AccountManager.accounts += account
        }

        ConfigSystem.store(AccountManager)
        AccountManager.loginDirectAccount(account)

        return true
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
        val boxH = 200
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
            drawText(context, msg, (centerX - mw / 2).toFloat(), (boxTop + 50).toFloat(), Color4b(100, 255, 100))
            val sub = "Account added and logged in."
            val sw = mc.font.width(sub)
            drawText(context, sub, (centerX - sw / 2).toFloat(), (boxTop + 68).toFloat(), theme.textDimmed)
        } else if (loginError != null) {
            val msg = "Login Failed"
            val mw = mc.font.width(msg)
            drawText(context, msg, (centerX - mw / 2).toFloat(), (boxTop + 50).toFloat(), Color4b(255, 80, 80))
            val errMsg = loginError!!.take(60)
            val ew = mc.font.width(errMsg)
            drawText(context, errMsg, (centerX - ew / 2).toFloat(), (boxTop + 68).toFloat(), theme.textDimmed)
        } else if (logging) {
            val dots = ".".repeat(((System.currentTimeMillis() / 400) % 4).toInt())
            val waitMsg = "Authenticating$dots"
            val ww = mc.font.width(waitMsg)
            drawText(context, waitMsg, (centerX - ww / 2).toFloat(), (boxTop + 55).toFloat(), theme.accent)
        } else {
            val hint = "Click 'Paste from Clipboard' to paste"
            val hw = mc.font.width(hint)
            drawText(context, hint, (centerX - hw / 2).toFloat(), (boxTop + 45).toFloat(), theme.textDimmed)

            val hint2 = "your Netscape cookie export, then Login."
            val hw2 = mc.font.width(hint2)
            drawText(context, hint2, (centerX - hw2 / 2).toFloat(), (boxTop + 60).toFloat(), theme.textDimmed)

            if (cookieInput.isNotEmpty()) {
                val lines = cookieInput.lines()
                val preview = "Loaded ${lines.size} lines"
                val pw = mc.font.width(preview)
                drawText(context, preview, (centerX - pw / 2).toFloat(), (boxTop + 80).toFloat(), Color4b(140, 255, 140))
            }
        }
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
