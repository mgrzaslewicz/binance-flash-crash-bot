package autocoin.binance.bot.strategy.action.decorator

import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.StrategyActionExecutor
import mu.KLogging
import java.util.concurrent.ExecutorService

class AsyncAction(
    private val executorService: ExecutorService,
    private val decorated: StrategyAction
) : StrategyAction {
    companion object : KLogging()

    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        executorService.submit {
            try {
                decorated.apply(strategyExecutor)
            } catch (e: Exception) {
                logger.error(e) { "Error while executing async action" }
            }
        }
        return true
    }

}

fun StrategyAction.async(executorService: ExecutorService): StrategyAction {
    return AsyncAction(executorService, this)
}
