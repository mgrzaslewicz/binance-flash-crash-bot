package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.order.ExchangeCancelOrderParams
import automate.profit.autocoin.exchange.order.ExchangeOrder
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import com.google.common.util.concurrent.RateLimiter
import mu.KLogging
import java.math.BigDecimal

class RateLimitingExchangeOrderService(private val decorated: ExchangeOrderService) : ExchangeOrderService by decorated {
    companion object : KLogging()

    /**
     * https://www.binance.com/en/support/announcement/notice-on-adjusting-order-rate-limits-to-the-spot-exchange-2188a59425384e2082b79d9beccf669c
     */
    private val rateLimiter = RateLimiter.create(5.0)

    override fun cancelOrder(exchangeName: String, exchangeKey: ExchangeKeyDto, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        val howManySecondsWaited = rateLimiter.acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[$exchangeName] Waited ${howManySecondsWaited}s to acquire cancelOrder permit" }
        }
        return decorated.cancelOrder(
            exchangeName = exchangeName,
            exchangeKey = exchangeKey,
            cancelOrderParams = cancelOrderParams
        )
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
        val howManySecondsWaited = rateLimiter.acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[$exchangeName] Waited ${howManySecondsWaited}s to acquire placeLimitBuyOrder permit" }
        }
        return decorated.placeLimitBuyOrder(
            exchangeName = exchangeName,
            exchangeKey = exchangeKey,
            baseCurrencyCode = baseCurrencyCode,
            counterCurrencyCode = counterCurrencyCode,
            buyPrice = buyPrice,
            amount = amount,
            isDemoOrder = isDemoOrder,
        )
    }
}

fun ExchangeOrderService.rateLimiting() = RateLimitingExchangeOrderService(decorated = this)
