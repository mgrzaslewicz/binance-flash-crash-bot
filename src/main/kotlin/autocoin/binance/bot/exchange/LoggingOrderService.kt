package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.order.*
import mu.KLogging
import java.math.BigDecimal

class LoggingOrderService(private val decorated: ExchangeOrderService) : ExchangeOrderService by decorated {
    companion object : KLogging()

    override fun cancelOrder(exchangeName: String, exchangeKey: ExchangeKeyDto, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        logger.info { "[$exchangeName] Would cancelOrder $cancelOrderParams" }
        return decorated.cancelOrder(exchangeName, exchangeKey, cancelOrderParams)
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
        logger.info { "[$exchangeName] Going to placeLimitBuyOrder exchangeName=$exchangeName, exchangeKey=$exchangeKey, baseCurrencyCode=$baseCurrencyCode, counterCurrencyCode=$counterCurrencyCode, buyPrice=${buyPrice.toPlainString()}, amount=${amount.toPlainString()}" }
        val order = decorated.placeLimitBuyOrder(
            exchangeName,
            exchangeKey,
            baseCurrencyCode,
            counterCurrencyCode,
            buyPrice,
            amount,
            isDemoOrder
        )
        logger.info { "[$exchangeName] Placed limit buy order $order" }
        return order
    }

    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        exchangeName: String,
        exchangeKey: ExchangeKeyDto,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {

        logger.info { "[$exchangeName] Going to placeLimitBuyOrder exchangeName=$exchangeName, exchangeKey=$exchangeKey, baseCurrencyCode=$baseCurrencyCode, counterCurrencyCode=$counterCurrencyCode, currentPrice=${currentPrice.toPlainString()}" }
        val order = decorated.placeMarketBuyOrderWithCounterCurrencyAmount(
            exchangeName,
            exchangeKey,
            baseCurrencyCode,
            counterCurrencyCode,
            counterCurrencyAmount,
            currentPrice,
            isDemoOrder
        )
        logger.info { "[$exchangeName] Placed market buy order $order" }
        return order
    }

}

fun ExchangeOrderService.logging() = LoggingOrderService(this)

