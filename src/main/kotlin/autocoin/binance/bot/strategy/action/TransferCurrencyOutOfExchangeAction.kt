package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.executor.StrategyExecutor

class WithdrawBaseCurrencyAction(
    private val currency: String,
    private val walletAddress: String,
    override val shouldBreakActionChainOnFail: Boolean,
) : StrategyAction {
    override fun apply(strategyExecutor: StrategyExecutor) = strategyExecutor.withdraw(
        currency = currency,
        walletAddress = walletAddress,
    )
}
