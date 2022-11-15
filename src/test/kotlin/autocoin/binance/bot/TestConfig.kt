package autocoin.binance.bot

import autocoin.binance.bot.app.config.AppConfig
import autocoin.binance.bot.strategy.StrategyParameters
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair

object TestConfig {
    val testAppConfig = AppConfig(
    )
    val currencyPair = CurrencyPair.of("A", "B")
    val sampleStrategyParameters = StrategyParameters(
        currencyPair = currencyPair,
        userId = "user-1",
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
        counterCurrencyAmountLimitForBuying = 100.0.toBigDecimal(),
    )
}
