package dev.kingtux.streamshield.window

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.apache.logging.log4j.LogManager
import java.awt.GraphicsEnvironment

/**
 * Decides AWT's headless mode before Minecraft gets a chance to force it.
 *
 * `net.minecraft.client.main.Main` has a static initializer that runs
 * `System.setProperty("java.awt.headless", "true")`, which is why passing
 * `-Djava.awt.headless=false` on the command line never helps: the runtime call
 * overwrites the JVM argument before the game starts.
 *
 * Clearing the property again from a mod initializer only works if nothing has read it
 * yet — [GraphicsEnvironment] caches its answer in a private static field on the first
 * `isHeadless()` call and never recomputes it. Once that field says `true`, the only way
 * out is reflection, which modern JDKs refuse unless the game was launched with
 * `--add-opens java.desktop/java.awt=ALL-UNNAMED`.
 *
 * So we get there first. This runs as a `preLaunch` entrypoint, before Minecraft's `Main`
 * class is even loaded; calling `isHeadless()` here caches the real answer for the
 * lifetime of the JVM and turns Minecraft's later `setProperty` call into a no-op.
 */
object AwtHeadlessGuard : PreLaunchEntrypoint {
    private val LOGGER = LogManager.getLogger("StreamShield")

    override fun onPreLaunch() {
        // Honour an explicit request for headless mode. Anything else — unset, or an
        // explicit "false" — means we want a real display if the platform has one.
        if (System.getProperty("java.awt.headless").toBoolean()) {
            LOGGER.info(
                "java.awt.headless=true was set before launch; leaving AWT headless " +
                    "(the secondary window will be disabled)",
            )
            return
        }

        // Caches GraphicsEnvironment.headless: false if -Djava.awt.headless=false was
        // passed, otherwise the JDK's own platform check (on Linux: whether DISPLAY is set).
        if (GraphicsEnvironment.isHeadless()) {
            LOGGER.warn(
                "AWT reports no usable display, so the secondary window will be disabled. " +
                    "On Linux this means DISPLAY is unset — a Wayland session without XWayland " +
                    "cannot host a Swing window.",
            )
        } else {
            LOGGER.debug("AWT display mode locked in before Minecraft could force headless mode")
        }
    }
}
