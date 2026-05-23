package dev.kingtux.streamshield.mirror

import dev.kingtux.streamshield.window.ChatEvent
import dev.kingtux.streamshield.window.SecondaryWindow
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents

object MessageMirror {
    fun register() {
        ClientReceiveMessageEvents.GAME.register(
            ClientReceiveMessageEvents.Game { message, _ ->
                SecondaryWindow.appendChat(ChatEvent.System(message.string))
            },
        )
        ClientReceiveMessageEvents.CHAT.register(
            ClientReceiveMessageEvents.Chat { message, _, sender, _, _ ->
                SecondaryWindow.appendChat(ChatEvent.Player(sender?.name, message.string))
            },
        )
    }
}
