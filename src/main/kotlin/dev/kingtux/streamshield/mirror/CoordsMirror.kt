package dev.kingtux.streamshield.mirror

import dev.kingtux.streamshield.window.LocationSnapshot
import dev.kingtux.streamshield.window.SecondaryWindow
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity

object CoordsMirror {
    private const val PUSH_INTERVAL_TICKS = 2

    private var tickCounter = 0

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register(
            ClientTickEvents.EndTick { client ->
                val player = client.player ?: return@EndTick
                val level = client.level ?: return@EndTick
                tickCounter++
                if (tickCounter < PUSH_INTERVAL_TICKS) return@EndTick
                tickCounter = 0

                val facing = describeFacing(player)
                val dimension = level.dimension().identifier().toString()
                SecondaryWindow.updateLocation(
                    LocationSnapshot(
                        x = player.x,
                        y = player.y,
                        z = player.z,
                        yaw = Mth.wrapDegrees(player.yRot),
                        pitch = Mth.wrapDegrees(player.xRot),
                        facing = facing,
                        dimension = dimension,
                    ),
                )
            },
        )
    }

    private fun describeFacing(entity: Entity): String {
        val direction = Direction.fromYRot(entity.yRot.toDouble())
        val human = when (direction) {
            Direction.NORTH -> "north (-Z)"
            Direction.SOUTH -> "south (+Z)"
            Direction.WEST -> "west (-X)"
            Direction.EAST -> "east (+X)"
            else -> direction.name.lowercase()
        }
        return human
    }
}
