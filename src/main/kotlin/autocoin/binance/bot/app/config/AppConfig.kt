package autocoin.binance.bot.app.config

import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class AppConfig(
    val botHomeFolder: Path = Paths.get(
        firstNotNull(
            getProperty("tradingBotHome"),
            getProperty("user.home"),
        )
    ).resolve(".trading-bot"),

    val fileRepositoryDirectory: Path = firstNotNull(
        getProperty("fileRepositoryDirectory")?.let { Path.of(it) },
        botHomeFolder.resolve("file-repository"),
    ),

    val shouldMakeRealOrders: Boolean = firstNotNull(
        getProperty("makeRealOrders"),
        getenv("MAKE_REAL_ORDERS"),
        "false",
    ).toBoolean()
) {
    fun createConfigFolders() {
        Files.createDirectories(botHomeFolder)
    }
}

fun loadConfig(): AppConfig {
    return AppConfig()
}

private fun <T> firstNotNull(vararg arg: T?) = arg.first { it != null }!!
