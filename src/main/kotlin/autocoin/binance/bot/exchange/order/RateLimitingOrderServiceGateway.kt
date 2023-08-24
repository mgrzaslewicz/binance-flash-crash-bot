package autocoin.binance.bot.exchange.order

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import com.autocoin.exchangegateway.spi.ratelimiter.RateLimiterProvider
import mu.KLogging
import java.math.BigDecimal

class RateLimitingOrderServiceGateway(
    private val decorated: OrderServiceGateway<ApiKeyId>,
    private val rateLimiterProvider: RateLimiterProvider<ApiKeyId>,
) : OrderServiceGateway<ApiKeyId> by decorated {
    companion object : KLogging()

    override fun cancelOrder(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        cancelOrderParams: CancelOrderParams,
    ): Boolean {
        val howManySecondsWaited = rateLimiterProvider(apiKey.id).acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchange.exchangeName}, apiKey.id=${apiKey.id}] Waited ${howManySecondsWaited * 1000} ms to acquire cancelOrder permit" }
        }
        return decorated.cancelOrder(
            exchange = exchange,
            apiKey = apiKey,
            cancelOrderParams = cancelOrderParams
        )
    }

    override fun placeLimitBuyOrder(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        buyPrice: BigDecimal,
        amount: BigDecimal,
    ): Order {
        val howManySecondsWaited = rateLimiterProvider(apiKey.id).acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchange.exchangeName}, apiKey.id=${apiKey.id}] Waited ${howManySecondsWaited * 1000} ms to acquire placeLimitBuyOrder permit" }
        }
        return decorated.placeLimitBuyOrder(
            exchange = exchange,
            apiKey = apiKey,
            currencyPair = currencyPair,
            buyPrice = buyPrice,
            amount = amount,
        )
    }

}

fun OrderServiceGateway<ApiKeyId>.rateLimiting(rateLimiterProvider: RateLimiterProvider<ApiKeyId>) =
    RateLimitingOrderServiceGateway(decorated = this, rateLimiterProvider = rateLimiterProvider)
