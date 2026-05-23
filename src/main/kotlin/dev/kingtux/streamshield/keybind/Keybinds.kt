package dev.kingtux.streamshield.keybind

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

object Keybinds {
    val CATEGORY: KeyMapping.Category =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("stream_shield", "main"))

    lateinit var toggle: KeyMapping
        private set

    fun register() {
        toggle = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.stream_shield.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.value,
                CATEGORY,
            ),
        )
    }
}
