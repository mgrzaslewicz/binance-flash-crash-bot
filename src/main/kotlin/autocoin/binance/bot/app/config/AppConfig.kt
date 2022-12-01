package autocoin.binance.bot.app.config

import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class AppConfig(
    val botHomeFolder: Path = Paths.get(
        getProperty("tradingBotHome")
            ?: getProperty("user.home"),
    ).resolve(".trading-bot"),

    val fileRepositoryDirectory: Path =
        getProperty("fileRepositoryDirectory")?.let { Path.of(it) }
            ?: botHomeFolder.resolve("file-repository"),

    val shouldMakeRealOrders: Boolean = (
            getProperty("makeRealOrders")
                ?: getenv("MAKE_REAL_ORDERS")
                ?: "false"
            ).toBoolean(),

    val serverPort: Int =
        getProperty("serverPort")?.toInt()
            ?: getenv("SERVER_PORT")?.toInt()
            ?: 8284,

    ) {

    fun createConfigFolders() {
        Files.createDirectories(botHomeFolder)
    }
}

fun loadConfig(): AppConfig {
    return AppConfig()
}

