package dev.kingtux.streamshield

object ObfuscationState {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var maskCharacter: String = "•"
}
