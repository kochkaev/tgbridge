package dev.vanutp.tgbridge.common

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.vanutp.tgbridge.common.models.Config
import dev.vanutp.tgbridge.common.models.Lang
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.*

object ConfigManager {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private lateinit var configDir: Path
    lateinit var config: Config
        private set
    lateinit var lang: Lang
        private set
    private lateinit var fallbackMinecraftLangGetter: (String) -> String?
    private var minecraftLang: Map<String, String>? = null
    private val hardcodedDefaultMinecraftLang = mapOf(
        "gui.xaero-deathpoint-old" to "Old Death",
        "gui.xaero-deathpoint" to "Death",
    )

    fun getMinecraftLangKey(key: String): String? {
        return minecraftLang?.get(key)
            ?: fallbackMinecraftLangGetter(key)
            ?: hardcodedDefaultMinecraftLang[key]
    }

    fun init(configDir: Path, fallbackMinecraftLangGetter: (String) -> String?) {
        this.configDir = configDir
        this.fallbackMinecraftLangGetter = fallbackMinecraftLangGetter
        reload()
    }

    fun reload() {
        if (configDir.notExists()) {
            configDir.createDirectory()
        }

        val configPath = configDir.resolve("config.yml")
        if (configPath.notExists()) {
            configPath.writeText(yaml.encodeToString(Config()))
        }
        val loadedConfig = yaml.decodeFromString<Config>(configPath.readText())
        if (loadedConfig.general.botToken == Config().general.botToken || loadedConfig.general.chatId == Config().general.chatId) {
            throw DefaultConfigUnchangedException()
        }
        if (loadedConfig.general.chatId > 0) {
            loadedConfig.general.chatId = -1000000000000 - loadedConfig.general.chatId
        }
        config = loadedConfig
        // write new keys & update docs
        configPath.writeText(yaml.encodeToString(config))

        val langPath = configDir.resolve("lang.yml")
        if (langPath.notExists()) {
            langPath.writeText(yaml.encodeToString(Lang()))
        }
        lang = yaml.decodeFromString<Lang>(langPath.readText())
        // write new keys & update docs
        langPath.writeText(yaml.encodeToString(lang))

        val minecraftLangPath = configDir.resolve("minecraft_lang.json")
        if (minecraftLangPath.exists()) {
            minecraftLang = Json.decodeFromString<Map<String, String>>(minecraftLangPath.readText())
        }
    }
}
