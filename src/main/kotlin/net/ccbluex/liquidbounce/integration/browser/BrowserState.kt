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

/**
 * Lifecycle state of a [Browser] instance. Mirrors the upstream enum so the
 * Svelte side can render a fallback screen if a page fails to load.
 */
sealed class BrowserState {
    object Loading : BrowserState()
    data class Success(val url: String) : BrowserState()
    data class Failure(
        val errorCode: Int,
        val errorText: String,
        val failedUrl: String,
    ) : BrowserState()
}
