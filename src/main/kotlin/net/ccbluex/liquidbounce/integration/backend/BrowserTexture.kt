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
package net.ccbluex.liquidbounce.integration.backend

import com.mojang.blaze3d.pipeline.RenderTarget

/**
 * Thin abstraction over the GL texture that a [Browser] paints into.
 * Concrete implementations (e.g. `CefBrowserTexture` for MCEF) take care of
 * uploading CEF's off-screen framebuffer into a Minecraft-compatible
 * [RenderTarget] so it can be sampled by the screen render pass.
 *
 * The class deliberately exposes the bare minimum of Minecraft API
 * surface, so that backend code can stay testable without spinning up a
 * full GL context.
 */
abstract class BrowserTexture {

    /** Underlying render target. May be null until the browser has produced its first frame. */
    abstract val renderTarget: RenderTarget?

    /** Whether the texture is ready to be drawn. */
    abstract val isValid: Boolean

    /** Resize the texture to [width]x[height] and recreate the FBO if needed. */
    abstract fun resize(width: Int, height: Int)

    /** Free the GL resources. Idempotent. */
    abstract fun close()
}
