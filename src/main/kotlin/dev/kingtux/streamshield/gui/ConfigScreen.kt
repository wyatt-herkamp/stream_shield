package dev.kingtux.streamshield.gui

import dev.kingtux.streamshield.ObfuscationState
import dev.kingtux.streamshield.config.ConfigManager
import dev.kingtux.streamshield.window.SecondaryWindow
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * ModMenu config screen for Stream Shield. Built entirely from layout-managed widgets so it does
 * not need to override the (reworked) render pipeline in this Minecraft version.
 */
class ConfigScreen(private val parent: Screen) :
    Screen(Component.literal("Stream Shield Settings")) {

    private val cfg = ConfigManager.config

    private var enabledByDefault = cfg.enabledByDefault
    private var showWhenEnabled = cfg.showSecondaryWindowWhenEnabled

    // -1 is the sentinel for "Auto (primary)"; clamp a stale saved index (e.g. unplugged monitor).
    private var displayIndex: Int =
        cfg.displayIndex?.takeIf { it in 0 until SecondaryWindow.displayCount() } ?: -1

    private var maskCharacter = cfg.maskCharacter

    override fun init() {
        val layout = LinearLayout.vertical().spacing(8)

        layout.addChild(StringWidget(title, font))

        layout.addChild(
            CycleButton.onOffBuilder(enabledByDefault)
                .create(0, 0, BTN_WIDTH, ROW_HEIGHT, Component.literal("Enabled on launch")) { _, value ->
                    enabledByDefault = value
                },
        )

        layout.addChild(
            CycleButton.onOffBuilder(showWhenEnabled)
                .create(0, 0, BTN_WIDTH, ROW_HEIGHT, Component.literal("Show window when enabled")) { _, value ->
                    showWhenEnabled = value
                },
        )

        val displayValues = listOf(-1) + (0 until SecondaryWindow.displayCount()).toList()
        layout.addChild(
            CycleButton.builder<Int>(
                { idx ->
                    if (idx < 0) Component.literal("Auto (primary)")
                    else Component.literal(SecondaryWindow.displayDescription(idx))
                },
                displayIndex,
            )
                .withValues(displayValues)
                .create(0, 0, BTN_WIDTH, ROW_HEIGHT, Component.literal("Window display")) { _, value ->
                    displayIndex = value
                },
        )

        layout.addChild(StringWidget(Component.literal("Mask character"), font))
        layout.addChild(
            EditBox(font, BTN_WIDTH, ROW_HEIGHT, Component.literal("Mask character")).apply {
                setMaxLength(8)
                value = maskCharacter
                setResponder { maskCharacter = it }
            },
        )

        layout.addChild(
            Button.builder(Component.literal("Save & Close")) { onClose() }
                .width(BTN_WIDTH)
                .build(),
        )

        layout.arrangeElements()
        FrameLayout.centerInRectangle(layout, 0, 0, width, height)
        layout.visitWidgets { addRenderableWidget(it) }
    }

    override fun onClose() {
        save()
        minecraft.setScreenAndShow(parent)
    }

    private fun save() {
        val mask = maskCharacter.ifEmpty { "•" }
        val newDisplayIndex = displayIndex.takeIf { it >= 0 }
        ConfigManager.update {
            it.copy(
                enabledByDefault = enabledByDefault,
                showSecondaryWindowWhenEnabled = showWhenEnabled,
                maskCharacter = mask,
                displayIndex = newDisplayIndex,
            )
        }
        ObfuscationState.maskCharacter = mask
        // Reflect the chosen display immediately on the live window.
        SecondaryWindow.moveToConfiguredDisplay()
    }

    private companion object {
        const val BTN_WIDTH = 280
        const val ROW_HEIGHT = 20
    }
}
