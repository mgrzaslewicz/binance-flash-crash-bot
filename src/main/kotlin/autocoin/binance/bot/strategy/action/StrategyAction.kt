package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import java.math.BigDecimal

interface StrategyActionExecutor {
}

interface CancelOrderActionExecutor : StrategyActionExecutor {
    fun cancelOrder(order: StrategyOrder): Boolean
}

interface PlaceBuyLimitOrderActionExecutor : StrategyActionExecutor {
    fun placeBuyLimitOrder(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal): Order
}

interface PlaceBuyMarketOrderActionExecutor : StrategyActionExecutor {
    fun placeBuyMarketOrder(currentPrice: BigDecimal, counterCurrencyAmount: BigDecimal): Order
}

interface WithdrawActionExecutor : StrategyActionExecutor {
    fun withdraw(currency: String, walletAddress: String): WithdrawResult
}

interface StrategyAction {
    fun apply(strategyExecutor: StrategyActionExecutor): Boolean
}
