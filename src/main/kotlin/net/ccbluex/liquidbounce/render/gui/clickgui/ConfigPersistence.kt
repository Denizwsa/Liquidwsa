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
package net.ccbluex.liquidbounce.render.gui.clickgui

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.Minecraft
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val logger = clientLogger("ClickGuiPersistence")

/**
 * Persists the Java-side ClickGUI panel layout (positions, expanded state,
 * scroll offset, z-index) to a JSON file inside [ConfigSystem.rootFolder].
 *
 * Replaces the Svelte WebUI's `localStorage` storage with an in-memory cache
 * plus a debounced file flush so layout is preserved across game restarts.
 */
object ClickGuiConfig {

    private val file: File = File(ConfigSystem.rootFolder, "clickgui-panels.json")

    private val cache: MutableMap<String, JsonElement> = ConcurrentHashMap()

    @Volatile
    private var dirty: Boolean = false

    @Volatile
    private var lastWriteMs: Long = 0L

    init {
        load()
    }

    fun getString(key: String, default: String? = null): String? =
        (cache[key] as? JsonPrimitive)?.asString ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        (cache[key] as? JsonPrimitive)?.asBoolean ?: default

    fun getInt(key: String, default: Int = 0): Int =
        (cache[key] as? JsonPrimitive)?.asInt ?: default

    fun getFloat(key: String, default: Float = 0f): Float =
        (cache[key] as? JsonPrimitive)?.asFloat ?: default

    fun put(key: String, value: String) = putElement(key, JsonPrimitive(value))
    fun put(key: String, value: Boolean) = putElement(key, JsonPrimitive(value))
    fun put(key: String, value: Int) = putElement(key, JsonPrimitive(value))
    fun put(key: String, value: Float) = putElement(key, JsonPrimitive(value))

    private fun putElement(key: String, element: JsonElement) {
        cache[key] = element
        dirty = true
        scheduleFlush()
    }

    /**
     * Reads the on-disk JSON file into [cache]. Called once at startup; any
     * I/O failure (missing file, malformed JSON) is logged and treated as an
     * empty cache so the GUI can still start.
     */
    private fun load() {
        cache.clear()
        if (!file.exists()) {
            return
        }
        runCatching {
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                val parsed = fileGson.fromJson(reader, JsonObject::class.java) ?: return
                for ((key, value) in parsed.entrySet()) {
                    if (value != null && !value.isJsonNull) {
                        cache[key] = value
                    }
                }
            }
        }.onFailure { ex ->
            logger.error("Failed to load ClickGUI config at ${file.absolutePath}", ex)
        }
    }

    private fun scheduleFlush() {
        if (Minecraft.getInstance()?.isSameThread == true) {
            flushIfDue(System.currentTimeMillis())
        }
    }

    private fun flushIfDue(nowMs: Long) {
        if (!dirty) return
        if (nowMs - lastWriteMs < 500L) return
        flush()
    }

    /**
     * Writes [cache] to disk. Debounced: subsequent calls within 500ms are
     * coalesced to avoid file churn while a user drags a panel or types.
     */
    fun flush() {
        if (!dirty) return
        runCatching {
            file.parentFile?.mkdirs()
            val root = JsonObject()
            for ((key, value) in cache) {
                root.add(key, value)
            }
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                fileGson.toJson(root, writer)
            }
            dirty = false
            lastWriteMs = System.currentTimeMillis()
        }.onFailure { ex ->
            logger.error("Failed to save ClickGUI config at ${file.absolutePath}", ex)
        }
    }
}
