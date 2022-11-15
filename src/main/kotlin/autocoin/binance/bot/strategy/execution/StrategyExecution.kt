package autocoin.binance.bot.strategy.execution

import autocoin.binance.bot.app.config.ExchangeName
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.util.*

data class StrategyExecution(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val exchangeName: ExchangeName,

    val baseCurrencyCode: String,
    val counterCurrencyCode: String,

    val numberOfBuyLimitOrdersToKeep: Int = 4,

    val counterCurrencyAmountLimitForBuying: BigDecimal,
    val orders: List<StrategyOrder> = emptyList(),

    val createTimeMillis: Long,


    val exchangeApiKey: ExchangeKeyDto,
) {
    @JsonIgnore
    val currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode)

    @JsonIgnore
    val numberOfOrders = orders.size

    @JsonIgnore
    val ordersByPriceDesc: List<StrategyOrder> = orders.sortedByDescending { it.price }

    @JsonIgnore
    val orderWithMaxPrice: StrategyOrder? = ordersByPriceDesc.firstOrNull()

    @JsonIgnore
    val orderWithMinPrice: StrategyOrder? = ordersByPriceDesc.lastOrNull()
}
