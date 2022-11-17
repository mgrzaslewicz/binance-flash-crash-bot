package autocoin.binance.bot.strategy.parameters.repository

import autocoin.binance.bot.strategy.parameters.StrategyParameters

interface StrategyParametersRepository {
    fun getAll(): List<StrategyParameters>
}
