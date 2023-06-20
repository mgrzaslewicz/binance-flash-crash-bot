package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.action.CancelOrderActionExecutor
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderActionExecutor
import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderActionExecutor
import autocoin.binance.bot.strategy.action.WithdrawActionExecutor
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto

interface StrategyExecutor :
    PriceListener,
    CancelOrderActionExecutor,
    PlaceBuyLimitOrderActionExecutor,
    PlaceBuyMarketOrderActionExecutor,
    WithdrawActionExecutor {
    val strategyExecution: StrategyExecutionDto
}
