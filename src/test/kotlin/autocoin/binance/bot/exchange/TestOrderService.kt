package autocoin.binance.bot.exchange

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.api.exchange.order.Order
import com.autocoin.exchangegateway.api.exchange.xchange.SupportedXchangeExchange.binance
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.spi.exchange.order.OrderSide
import com.autocoin.exchangegateway.spi.exchange.order.OrderStatus
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.time.Clock
import java.util.*
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair as SpiCurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.Order as SpiOrder

class TestOrderService(private val clock: Clock = Clock.systemDefaultZone()) : OrderServiceGateway<ApiKeyId> {
    private companion object : KLogging()

    val successfulActionHistory: MutableList<Any> = mutableListOf()
    private var placeBuyLimitOrderInvocationIndex = 0


    override fun cancelOrder(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        cancelOrderParams: CancelOrderParams,
    ): Boolean {
        successfulActionHistory += cancelOrderParams
        return true
    }

    var placeLimitBuyOrderInvocationFailureIndexes: List<Int> = emptyList()


    override fun placeLimitBuyOrder(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: SpiCurrencyPair,
        buyPrice: BigDecimal,
        amount: BigDecimal,
    ): Order {
        if (placeBuyLimitOrderInvocationIndex++ in placeLimitBuyOrderInvocationFailureIndexes) {
            throw Exception("Failed on purpose during placing buy limit order")
        } else {
            logger.info { "Placing buy limit order with amount=$amount ${currencyPair.base} and buyPrice=$buyPrice ${currencyPair.counter}" }
            return Order(
                exchange = binance,
                exchangeOrderId = UUID.randomUUID().toString(),
                side = OrderSide.BID_BUY,
                orderedAmount = amount,
                filledAmount = BigDecimal.ZERO,
                price = buyPrice,
                currencyPair = currencyPair,
                status = OrderStatus.NEW,
                receivedAtMillis = clock.millis(),
                exchangeTimestampMillis = null,
            ).apply {
                successfulActionHistory += this
            }
        }
    }

    override fun getOpenOrders(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
    ): List<SpiOrder> {
        return emptyList()
    }

    override fun getOpenOrders(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
    ): List<SpiOrder> {
        TODO("Not yet implemented")
    }

    override fun placeLimitSellOrder(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        sellPrice: BigDecimal,
        amount: BigDecimal,
    ): SpiOrder {
        TODO("Not yet implemented")
    }

    override fun placeMarketBuyOrderWithBaseCurrencyAmount(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        baseCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): SpiOrder {
        TODO("Not yet implemented")
    }

    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): SpiOrder {
        TODO("Not yet implemented")
    }

    override fun placeMarketSellOrderWithBaseCurrencyAmount(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        baseCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): SpiOrder {
        TODO("Not yet implemented")
    }

    override fun placeMarketSellOrderWithCounterCurrencyAmount(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyPair: CurrencyPair,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): SpiOrder {
        TODO("Not yet implemented")
    }

}
