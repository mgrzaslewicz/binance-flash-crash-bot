package autocoin.binance.bot.exchange.order

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.time.Duration

class AddingDelayOrderServiceGateway(
    private val decorated: OrderServiceGateway<ApiKeyId>,
    private val delay: Duration = Duration.ofSeconds(1),
) : OrderServiceGateway<ApiKeyId> by decorated {
    companion object : KLogging()

    override fun cancelOrder(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        cancelOrderParams: CancelOrderParams,
    ): Boolean {
        Thread.sleep(delay.toMillis())
        return decorated.cancelOrder(
            exchangeName = exchangeName,
            apiKey = apiKey,
            cancelOrderParams = cancelOrderParams,
        )
    }

    override fun placeLimitBuyOrder(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        buyPrice: BigDecimal,
        amount: BigDecimal,
    ): Order {
        Thread.sleep(delay.toMillis())
        return decorated.placeLimitBuyOrder(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyPair = currencyPair,
            buyPrice = buyPrice,
            amount = amount,
        )
    }

}

fun OrderServiceGateway<ApiKeyId>.addingDelay(delay: Duration = Duration.ofSeconds(1)) =
    AddingDelayOrderServiceGateway(this, delay = delay)
