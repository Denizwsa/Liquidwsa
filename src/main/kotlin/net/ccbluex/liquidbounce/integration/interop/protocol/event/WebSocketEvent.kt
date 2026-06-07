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
package net.ccbluex.liquidbounce.integration.interop.protocol.event

/**
 * Marker interface for events the integration browser wants to receive
 * via the interop WebSocket. In the minimal WebGUI core we don't
 * actually have a WebSocket server, so this is a no-op tag that keeps
 * the upstream event classes compile-clean.
 */
interface WebSocketEvent
