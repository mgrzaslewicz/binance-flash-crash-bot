package autocoin.binance.bot.strategy.action

class WithdrawBaseCurrencyAction(
    private val currency: String,
    private val walletAddress: String,
    override val shouldBreakActionChainOnFail: Boolean,
) : StrategyAction {
    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is WithdrawActionExecutor) {
            return strategyExecutor.withdraw(
                currency = currency,
                walletAddress = walletAddress,
            )
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement WithdrawActionExecutor")
        }
    }
}
