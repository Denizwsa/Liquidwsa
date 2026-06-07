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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.utils.collection.GenericPools;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;

/**
 * This mixin used to recycle the textures HashMap in RenderType#draw
 * via MixinExtras @Local injection, but the method structure changed
 * in 26.1.2 (new render pipeline). The optimization is not critical
 * — left as a no-op mixin to avoid deleting the registration entry.
 */
@NullMarked
@Mixin(RenderType.class)
public abstract class MixinRenderType {

}
