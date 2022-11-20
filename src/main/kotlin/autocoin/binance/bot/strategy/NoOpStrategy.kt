package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecution
import java.math.BigDecimal

class NoOpStrategy: Strategy {
    override fun getActions(price: BigDecimal, strategyExecution: StrategyExecution): List<StrategyAction> {
        return emptyList()
    }
}
