package autocoin.binance.bot.strategy.execution.repository

import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.OrderStatus
import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.util.*

data class StrategyOrder(
    val id: String = UUID.randomUUID().toString(),
    val exchangeOrderId: String,
    val status: OrderStatus = OrderStatus.NEW,
    val price: BigDecimal,
    val amount: BigDecimal,
    val amountFilled: BigDecimal = BigDecimal.ZERO,
    val baseCurrencyCode: String,
    val counterCurrencyCode: String,
    val createTimeMillis: Long,
    val closeTimeMillis: Long? = null,
) {
    @JsonIgnore
    val currencyPair: CurrencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode)
}



