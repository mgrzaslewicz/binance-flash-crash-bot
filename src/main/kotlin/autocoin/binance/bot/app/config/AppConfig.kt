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

    val binanceApiKey: String = firstNotNull(
        getProperty("binanceApiKey"),
        getenv("BINANCE_API_KEY"),
        "none",
    ),
    val binanceApiSecret: String = firstNotNull(
        getProperty("binanceApiSecret"),
        getenv("BINANCE_API_SECRET"),
        "none",
    ),
    val shouldPutRealOrders: Boolean = binanceApiKey != "none" && binanceApiSecret != "none"
) {
    fun createConfigFolders() {
        Files.createDirectories(botHomeFolder)
    }
}

fun loadConfig(): AppConfig {
    return AppConfig()
}

private fun <T> firstNotNull(vararg arg: T?) = arg.first { it != null }!!
