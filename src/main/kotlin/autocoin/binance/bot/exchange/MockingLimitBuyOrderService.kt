package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.ExchangeOrder
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import automate.profit.autocoin.exchange.order.ExchangeOrderStatus
import automate.profit.autocoin.exchange.order.ExchangeOrderType
import mu.KLogging
import java.math.BigDecimal
import java.time.Clock
import java.util.*

class MockingLimitBuyOrderService(private val clock: Clock, private val decorated: ExchangeOrderService) : ExchangeOrderService by decorated {
    private companion object : KLogging()

    override fun placeLimitBuyOrder(
        exchangeName: String,
        exchangeKey: ExchangeKeyDto,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        buyPrice: BigDecimal,
        amount: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        logger.info { "Creating mock placeLimitBuyOrder response" }
        return ExchangeOrder(
            exchangeName = exchangeName,
            exchangeOrderId = UUID.randomUUID().toString(),
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = amount,
            filledAmount = BigDecimal.ZERO,
            price = buyPrice,
            currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode),
            status = ExchangeOrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        )
    }

}

fun ExchangeOrderService.mockingLimitBuyOrder(clock: Clock) = MockingLimitBuyOrderService(clock, this)
