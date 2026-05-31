package dev.kingtux.streamshield.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.kingtux.streamshield.gui.ConfigScreen

/**
 * ModMenu entrypoint. Wired via the "modmenu" entrypoint in fabric.mod.json and only loaded when
 * ModMenu is present (declared under "suggests", not "depends").
 */
class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory { parent -> ConfigScreen(parent) }
}
