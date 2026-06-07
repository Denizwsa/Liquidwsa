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
package net.ccbluex.liquidbounce.integration.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Stub of the upstream task system used for progress reporting on the
 * splash screen. In the minimal WebGUI core the deep-learning engine is
 * small enough that we can drop the progress reporting, so the manager
 * becomes a thin wrapper around a [CoroutineScope] with no UI hooks.
 */
class TaskManager(private val scope: CoroutineScope) {

    val isCompleted: Boolean get() = true

    /** Register a sub-task. No-op in the minimal core. */
    fun launch(name: String, block: suspend (Task) -> Unit) {
        scope.launchSafe(name, block)
    }

    /** Run the wrapped scope to completion. Used by tests. */
    fun join() = runBlocking { }
}

private fun CoroutineScope.launchSafe(name: String, block: suspend (Task) -> Unit) {
    launch {
        runCatching { block(Task(name)) }
    }
}

/** Minimal placeholder for the upstream `Task` data class. */
class Task(val name: String) {
    val subTasks: MutableList<Task> = mutableListOf()
}
