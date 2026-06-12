package net.ccbluex.liquidbounce.common

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.LiquidBounce
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.texture.MipmapStrategy
import net.minecraft.client.renderer.texture.ReloadableTexture
import net.minecraft.client.renderer.texture.TextureContents
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import org.jspecify.annotations.NullMarked
import java.io.IOException
import java.util.Objects

@NullMarked
@Environment(EnvType.CLIENT)
class ArrayListLogoTexture : ReloadableTexture(COMPANION_ID) {

    override fun loadContents(resourceManager: ResourceManager): TextureContents {
        try {
            LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/arraylist_logo.png").use { stream ->
                val nativeImage = NativeImage.read(Objects.requireNonNull(stream))
                return TextureContents(
                    nativeImage,
                    TextureMetadataSection(
                        true, false,
                        MipmapStrategy.AUTO,
                        TextureMetadataSection.DEFAULT_ALPHA_CUTOFF_BIAS
                    )
                )
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        @JvmField
        val COMPANION_ID: Identifier = LiquidBounce.identifier("arraylist_logo")
        const val WIDTH: Int = 128
        const val HEIGHT: Int = 128
    }
}
