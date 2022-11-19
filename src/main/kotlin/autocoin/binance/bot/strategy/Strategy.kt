package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecution
import java.math.BigDecimal

interface Strategy {
    fun getActions(price: BigDecimal, strategyExecution: StrategyExecution): List<StrategyAction>
}
