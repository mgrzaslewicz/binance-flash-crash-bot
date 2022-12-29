package autocoin.binance.bot.exchange

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import com.google.common.util.concurrent.RateLimiter
import mu.KLogging
import java.math.BigDecimal

class RateLimitingExchangeOrderServiceGateway(
    private val decorated: OrderServiceGateway<ApiKeyId>,
    private val permitsPerSecond: Double = 5.0,
    /**
     * https://www.binance.com/en/support/announcement/notice-on-adjusting-order-rate-limits-to-the-spot-exchange-2188a59425384e2082b79d9beccf669c
     */
    private val rateLimiter: RateLimiter = RateLimiter.create(permitsPerSecond),
) : OrderServiceGateway<ApiKeyId> by decorated {
    companion object : KLogging()


    override fun cancelOrder(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        cancelOrderParams: CancelOrderParams,
    ): Boolean {
        val howManySecondsWaited = rateLimiter.acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchangeName.value}, apiKey.id=${apiKey.id}] Waited ${howManySecondsWaited * 1000} ms to acquire cancelOrder permit" }
        }
        return decorated.cancelOrder(
            exchangeName = exchangeName,
            apiKey = apiKey,
            cancelOrderParams = cancelOrderParams
        )
    }

    override fun placeLimitBuyOrder(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        buyPrice: BigDecimal,
        amount: BigDecimal,
    ): Order {
        val howManySecondsWaited = rateLimiter.acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchangeName.value}, apiKey.id=${apiKey.id}] Waited ${howManySecondsWaited * 1000} ms to acquire placeLimitBuyOrder permit" }
        }
        return decorated.placeLimitBuyOrder(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyPair = currencyPair,
            buyPrice = buyPrice,
            amount = amount,
        )
    }

}

fun OrderServiceGateway<ApiKeyId>.rateLimiting(permitsPerSecond: Double = 5.0) = RateLimitingExchangeOrderServiceGateway(decorated = this, permitsPerSecond = permitsPerSecond)
