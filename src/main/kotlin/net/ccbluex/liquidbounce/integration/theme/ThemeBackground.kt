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
package net.ccbluex.liquidbounce.integration.theme

/**
 * Background-image slot the upstream theme system supported. We keep a
 * stub so legacy import sites still compile, but in the minimal WebGUI
 * core the value is unused.
 */
class ThemeBackground {
    var imagePath: String? = null
    var opacity: Float = 0.6f
}
