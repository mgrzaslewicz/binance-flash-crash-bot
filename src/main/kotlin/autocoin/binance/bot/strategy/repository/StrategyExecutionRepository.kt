package autocoin.binance.bot.strategy.repository

import autocoin.binance.bot.app.config.ExchangeName

data class StrategyExecution(
    val id: String,
    val exchangeName: ExchangeName,
    val apiKey: String,
    val userId: String,
    val secretKey: String,
    val exchangeUserName: String? = null,
    val exchangeSpecificParameters: Map<String, String>? = null,
) {
    override fun toString(): String {
        return "StrategyExecution(id=$id, userId=$userId, exchangeName=$exchangeName, apiKey='$apiKey', secretKey='${
            secretKey.substring(0, 4)
        }...', userName=$exchangeUserName, exchangeSpecificParameters=$exchangeSpecificParameters)"
    }
}

interface StrategyExecutionRepository {
    fun getConfigurations(userId: String): List<StrategyExecution>
    fun getConfigurations(): List<StrategyExecution>
}


