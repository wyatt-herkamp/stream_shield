package dev.kingtux.streamshield.config

import dev.kingtux.streamshield.StreamShield
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import java.nio.file.Path

object ConfigManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Volatile
    var config: Config = Config()
        private set

    private fun configPath(): Path =
        Minecraft.getInstance().gameDirectory.toPath().resolve("${StreamShield.MOD_ID}.json")

    fun load() {
        val path = configPath()
        val file = path.toFile()
        config = if (file.exists()) {
            try {
                json.decodeFromString<Config>(file.readText())
            } catch (e: Exception) {
                StreamShield.LOGGER.error("Failed to read config at $path; using defaults", e)
                Config()
            }
        } else {
            Config().also { save(it) }
        }
    }

    fun save(updated: Config = config) {
        config = updated
        try {
            configPath().toFile().writeText(json.encodeToString(updated))
        } catch (e: Exception) {
            StreamShield.LOGGER.error("Failed to save config", e)
        }
    }

    fun update(transform: (Config) -> Config) {
        save(transform(config))
    }
}
