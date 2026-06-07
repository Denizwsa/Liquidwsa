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

/**
 * Describes whether a [BrowserBackend] supports GPU acceleration and which
 * toggle the user has selected. Used by the F12 key handler in [ScreenManager]
 * to flip hardware acceleration on/off at runtime without restarting the
 * engine (when the backend supports it).
 */
class BrowserAccelerationFlags(val isSupported: Boolean)
