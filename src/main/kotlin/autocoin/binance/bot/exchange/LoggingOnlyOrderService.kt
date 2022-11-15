package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.*
import mu.KLogging
import java.math.BigDecimal
import java.time.Clock
import java.util.*

class LoggingOnlyOrderService(private val clock: Clock) : ExchangeOrderService {
    companion object : KLogging()

    override fun cancelOrder(exchangeName: String, exchangeKey: ExchangeKeyDto, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        logger.info { "[$exchangeName] Would cancelOrder $cancelOrderParams" }
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
            exchangeName = exchangeName,
            orderId = UUID.randomUUID().toString(),
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = amount,
            filledAmount = BigDecimal.ZERO,
            price = buyPrice,
            currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode),
            status = ExchangeOrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        ).also {
            logger.info { "[$exchangeName] Would placeLimitBuyOrder $it" }
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
