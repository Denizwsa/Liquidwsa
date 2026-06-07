/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package net.ccbluex.liquidbounce.integration.browser

import com.mojang.blaze3d.vertex.PoseStack

/**
 * Draws a [Browser] into a Minecraft pose-stack. The actual
 * `RenderTarget` blit happens inside the MCEF mod's `CefTexture.render`
 * method; here we only translate the matrix so the texture is drawn in
 * the right spot. The implementation is therefore empty until MCEF is
 * actually wired up.
 *
 * We keep the [draw] entry point so call-sites can stay agnostic about
 * whether a real backend is present.
 */
object BrowserRenderer {

    /** Apply the browser's viewport transform to [pose]. */
    fun applyTransform(pose: PoseStack, browser: Browser) {
        pose.pushPose()
        pose.translate(
            browser.viewport.x.toFloat(),
            browser.viewport.y.toFloat(),
            0f,
        )
    }

    /** Inverse of [applyTransform]. */
    fun popTransform(pose: PoseStack) {
        pose.popPose()
    }

    /**
     * Composite the browser onto the screen. Returns silently if MCEF
     * isn't installed yet — the screen will still draw a backdrop and
     * the [Browser] field stays null.
     */
    fun draw(@Suppress("UNUSED_PARAMETER") browser: Browser) {
        // Real blit lives behind MCEF's `CefTexture`. We only expose the
        // hook here so call-sites compile cleanly.
    }
}
