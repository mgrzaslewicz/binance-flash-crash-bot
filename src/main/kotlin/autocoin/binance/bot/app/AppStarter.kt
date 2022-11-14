package autocoin.binance.bot.app

import autocoin.binance.bot.app.config.AppConfig
import autocoin.binance.bot.app.config.AppContext

class AppStarter(private val config: AppConfig, private val context: AppContext) {
    fun start() {
        config.createConfigFolders()
        with(context) {
            strategyExecutionRepository.getConfigurations()
        }
    }
}
