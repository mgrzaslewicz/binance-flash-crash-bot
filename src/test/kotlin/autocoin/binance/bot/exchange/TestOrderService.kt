package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.*
import java.math.BigDecimal
import java.time.Clock
import java.util.*

class TestOrderService(private val clock: Clock = Clock.systemDefaultZone()) : ExchangeOrderService {
    val actionHistory: MutableList<Any> = mutableListOf()
    val placedLimitBuyOrders: MutableList<ExchangeOrder> = mutableListOf()

    override fun cancelOrder(exchangeName: String, exchangeKey: ExchangeKeyDto, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        actionHistory += cancelOrderParams
        return true
    }


    override fun cancelOrder(exchangeName: String, exchangeUserId: String, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        TODO("Not yet implemented")
    }

    override fun getOpenOrders(exchangeName: String, exchangeKey: ExchangeKeyDto): List<ExchangeOrder> {
        TODO("Not yet implemented")
    }

    override fun getOpenOrders(exchangeName: String, exchangeUserId: String): List<ExchangeOrder> {
        TODO("Not yet implemented")
    }

    override fun getOpenOrdersForAllExchangeKeys(currencyPairs: List<CurrencyPair>): List<ExchangeOpenOrders> {
        TODO("Not yet implemented")
    }

    override fun isOrderNotOpen(exchangeName: String, exchangeUserId: String, order: ExchangeOrder): Boolean {
        TODO("Not yet implemented")
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
        return ExchangeOrder(
            exchangeName = "BINANCE",
            orderId = UUID.randomUUID().toString(),
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = amount,
            filledAmount = BigDecimal.ZERO,
            price = buyPrice,
            currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode),
            status = ExchangeOrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        ).apply {
            placedLimitBuyOrders += this
            actionHistory += this
        }
    }

    override fun placeLimitBuyOrder(
        exchangeName: String,
        exchangeUserId: String,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        buyPrice: BigDecimal,
        amount: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        TODO("Not yet implemented")
    }

    override fun placeLimitSellOrder(
        exchangeName: String,
        exchangeKey: ExchangeKeyDto,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        sellPrice: BigDecimal,
        amount: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        TODO("Not yet implemented")
    }

    override fun placeLimitSellOrder(
        exchangeName: String,
        exchangeUserId: String,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        sellPrice: BigDecimal,
        amount: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        TODO("Not yet implemented")
    }
}
