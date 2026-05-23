package dev.kingtux.streamshield.config

import kotlinx.serialization.Serializable

@Serializable
data class WindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

@Serializable
data class Config(
    val enabledByDefault: Boolean = false,
    val showSecondaryWindowWhenEnabled: Boolean = true,
    val maskCharacter: String = "•",
    val windowBounds: WindowBounds? = null,
)
