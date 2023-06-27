package autocoin.binance.bot.strategy.action

import mu.KLogging

class TryWithdrawBaseCurrencyAction(
    private val currency: String,
    private val walletAddress: String,
) : StrategyAction {
    companion object : KLogging()

    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is WithdrawActionExecutor) {
            try {
                strategyExecutor.withdraw(
                    currency = currency,
                    walletAddress = walletAddress,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error while withdrawing base currency" }
            }
            return true
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement WithdrawActionExecutor")
        }
    }
}
