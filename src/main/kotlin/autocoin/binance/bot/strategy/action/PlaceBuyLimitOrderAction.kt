package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.executor.StrategyExecutor
import java.math.BigDecimal

class PlaceBuyLimitOrderAction(val price: BigDecimal, val amount: BigDecimal, override val shouldBreakActionChainOnFail: Boolean) : StrategyAction {
    override fun apply(strategyExecutor: StrategyExecutor): Boolean {
        return strategyExecutor.placeBuyLimitOrder(buyPrice = price, baseCurrencyAmount = amount) != null
    }
}
