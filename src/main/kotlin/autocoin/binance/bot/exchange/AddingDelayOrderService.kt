package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.order.ExchangeCancelOrderParams
import automate.profit.autocoin.exchange.order.ExchangeOrder
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import mu.KLogging
import java.math.BigDecimal
import java.time.Duration

class AddingDelayOrderService(private val decorated: ExchangeOrderService, private val delay: Duration = Duration.ofSeconds(1)) : ExchangeOrderService by decorated {
    companion object : KLogging()

    override fun cancelOrder(exchangeName: String, exchangeKey: ExchangeKeyDto, cancelOrderParams: ExchangeCancelOrderParams): Boolean {
        Thread.sleep(delay.toMillis())
        return decorated.cancelOrder(exchangeName = exchangeName, exchangeKey = exchangeKey, cancelOrderParams = cancelOrderParams)
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
        Thread.sleep(delay.toMillis())
        return decorated.placeLimitBuyOrder(
            exchangeName = exchangeName,
            exchangeKey = exchangeKey,
            baseCurrencyCode = baseCurrencyCode,
            counterCurrencyCode = counterCurrencyCode,
            buyPrice = buyPrice,
            amount = amount,
            isDemoOrder = isDemoOrder
        )
    }

}

fun ExchangeOrderService.addingDelay(delay: Duration = Duration.ofSeconds(1)): ExchangeOrderService =
    AddingDelayOrderService(this, delay = delay)
