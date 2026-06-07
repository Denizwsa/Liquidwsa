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
package net.ccbluex.liquidbounce.utils.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Minimal coroutine-backed task manager. Replaces the WebGUI-era `TaskManager`
 * for callers that need to fire-and-forget named background work.
 *
 * Compatible with the legacy `launch(name) { task -> ... }` API.
 */
class TaskManager(private val scope: CoroutineScope) {

    val activeTasks: MutableList<Task> = mutableListOf()

    val isCompleted: Boolean
        get() = activeTasks.isEmpty()

    fun launch(name: String, block: suspend CoroutineScope.(Task) -> Unit): Task {
        val task = Task(name)
        activeTasks += task
        scope.launch {
            try {
                block(task)
            } finally {
                task.complete()
                activeTasks.remove(task)
            }
        }
        return task
    }
}
