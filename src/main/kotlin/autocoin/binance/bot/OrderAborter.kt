package autocoin.binance.bot

import autocoin.binance.bot.app.config.AppContext
import autocoin.binance.bot.app.config.loadConfig
import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.exchange.apikey.md5
import com.autocoin.exchangegateway.api.exchange.apikey.ApiKey
import com.autocoin.exchangegateway.api.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.api.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.api.exchange.xchange.ExchangeNames.Companion.binance
import mu.KotlinLogging
import java.lang.System.getenv
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
        val publicKey = getenv("PUBLIC_KEY")
        val secretKey = getenv("SECRET_KEY")
        val currencyPair = CurrencyPair.of(getenv("CURRENCY_PAIR"))
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
            val openOrders = orderServiceGateway.getOpenOrders(
                exchangeName = binance,
                apiKey = apiKey,
                currencyPair = currencyPair,
            )
            logger.info { "Open orders: $openOrders" }
            openOrders.forEach {
                logger.info { "Cancelling order ${it.exchangeOrderId}" }
                orderServiceGateway.cancelOrder(
                    exchangeName = binance,
                    apiKey = apiKey,
                    cancelOrderParams = CancelOrderParams(
                        exchangeName = it.exchangeName,
                        orderId = it.exchangeOrderId,
                        orderSide = it.side,
                        currencyPair = it.currencyPair,
                    )
                ).also {
                    logger.info { "Order cancelled: $it" }
                }
            }
        }
    }
    logger.info { "Finished in ${bootTimeMillis}ms" }
}
