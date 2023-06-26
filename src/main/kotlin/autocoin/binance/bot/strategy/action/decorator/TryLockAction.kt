package autocoin.binance.bot.strategy.action.decorator

import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.StrategyActionExecutor
import java.util.concurrent.locks.Lock

class TryLockAction(
    private val lock: Lock,
    private val decorated: StrategyAction
) :
    StrategyAction {
    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        return if (lock.tryLock()) {
            try {
                decorated.apply(strategyExecutor)
            } finally {
                lock.unlock()
            }
        } else {
            false
        }
    }

    override val shouldBreakActionChainOnFail = decorated.shouldBreakActionChainOnFail
}

fun StrategyAction.tryLock(lock: Lock): StrategyAction {
    return TryLockAction(lock, this)
}
