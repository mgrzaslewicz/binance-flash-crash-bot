package autocoin.binance.bot.app

import autocoin.binance.bot.app.config.AppConfig
import autocoin.binance.bot.app.config.AppContext
import autocoin.binance.bot.exchange.priceUpdatedEventType
import mu.KLogging

class AppStarter(private val config: AppConfig, private val context: AppContext) {
    companion object : KLogging()

    fun start() {
        config.createConfigFolders()
        with(context) {
            val strategyParametersList = strategyParametersRepository.getAll()
            check(strategyParametersList.isNotEmpty()) { "strategy parameters need to be provided" }
            strategyExecutionsService.addOrResumeStrategyExecutors(strategyParametersList)
            eventBus.register(priceUpdatedEventType, strategyExecutionsService::onPriceUpdated)
            logger.info { "Starting listening for price changes" }
            strategyExecutionsService.currencyPairsCurrentlyNeeded().forEach {
                binanceTickerStream.listenForTicker(it)
            }
        }
    }
}
