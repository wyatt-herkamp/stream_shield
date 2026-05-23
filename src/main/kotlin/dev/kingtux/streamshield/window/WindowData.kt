package dev.kingtux.streamshield.window

data class LocationSnapshot(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val facing: String,
    val dimension: String,
)

sealed interface ChatEvent {
    data class System(val text: String) : ChatEvent
    data class Player(val sender: String?, val text: String) : ChatEvent
}
