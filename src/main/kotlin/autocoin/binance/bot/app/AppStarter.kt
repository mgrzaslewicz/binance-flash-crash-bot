package autocoin.binance.bot.app

import autocoin.binance.bot.app.config.AppConfig
import autocoin.binance.bot.app.config.AppContext
import autocoin.binance.bot.exchange.exchangeKeyDto
import autocoin.binance.bot.exchange.priceUpdatedEventType
import autocoin.binance.bot.strategy.StrategyParameters
import automate.profit.autocoin.exchange.currency.CurrencyPair
import mu.KLogging

class AppStarter(private val config: AppConfig, private val context: AppContext) {
    companion object : KLogging()

    fun start() {
        config.createConfigFolders()
        with(context) {
            strategyExecutionsService.addStrategyExecutor(
                StrategyParameters(
                    currencyPair = CurrencyPair.Companion.of("BTC", "USDT"),
                    userId = "mikolaj",
                    counterCurrencyAmountLimitForBuying = 2000.toBigDecimal(),
                    numberOfBuyLimitOrdersToKeep = 4,
                    exchangeApiKey = exchangeKeyDto(
                        apiKey = config.binanceApiKey,
                        secretKey = config.binanceApiSecret,
                    )
                )
            )
            eventBus.register(priceUpdatedEventType, strategyExecutionsService::onPriceUpdated)
            logger.info { "Starting listening for price changes" }
            binanceTickerStream.listenForTicker(CurrencyPair.of("BTC", "USDT"))
        }
    }
}
