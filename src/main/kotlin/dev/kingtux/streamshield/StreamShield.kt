package dev.kingtux.streamshield

import dev.kingtux.streamshield.config.ConfigManager
import dev.kingtux.streamshield.keybind.Keybinds
import dev.kingtux.streamshield.mirror.CoordsMirror
import dev.kingtux.streamshield.mirror.MessageMirror
import dev.kingtux.streamshield.window.SecondaryWindow
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object StreamShield : ClientModInitializer {
    const val MOD_ID = "stream_shield"
    val LOGGER: Logger = LogManager.getLogger("StreamShield")

    override fun onInitializeClient() {
        // Minecraft's Main sets java.awt.headless=true in a static initializer, which
        // would stop Swing windows from opening. AwtHeadlessGuard already settled the
        // question before that ran; this only matters if the preLaunch entrypoint was
        // skipped, and is safe in a desktop client environment either way.
        System.setProperty("java.awt.headless", "false")

        Keybinds.register()
        MessageMirror.register()
        CoordsMirror.register()

        ClientLifecycleEvents.CLIENT_STARTED.register(
            ClientLifecycleEvents.ClientStarted {
                ConfigManager.load()
                ObfuscationState.maskCharacter = ConfigManager.config.maskCharacter
                if (ConfigManager.config.enabledByDefault) {
                    setEnabled(true)
                }
                SecondaryWindow.init()
                if (ConfigManager.config.showSecondaryWindowWhenEnabled && ObfuscationState.enabled) {
                    SecondaryWindow.setVisible(true)
                }
            },
        )

        ClientLifecycleEvents.CLIENT_STOPPING.register(
            ClientLifecycleEvents.ClientStopping {
                SecondaryWindow.dispose()
            },
        )

        ClientTickEvents.END_CLIENT_TICK.register(
            ClientTickEvents.EndTick {
                while (Keybinds.toggle.consumeClick()) {
                    setEnabled(!ObfuscationState.enabled)
                }
            },
        )
    }

    fun setEnabled(value: Boolean) {
        ObfuscationState.enabled = value
        SecondaryWindow.updateStatus(value)
        if (ConfigManager.config.showSecondaryWindowWhenEnabled) {
            SecondaryWindow.setVisible(value)
        }
        LOGGER.info("Stream Shield {}", if (value) "ENABLED" else "disabled")
    }
}
