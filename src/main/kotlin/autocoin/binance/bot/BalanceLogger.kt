package autocoin.binance.bot

import autocoin.binance.bot.app.config.AppContext
import autocoin.binance.bot.app.config.loadConfig
import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.exchange.apikey.md5
import com.autocoin.exchangegateway.api.exchange.apikey.ApiKey
import com.autocoin.exchangegateway.api.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.api.exchange.xchange.SupportedXchangeExchange.binance
import mu.KotlinLogging
import java.lang.System.getenv
import kotlin.system.measureTimeMillis


private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val durationMillis = measureTimeMillis {
        val config = loadConfig()
        logger.info { "Config: $config" }
        val appContext = AppContext(config)
        val publicKey = getenv("PUBLIC_KEY")
        val secretKey = getenv("SECRET_KEY")
        val currency = getenv("CURRENCY")
        val apiKey =
            ApiKeySupplier(
                id = ApiKeyId(
                    userId = "cmduser",
                    keyHash = publicKey.md5() + ":" + secretKey.md5(),
                ),
                supplier = {
                    ApiKey(
                        publicKey = publicKey,
                        secretKey = secretKey,
                    )
                }
            )
        with(appContext) {
            val balance = walletServiceGateway.getCurrencyBalance(
                exchange = binance,
                apiKey = apiKey,
                currencyCode = currency,
            )
            logger.info { "Balance: $balance" }
        }
    }
    logger.info { "Finished in ${durationMillis}ms" }
}
