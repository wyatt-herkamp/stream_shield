package dev.kingtux.streamshield.window

import dev.kingtux.streamshield.StreamShield
import dev.kingtux.streamshield.config.ConfigManager
import dev.kingtux.streamshield.config.WindowBounds
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.GridLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.ArrayDeque
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

object SecondaryWindow {
    private const val MAX_CHAT_LINES = 200

    private var frame: JFrame? = null
    private var chatArea: JTextArea? = null
    private var composingLabel: JLabel? = null
    private var coordsX: JLabel? = null
    private var coordsY: JLabel? = null
    private var coordsZ: JLabel? = null
    private var facingLabel: JLabel? = null
    private var dimensionLabel: JLabel? = null
    private var statusLabel: JLabel? = null

    private val chatBuffer = ArrayDeque<String>()

    @Volatile
    private var initialized = false

    fun init() {
        if (initialized) return

        // Belt-and-suspenders in case AwtHeadlessGuard never ran; harmless once
        // GraphicsEnvironment has cached its answer (see AwtHeadlessGuard).
        System.setProperty("java.awt.headless", "false")

        if (GraphicsEnvironment.isHeadless()) {
            // Something read the flag while Minecraft still had java.awt.headless=true, so
            // the cached decision is stuck. Reflection is the only way back out, and it
            // needs --add-opens to get at a java.desktop internal.
            if (!forceClearHeadlessCache()) {
                StreamShield.LOGGER.warn(
                    "AWT is stuck in headless mode, so the secondary window is disabled. " +
                        "Add --add-opens=java.desktop/java.awt=ALL-UNNAMED to the JVM arguments " +
                        "to let Stream Shield undo it. (On Linux, also check that DISPLAY is set — " +
                        "Swing needs X11 or XWayland.)",
                )
                return
            }
        }

        initialized = true
        try {
            SwingUtilities.invokeLater {
                try {
                    buildFrame()
                } catch (e: Throwable) {
                    StreamShield.LOGGER.error("Failed to build secondary window", e)
                }
            }
        } catch (e: Throwable) {
            // e.g. AWTError when AWT is non-headless but cannot reach the display server.
            StreamShield.LOGGER.error("AWT is unavailable; secondary window disabled", e)
        }
    }

    private fun forceClearHeadlessCache(): Boolean = try {
        val field = GraphicsEnvironment::class.java.getDeclaredField("headless")
        field.isAccessible = true
        field.set(null, java.lang.Boolean.FALSE)
        StreamShield.LOGGER.info("Forced GraphicsEnvironment.headless = false via reflection")
        !GraphicsEnvironment.isHeadless()
    } catch (e: Throwable) {
        StreamShield.LOGGER.debug("Could not reset GraphicsEnvironment.headless", e)
        false
    }

    private val BG = Color(28, 30, 35)
    private val BG_DEEP = Color(20, 20, 22)
    private val FG = Color(230, 230, 230)
    private val FG_DIM = Color(165, 170, 178)
    private val FG_ACCENT = Color(140, 200, 255)
    private val BORDER = Color(80, 84, 92)

    private fun titledBorder(title: String) =
        BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER),
                title,
                javax.swing.border.TitledBorder.LEADING,
                javax.swing.border.TitledBorder.TOP,
                Font(Font.MONOSPACED, Font.BOLD, 12),
                FG_DIM,
            ),
            BorderFactory.createEmptyBorder(2, 4, 2, 4),
        )

    private fun darkPanel(layout: java.awt.LayoutManager): JPanel = JPanel(layout).apply {
        background = BG
        isOpaque = true
    }

    private fun buildFrame() {
        logAvailableDisplays()

        val f = JFrame("Stream Shield — private view").apply {
            defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            layout = BorderLayout()
            contentPane.background = BG
        }

        val mono = Font(Font.MONOSPACED, Font.PLAIN, 13)

        val chat = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = mono
            background = BG_DEEP
            foreground = FG
            caretColor = FG
            border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
        }
        val chatScroll = JScrollPane(chat).apply {
            border = titledBorder("Chat")
            background = BG
            viewport.background = BG_DEEP
            isOpaque = true
        }

        val composing = JLabel(" ").apply {
            font = mono
            foreground = FG_ACCENT
            background = BG_DEEP
            isOpaque = true
            border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
        }
        val composingWrap = darkPanel(BorderLayout()).apply {
            border = titledBorder("Typing")
            add(composing, BorderLayout.CENTER)
        }

        val coordsPanel = darkPanel(GridLayout(0, 2, 6, 4)).apply {
            border = titledBorder("Location")
        }
        val x = valueLabel(mono); val y = valueLabel(mono); val z = valueLabel(mono)
        val facing = valueLabel(mono); val dim = valueLabel(mono)
        listOf(
            "X" to x, "Y" to y, "Z" to z,
            "Facing" to facing, "Dimension" to dim,
        ).forEach { (name, valueLabel) ->
            coordsPanel.add(JLabel(name).apply {
                font = mono
                foreground = FG_DIM
            })
            coordsPanel.add(valueLabel)
        }

        val status = JLabel("Disabled").apply {
            font = mono.deriveFont(Font.BOLD)
            foreground = FG
            background = BG
            isOpaque = true
            border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
        }

        val south = darkPanel(BorderLayout()).apply {
            add(composingWrap, BorderLayout.NORTH)
            add(coordsPanel, BorderLayout.CENTER)
        }

        f.add(status, BorderLayout.NORTH)
        f.add(chatScroll, BorderLayout.CENTER)
        f.add(south, BorderLayout.SOUTH)

        val bounds = ConfigManager.config.windowBounds
        val width = bounds?.width ?: 560
        val height = bounds?.height ?: 520
        if (bounds != null && isVisibleOnAnyScreen(bounds)) {
            f.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
        } else {
            centerOnDevice(f, preferredDevice(), width, height)
        }

        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                persistBounds()
            }
        })

        frame = f
        chatArea = chat
        composingLabel = composing
        coordsX = x; coordsY = y; coordsZ = z
        facingLabel = facing; dimensionLabel = dim
        statusLabel = status

        updateStatus(dev.kingtux.streamshield.ObfuscationState.enabled)
    }

    private fun valueLabel(font: Font) = JLabel("—").apply {
        this.font = font
        foreground = FG
    }

    private fun preferredDevice(): java.awt.GraphicsDevice {
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val devices = env.screenDevices
        val idx = ConfigManager.config.displayIndex
        if (idx != null && idx !in devices.indices) {
            StreamShield.LOGGER.warn(
                "Configured displayIndex={} is out of range (0..{}); using primary display",
                idx, devices.size - 1,
            )
        }
        return if (idx != null && idx in devices.indices) devices[idx] else env.defaultScreenDevice
    }

    private fun centerOnDevice(f: JFrame, device: java.awt.GraphicsDevice, width: Int, height: Int) {
        val b = device.defaultConfiguration.bounds
        val x = b.x + (b.width - width) / 2
        val y = b.y + (b.height - height) / 2
        f.setBounds(x, y, width, height)
    }

    private fun isVisibleOnAnyScreen(bounds: WindowBounds): Boolean {
        val rect = java.awt.Rectangle(bounds.x, bounds.y, bounds.width, bounds.height)
        return GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any {
            it.defaultConfiguration.bounds.intersects(rect)
        }
    }

    private fun logAvailableDisplays() {
        val devices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        StreamShield.LOGGER.info("Available displays ({} found):", devices.size)
        devices.forEachIndexed { i, d ->
            val b = d.defaultConfiguration.bounds
            StreamShield.LOGGER.info(
                "  [{}] id={} {}x{} @ ({},{})", i, d.iDstring, b.width, b.height, b.x, b.y,
            )
        }
    }

    fun setVisible(visible: Boolean) {
        SwingUtilities.invokeLater {
            val f = frame ?: return@invokeLater
            f.isVisible = visible
            if (visible) {
                f.toFront()
            }
        }
    }

    fun updateStatus(enabled: Boolean) {
        SwingUtilities.invokeLater {
            statusLabel?.text = if (enabled) "Stream Shield: ENABLED" else "Stream Shield: disabled"
            statusLabel?.foreground = if (enabled) Color(120, 230, 120) else FG_DIM
        }
    }

    fun appendChat(event: ChatEvent) {
        SwingUtilities.invokeLater {
            val line = when (event) {
                is ChatEvent.System -> event.text
                is ChatEvent.Player -> if (event.sender != null) "<${event.sender}> ${event.text}" else event.text
            }
            chatBuffer.addLast(line)
            while (chatBuffer.size > MAX_CHAT_LINES) chatBuffer.removeFirst()
            chatArea?.let { area ->
                area.text = chatBuffer.joinToString("\n")
                area.caretPosition = area.document.length
            }
        }
    }

    fun setComposing(text: String) {
        SwingUtilities.invokeLater {
            composingLabel?.text = if (text.isEmpty()) " " else "> $text"
        }
    }

    fun updateLocation(snapshot: LocationSnapshot) {
        SwingUtilities.invokeLater {
            coordsX?.text = "%.2f".format(snapshot.x)
            coordsY?.text = "%.2f".format(snapshot.y)
            coordsZ?.text = "%.2f".format(snapshot.z)
            facingLabel?.text = "${snapshot.facing}  (yaw %.1f / pitch %.1f)".format(snapshot.yaw, snapshot.pitch)
            dimensionLabel?.text = snapshot.dimension
        }
    }

    /**
     * Number of monitors AWT currently sees, or 0 if AWT is unusable. Safe to call from
     * any thread; the settings screen calls this even when the window never opened.
     */
    fun displayCount(): Int = try {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size
    } catch (e: Throwable) {
        StreamShield.LOGGER.debug("Could not query displays", e)
        0
    }

    /** Human-readable label for a monitor index, e.g. "Display 1 — 1920×1080". */
    fun displayDescription(index: Int): String {
        val b = try {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .screenDevices.getOrNull(index)?.defaultConfiguration?.bounds
        } catch (e: Throwable) {
            null
        } ?: return "Display $index"
        return "Display $index — ${b.width}×${b.height}"
    }

    /**
     * Explicitly recenter the existing window on the monitor configured via [ConfigManager]
     * ([dev.kingtux.streamshield.config.Config.displayIndex]). Used when the user changes the
     * display in the settings screen; overrides any saved bounds because it is a direct action.
     */
    fun moveToConfiguredDisplay() {
        SwingUtilities.invokeLater {
            val f = frame ?: return@invokeLater
            centerOnDevice(f, preferredDevice(), f.width, f.height)
            f.toFront()
            persistBounds()
        }
    }

    fun persistBounds() {
        val f = frame ?: return
        val b = f.bounds
        ConfigManager.update { it.copy(windowBounds = WindowBounds(b.x, b.y, b.width, b.height)) }
    }

    fun dispose() {
        if (!initialized) return
        SwingUtilities.invokeLater {
            persistBounds()
            frame?.dispose()
            frame = null
        }
    }
}
