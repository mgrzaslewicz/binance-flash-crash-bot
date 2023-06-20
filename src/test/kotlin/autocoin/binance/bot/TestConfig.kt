package autocoin.binance.bot

import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.PositionBuyOrdersForFlashCrashStrategy
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.toStrategyExecution
import autocoin.binance.bot.strategy.executor.StrategyType
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import java.math.BigDecimal

object TestConfig {
    val currencyPair = CurrencyPair.of("A", "B")
    fun samplePositionBuyLimitOrdersSampleStrategyParameters(
        numberOfBuyLimitOrdersToKeep: Int = 4,
        counterCurrencyAmountLimitForBuying: BigDecimal = 100.0.toBigDecimal(),
    ): StrategyParametersDto {
        return StrategyParametersDto(
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            userId = "user-1",
            strategyType = StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH,
            apiKey = ApiKeyDto(
                publicKey = "key-1",
                secretKey = "secret-1",
            ),
            strategySpecificParameters = PositionBuyOrdersForFlashCrashStrategy.ParametersBuilder()
                .withNumberOfBuyLimitOrdersToKeep(numberOfBuyLimitOrdersToKeep)
                .withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying)
                .toSMap()
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
