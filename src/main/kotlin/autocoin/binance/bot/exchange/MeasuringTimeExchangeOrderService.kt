package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.order.ExchangeCancelOrderParams
import automate.profit.autocoin.exchange.order.ExchangeOrder
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import mu.KLogging
import java.math.BigDecimal
import kotlin.system.measureTimeMillis

class MeasuringTimeExchangeOrderService(private val decorated: ExchangeOrderService) : ExchangeOrderService by decorated {
    companion object : KLogging()

    override fun cancelOrder(exchangeName: String, exchangeKey: ExchangeKeyDto, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        var result: Boolean
        val millis = measureTimeMillis {
            result = decorated.cancelOrder(
                exchangeName = exchangeName,
                exchangeKey = exchangeKey,
                cancelOrderParams = cancelOrderParams
            )
        }
        logger.info { "Canceling order took ${millis}ms" }
        return result
    }

    override fun placeLimitBuyOrder(
        exchangeName: String,
        exchangeKey: ExchangeKeyDto,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        buyPrice: BigDecimal,
        amount: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        var result: ExchangeOrder
        val millis = measureTimeMillis {
            result = decorated.placeLimitBuyOrder(
                exchangeName = exchangeName,
                exchangeKey = exchangeKey,
                baseCurrencyCode = baseCurrencyCode,
                counterCurrencyCode = counterCurrencyCode,
                buyPrice = buyPrice,
                amount = amount,
                isDemoOrder = isDemoOrder,
            )
        }
        logger.info { "Placing limit buy order took ${millis}ms" }
        return result
    }
}

fun ExchangeOrderService.measuringTime() = MeasuringTimeExchangeOrderService(decorated = this)
