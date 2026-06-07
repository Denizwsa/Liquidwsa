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
package net.ccbluex.liquidbounce.integration.screen

import java.util.concurrent.atomic.AtomicLong

/**
 * Round-trip acknowledgement used to detect desyncs between the Svelte
 * theme and the Minecraft client. Whenever we open or close a virtual
 * screen we issue a new ticket; once the Svelte side reports back via the
 * interop server we mark the ticket as confirmed. If the confirmation
 * hasn't arrived after a deadline, we re-issue the screen open.
 */
class ScreenAcknowledgement {

    private data class Ticket(val token: Long, val timestamp: Long)
    private val pending = java.util.concurrent.atomic.AtomicReference<Ticket?>(null)
    private val nextToken = AtomicLong(1L)

    fun issue(): Long {
        val token = nextToken.getAndIncrement()
        pending.set(Ticket(token, System.currentTimeMillis()))
        return token
    }

    fun confirm(token: Long): Boolean {
        val current = pending.get() ?: return false
        return if (current.token == token) {
            pending.compareAndSet(current, null)
            true
        } else false
    }

    fun isPending(): Boolean = pending.get() != null

    fun reset() { pending.set(null) }
}
