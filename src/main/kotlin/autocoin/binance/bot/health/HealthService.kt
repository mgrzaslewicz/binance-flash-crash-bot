package autocoin.binance.bot.health

import autocoin.binance.bot.exchange.BinancePriceStream
import autocoin.binance.bot.strategy.StrategyExecutorService
import java.time.Clock
import java.time.Instant

data class UserOrder(
    val exchangeOrderId: String,
    val price: String,
    val amount: String,
    val timestamp: String,
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
    val timestamp: String,
)

class HealthService(
    private val binancePriceStream: BinancePriceStream,
    private val strategyExecutorService: StrategyExecutorService,
    private val clock: Clock,
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
                        timestamp = Instant.ofEpochMilli(order.createTimeMillis).toString(),
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
            timestamp = clock.instant().toString(),
        )
        return health
    }
}
