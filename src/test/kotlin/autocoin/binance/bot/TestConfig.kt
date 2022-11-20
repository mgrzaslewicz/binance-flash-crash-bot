package autocoin.binance.bot

import autocoin.binance.bot.strategy.counterCurrencyAmountLimitForBuyingParameter
import autocoin.binance.bot.strategy.executor.StrategyType
import autocoin.binance.bot.strategy.numberOfBuyLimitOrdersToKeepParameter
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.math.BigDecimal

object TestConfig {
    val currencyPair = CurrencyPair.of("A", "B")
    fun samplePositionBuyLimitOrdersSampleStrategyParameters(
        numberOfBuyLimitOrdersToKeep: Int = 4,
        counterCurrencyAmountLimitForBuying: BigDecimal = 100.0.toBigDecimal(),
    ): StrategyParameters {
        return StrategyParameters(
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            userId = "user-1",
            strategyType = StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH,
            exchangeApiKey = ExchangeKeyDto(
                apiKey = "key-1",
                secretKey = "secret-1",
                exchangeId = "does not matter",
                exchangeName = SupportedExchange.BINANCE.exchangeName,
                exchangeSpecificKeyParameters = emptyMap(),
                exchangeUserId = "does not matter",
                exchangeUserName = "does not matter",
                userName = null,
            ),
            strategySpecificParameters = mapOf(
                numberOfBuyLimitOrdersToKeepParameter to numberOfBuyLimitOrdersToKeep.toString(),
                counterCurrencyAmountLimitForBuyingParameter to counterCurrencyAmountLimitForBuying.toPlainString(),
            ),
        )
    }

    fun samplePositionBuyLimitOrdersStrategyExecution(
        numberOfBuyLimitOrdersToKeep: Int = 4,
        counterCurrencyAmountLimitForBuying: BigDecimal = 100.0.toBigDecimal(),
    ) = samplePositionBuyLimitOrdersSampleStrategyParameters(
        numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep,
        counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
    ).toStrategyExecution()
}
