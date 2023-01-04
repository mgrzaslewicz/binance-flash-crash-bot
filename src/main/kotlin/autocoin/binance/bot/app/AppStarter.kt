package autocoin.binance.bot.app

import autocoin.binance.bot.app.config.AppConfig
import autocoin.binance.bot.app.config.AppContext
import autocoin.binance.bot.exchange.priceUpdatedEventType
import mu.KLogging

class AppStarter(
    private val config: AppConfig,
    private val context: AppContext,
) {
    companion object : KLogging()

    fun start() {
        config.createConfigFolders()
        deleteStrategyExecutions()
        with(context) {
            strategyExecutions.load()
            strategyParameters.load()
            check(strategyParameters.isNotEmpty()) { "strategy parameters need to be provided" }
            strategyExecutionsService.addOrResumeStrategyExecutors(strategyParameters)

            eventBus.register(priceUpdatedEventType, strategyExecutionsService::onPriceUpdated)
            eventBus.register(priceUpdatedEventType, priceWebSocketConnectionKeeper::onPriceUpdated)

            logger.info { "Starting listening for price changes: ${strategyExecutionsService.currencyPairsCurrentlyNeeded()}" }
            binancePriceStream.listenForPriceUpdates(strategyExecutionsService.currencyPairsCurrentlyNeeded())

            priceWebSocketConnectionKeeper.scheduleCheckingConnection()

            server.start()
            healthMetricsScheduler.scheduleSendingMetrics()
        }
    }

    private fun deleteStrategyExecutions() {
        if (config.shouldDeleteStrategyExecutions) {
            logger.warn { "Deleting strategy executions" }
            context.strategyExecutions.clear()
            context.strategyExecutions.save()
        }
    }
}
