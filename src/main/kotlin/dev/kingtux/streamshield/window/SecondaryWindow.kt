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

        // Belt-and-suspenders: clear the headless flag in case the JVM was launched with it.
        // Must happen before any AWT/Swing class is touched.
        System.setProperty("java.awt.headless", "false")

        if (GraphicsEnvironment.isHeadless()) {
            // GraphicsEnvironment caches its headless decision on first access. If something
            // earlier in the JVM lifecycle initialized AWT before we cleared the property,
            // try to flip the cached flag via reflection.
            if (!forceClearHeadlessCache()) {
                StreamShield.LOGGER.warn(
                    "AWT already initialized in headless mode and could not be reset; " +
                        "secondary window disabled. Try removing -Djava.awt.headless=true from JVM args.",
                )
                return
            }
        }

        initialized = true
        SwingUtilities.invokeLater {
            try {
                buildFrame()
            } catch (e: Throwable) {
                StreamShield.LOGGER.error("Failed to build secondary window", e)
            }
        }
    }

    private fun forceClearHeadlessCache(): Boolean = try {
        val field = GraphicsEnvironment::class.java.getDeclaredField("headless")
        field.isAccessible = true
        field.set(null, java.lang.Boolean.FALSE)
        StreamShield.LOGGER.info("Forced GraphicsEnvironment.headless = false via reflection")
        !GraphicsEnvironment.isHeadless()
    } catch (e: Throwable) {
        StreamShield.LOGGER.warn("Could not reset GraphicsEnvironment.headless", e)
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
        if (bounds != null) {
            f.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
        } else {
            f.setSize(560, 520)
            f.setLocationRelativeTo(null)
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
