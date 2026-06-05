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
package net.ccbluex.liquidbounce.features.global

import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.utils.combat.Targets

/**
 * Target settings used by combat and visual features.
 *
 * Originally backed by the web theme's settings; kept as a minimal in-memory
 * configuration so that combat and rendering helpers can still consume it.
 */
object GlobalSettingsTarget {

    val visualChoices: MultiChoiceListValue<Targets> = MultiChoiceListValue(
        "VisualTargets",
        enumValues<Targets>().toMutableSet(),
        enumValues<Targets>().toSet(),
    )

    val combatChoices: MultiChoiceListValue<Targets> = MultiChoiceListValue(
        "CombatTargets",
        enumValues<Targets>().toMutableSet(),
        enumValues<Targets>().toSet(),
    )

    val visual: Set<Targets>
        get() = visualChoices.choices

    val combat: Set<Targets>
        get() = combatChoices.choices
}
