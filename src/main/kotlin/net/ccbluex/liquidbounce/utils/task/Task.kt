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

/**
 * Minimal progress-tracking task used by DJL deep learning. Replaces the
 * richer `integration.task.Task` stub that used to live in the WebGUI core
 * with a single-state marker so call sites compile without WebSocket fan-out.
 */
class Task(val name: String) {
    val subTasks: MutableList<Task> = mutableListOf()

    var isCompleted: Boolean = false
        private set

    fun complete() {
        isCompleted = true
    }
}
