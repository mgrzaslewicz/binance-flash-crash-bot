package autocoin.binance.bot.exchange.order

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.api.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.order.OrderSide
import com.autocoin.exchangegateway.spi.exchange.order.OrderStatus
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.time.Clock
import java.util.*
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair as SpiCurrencyPair

class MockingLimitBuyOrderServiceGateway(
    private val clock: Clock,
    private val decorated: OrderServiceGateway<ApiKeyId>,
) : OrderServiceGateway<ApiKeyId> by decorated {
    private companion object : KLogging()

    override fun placeLimitBuyOrder(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: SpiCurrencyPair,
        buyPrice: BigDecimal,
        amount: BigDecimal,
    ): Order {
        logger.info { "Creating mock placeLimitBuyOrder response" }
        return Order(
            exchangeName = exchangeName,
            exchangeOrderId = UUID.randomUUID().toString(),
            side = OrderSide.BID_BUY,
            orderedAmount = amount,
            filledAmount = BigDecimal.ZERO,
            price = buyPrice,
            currencyPair = currencyPair,
            status = OrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        )
    }

}

fun OrderServiceGateway<ApiKeyId>.mockingLimitBuyOrder(clock: Clock) = MockingLimitBuyOrderServiceGateway(clock, this)
