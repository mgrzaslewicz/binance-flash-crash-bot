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

class PreLoggingOrderServiceGateway(
    private val decorated: OrderServiceGateway<ApiKeyId>,
) : OrderServiceGateway<ApiKeyId> by decorated {
    companion object : KLogging()

    override fun cancelOrder(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        cancelOrderParams: CancelOrderParams,
    ): Boolean {
        logger.info { "[${exchangeName.value}] Going to cancelOrder $cancelOrderParams" }
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
        logger.info { "[${exchangeName.value}] Going to placeLimitBuyOrder exchangeName=$exchangeName, apiKey.id=${apiKey.id}, currencyPair=$currencyPair, buyPrice=${buyPrice.toPlainString()}, amount=${amount.toPlainString()}" }
        return decorated.placeLimitBuyOrder(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyPair = currencyPair,
            buyPrice = buyPrice,
            amount = amount,
        )
    }

    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): Order {
        logger.info { "[${exchangeName.value}] Going to placeLimitBuyOrder exchangeName=$exchangeName, apiKey.id=${apiKey.id}, currencyPair=$currencyPair, currentPrice=${currentPrice.toPlainString()}" }
        return decorated.placeMarketBuyOrderWithCounterCurrencyAmount(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyPair = currencyPair,
            counterCurrencyAmount = counterCurrencyAmount,
            currentPrice = currentPrice,
        )
    }
}

fun OrderServiceGateway<ApiKeyId>.preLogging() = PreLoggingOrderServiceGateway(this)

