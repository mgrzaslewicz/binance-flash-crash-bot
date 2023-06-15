package autocoin.binance.bot

import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.BuyWithMarketOrderBelowPriceStrategy
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
            strategySpecificParameters = PositionBuyOrdersForFlashCrashStrategy.Builder()
                .withNumberOfBuyLimitOrdersToKeep(numberOfBuyLimitOrdersToKeep)
                .withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying)
                .toStrategySpecificParameters()
        )
    }

    fun samplePositionBuyLimitOrdersStrategyExecution(
        numberOfBuyLimitOrdersToKeep: Int = 4,
        counterCurrencyAmountLimitForBuying: BigDecimal = 100.0.toBigDecimal(),
    ) = samplePositionBuyLimitOrdersSampleStrategyParameters(
        numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep,
        counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
    ).toStrategyExecution()

    fun samplePlaceBuyMarketOrdersBelowPriceStrategyParameters(
        pricesTriggeringBuyMarketOrder: List<BigDecimal>,
        counterCurrencyAmountLimitForBuying: BigDecimal,
        maxPriceForComingBackFromBottomBuyMarketOrder: BigDecimal,
    ): StrategyParametersDto {
        return StrategyParametersDto(
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            userId = "user-1",
            strategyType = StrategyType.BUY_WITH_MARKET_ORDER_BELOW_PRICE,
            apiKey = ApiKeyDto(
                publicKey = "key-1",
                secretKey = "secret-1",
            ),
            strategySpecificParameters = BuyWithMarketOrderBelowPriceStrategy.Builder()
                .withPricesTriggeringBuyMarketOrder(pricesTriggeringBuyMarketOrder)
                .withMaxPriceForComingBackFromBottomBuyMarketOrder(maxPriceForComingBackFromBottomBuyMarketOrder)
                .withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying)
                .toStrategySpecificParameters()
        )
    }

    fun samplePlaceBuyMarketOrdersBelowPriceStrategyExecution(
        pricesTriggeringBuyMarketOrderParameter: List<BigDecimal>,
        counterCurrencyAmountLimitForBuying: BigDecimal,
        maxPriceForComingBackFromBottomBuyMarketOrderParameter: BigDecimal,
    ) = samplePlaceBuyMarketOrdersBelowPriceStrategyParameters(
        pricesTriggeringBuyMarketOrder = pricesTriggeringBuyMarketOrderParameter,
        counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
        maxPriceForComingBackFromBottomBuyMarketOrder = maxPriceForComingBackFromBottomBuyMarketOrderParameter,
    ).toStrategyExecution()
}
