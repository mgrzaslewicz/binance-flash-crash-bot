package autocoin.binance.bot.health

import autocoin.binance.bot.exchange.BinancePriceStream
import autocoin.binance.bot.strategy.StrategyExecutorService

data class UserOrder(
    val exchangeOrderId: String,
    val price: String,
    val amount: String,
    val timestamp: Long,
)

data class RunningStrategy(
    val type: String,
    val currencyPair: String,
    val user: String,
    val orders: List<UserOrder>,
    val ordersAmountSum: String,
)

data class Health(
    val healthy: Boolean,
    val unhealthyReasons: List<String>,
    val howManyTimesPriceWebSocketReconnected: Int,
    val runningStrategies: List<RunningStrategy>,
)

class HealthService(
    private val binancePriceStream: BinancePriceStream,
    private val strategyExecutorService: StrategyExecutorService,
) {
    fun getHealth(): Health {
        val isConnectedToPriceStream = binancePriceStream.isConnected()
        val runningStrategies = strategyExecutorService.getRunningStrategies().map {
            RunningStrategy(
                currencyPair = it.baseCurrencyCode + "/" + it.counterCurrencyCode,
                user = it.userId,
                type = it.strategyType.name,
                orders = it.orders.map { order ->
                    UserOrder(
                        exchangeOrderId = order.exchangeOrderId,
                        price = order.price.toPlainString(),
                        amount = order.amount.toPlainString(),
                        timestamp = order.createTimeMillis,
                    )
                },
                ordersAmountSum = it.orders.sumOf { order -> order.amount }.toPlainString(),
            )
        }.sortedBy { it.user }

        val health = Health(
            healthy = isConnectedToPriceStream,
            unhealthyReasons = listOfNotNull(
                if (isConnectedToPriceStream) null else "Not connected to price websocket stream",
            ),
            howManyTimesPriceWebSocketReconnected = binancePriceStream.getWebsocketFailureCount(),
            runningStrategies = runningStrategies,
        )
        return health
    }
}
