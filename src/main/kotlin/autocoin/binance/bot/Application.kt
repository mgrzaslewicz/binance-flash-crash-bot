package autocoin.binance.bot

import autocoin.binance.bot.app.AppStarter
import autocoin.binance.bot.app.config.AppContext
import autocoin.binance.bot.app.config.loadConfig
import mu.KotlinLogging
import kotlin.system.measureTimeMillis


private val logger = KotlinLogging.logger { }

/**
 * Add limits when running process
-Xmx128M
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
 */
fun main(args: Array<String>) {
    val bootTimeMillis = measureTimeMillis {
        val config = loadConfig()
        logger.info { "Config: $config" }
        val appContext = AppContext(config)
        val appStarter = AppStarter(config = config, context = appContext)
        appStarter.start()
    }
    logger.info { "Started in ${bootTimeMillis}ms" }
}
