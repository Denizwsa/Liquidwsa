package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@Suppress("MagicNumber")
object ModuleBlockAnimation : ClientModule(
    name = "BlockAnimation",
    category = ModuleCategories.RENDER,
    state = true,
) {

    private val blockItem by item("Item", Items.DIAMOND_SWORD)
    private val posX by int("X", 30, 0..2000)
    private val posY by int("Y", 30, 0..2000)
    private val itemScale by float("Scale", 2.0f, 0.5f..5.0f)
    private val speed by float("Speed", 1.0f, 0.1f..5.0f)
    private val bobEnabled by boolean("Bob", true)
    private val glowEnabled by boolean("Glow", true)
    private val glowColor by color("GlowColor", Color4b(74, 143, 255, 60))
    private val bgColor by color("BgColor", Color4b(0, 0, 0, 80))

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val context = event.context
        val time = System.currentTimeMillis()
        val sc = itemScale
        val sz = (16 * sc).toInt()

        val bob = if (bobEnabled) (Math.sin(time * 0.003 * speed) * 4).toInt() else 0
        val x = posX
        val y = posY + bob

        if (glowEnabled) {
            context.fill(
                x - 4, y - 4,
                x + sz + 4, y + sz + 4,
                glowColor.argb,
            )
        }

        if (bgColor.a > 0) {
            context.fill(
                x - 2, y - 2,
                x + sz + 2, y + sz + 2,
                bgColor.argb,
            )
        }

        val itemStack = ItemStack(blockItem)
        context.pose().withPush {
            context.pose().translate(x.toFloat(), y.toFloat())
            context.pose().scale(sc, sc)
            context.item(itemStack, 0, 0)
            context.itemDecorations(mc.font, itemStack, 0, 0, null)
        }
    }
}
